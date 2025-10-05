package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class SmokeMain {

  public static void main(String[] args) {
    System.out.println("=== Smoke Test Avance (cédula) ===");
    var udao  = new UsuarioDaoJdbc();
    var ddao  = new DoctorDaoJdbc();
    var pdao  = new PacienteDaoJdbc();
    var citas = new CitaService();

    // Sufijo único para evitar colisiones
    String nonce = String.valueOf(System.currentTimeMillis() % 1_000_000);
    String userDoc = "doc_smoke_" + nonce;
    String userPac = "pac_smoke_" + nonce;
    String pass    = "pass";
    String cedDoc  = "D" + nonce;   // cédulas cortas/únicas
    String cedPac  = "P" + nonce;

    try {
      // 1) Crear usuarios
      int uDocId = udao.crearUsuario(userDoc, userDoc, pass, "Doctor");
      int uPacId = udao.crearUsuario(userPac, userPac, pass, "Paciente");
      System.out.printf("Usuarios creados  Doctor#%d  Paciente#%d%n", uDocId, uPacId);

      // 2) Completar/insertar perfiles con CÉDULA
      ddao.insertarPerfilDoctor(
          uDocId, cedDoc,
          "Doc", null, "Prueba", null,
          "Medicina General", "Sede A", "08:00-12:00",
          "doc@example.com", "3000000000", "M"
      );
      pdao.insertarPerfilPaciente(
          uPacId, cedPac,
          "Pac", null, "Prueba", null,
          "pac@example.com", "3110000000", "F",
          "Calle 123", LocalDate.of(1990,1,1)
      );
      System.out.printf("Perfiles listos  cedDoc=%s  cedPac=%s%n", cedDoc, cedPac);

      // IDs internos (FK a Cita y para cancelar por paciente)
      Integer doctorId   = udao.doctorIdPorUsuarioId(uDocId);
      Integer pacienteId = udao.pacienteIdPorUsuarioId(uPacId);
      if (doctorId == null || pacienteId == null) {
        throw new IllegalStateException("No se pudo obtener doctorId/pacienteId desde Usuario.");
      }

      // 3) Crear cita (vía CÉDULAS)
      LocalDate fecha = LocalDate.now().plusDays(1);
      LocalTime hora  = LocalTime.of(10, 0);
      int citaId = citas.crearPorCodigos(cedPac, cedDoc, fecha, hora, "Pendiente", "Smoke test");
      System.out.println("Cita creada id=" + citaId);

      // 4) Verificar agenda del doctor
      var agenda = citas.agendaDoctor(cedDoc, fecha);
      imprimirAgenda(agenda);

      // 5) Verificar citas del paciente (rango que cubra la fecha)
      var listaPac = citas.citasPaciente(pacienteId, fecha.minusDays(1), fecha.plusDays(1));
      imprimirCitasPaciente(listaPac);

      // 6) Cambiar estado y 7) cancelar por paciente
      citas.actualizarEstado(citaId, "Confirmada");
      System.out.println("Estado actualizado a Confirmada.");
      citas.cancelarPorPaciente(citaId, pacienteId);
      System.out.println("Cita cancelada por paciente.");

      System.out.println("=== ✅ Smoke OK ===");
    } catch (Exception e) {
      System.out.println("=== ❌ Smoke FAIL ===");
      e.printStackTrace(System.out);
      System.exit(1);
    }
  }

  private static void imprimirAgenda(List<CitaDaoJdbc.AgendaItem> agenda) {
    System.out.println("-- Agenda Doctor --");
    if (agenda.isEmpty()) {
      System.out.println("(sin turnos)");
      return;
    }
    for (var a : agenda) {
      System.out.printf("#%d  %s  %-20s  %-10s  %s%n",
          a.id, a.hora, a.paciente, a.estado, a.observacion == null ? "" : a.observacion);
    }
  }

  private static void imprimirCitasPaciente(List<CitaDaoJdbc.CitaPacItem> lista) {
    System.out.println("-- Citas Paciente --");
    if (lista.isEmpty()) {
      System.out.println("(sin citas)");
      return;
    }
    for (var c : lista) {
      System.out.printf("#%d  %s %s  %-20s  %-10s  %s%n",
          c.id, c.fecha, c.hora, c.doctor, c.estado, c.observacion == null ? "" : c.observacion);
    }
  }
}
