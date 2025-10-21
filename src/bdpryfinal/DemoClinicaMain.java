package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Demo funcional “realista” para presentar el proyecto con tu esquema de BD.
 * - Re-ejecutable gracias a un sufijo único (nonce) que evita choques UNIQUE.
 * - Actualiza perfiles por usuario_id (coincide con UNIQUE(usuario_id) en doctor/paciente).
 * - Crea citas por cédula, lista agenda/citas, actualiza estado y cancela.
 * - Inicia y cierra sesión (doctor) usando UsuarioDaoJdbc + SesionDaoJdbc.
 */
public class DemoClinicaMain {

  public static void main(String[] args) {
    System.out.println("=== Demo Clínica (cédula) ===");

    var udao   = new UsuarioDaoJdbc();
    var ddao   = new DoctorDaoJdbc();
    var pdao   = new PacienteDaoJdbc();
    var citas  = new CitaService();
    var notas  = new NotaService();
    var sdao   = new SesionDaoJdbc(); // <- para abrir/cerrar sesión

    // Sufijo único para que la demo sea re-ejecutable
    String nonce = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now());

    // “Datos reales” base + sufijo para unicidad
    String userDoc = "dr.alvarez";
    String passDoc = "segura2025";
    String cedDoc  = "1718132645" + nonce.substring(nonce.length()-4); // cédula única

    String userPac = "maria.rojas";
    String passPac = "maria2025";
    String cedPac  = "0923456789" + nonce.substring(nonce.length()-4); // cédula única

    Integer sesionId = null; // <- guardaremos el id de sesión para cerrarla al final

    try {
      // 1) Crear usuarios (triggers insertan filas mínimas en doctor/paciente con CED-<id>)
      int uDocId = udao.crearUsuario(userDoc, passDoc, "Doctor");
      int uPacId = udao.crearUsuario(userPac, passPac, "Paciente");
      System.out.printf("Usuarios creados  Doctor#%d  Paciente#%d%n", uDocId, uPacId);

      // 2) Upsert de perfiles por usuario_id (coincide con UNIQUE(usuario_id))
      ddao.insertarPerfilDoctor(
          uDocId, cedDoc,
          "Diego", "Andrés", "Álvarez", "García",
          "Cardiología", "Sede Centro", "08:00-12:00",
          "dr.alvarez+"+ "@clinicademo.com",
          "099" + nonce.substring(nonce.length()-7),
          "M"
      );

      pdao.insertarPerfilPaciente(
          uPacId, cedPac,
          "María", "Fernanda", "Rojas", "Vega",
          "maria.rojas+" + "@correo.com",
          "098" + nonce.substring(nonce.length()-7),
          "F",
          "Av. Libertad 123",
          LocalDate.of(1991, 5, 17)
      );

      System.out.println("Perfiles actualizados con cédula.");

      // 3) Iniciar sesión (DOCTOR) usando UsuarioDaoJdbc + SesionDaoJdbc
      if (!udao.VerfPassword(userDoc, passDoc)) {
        throw new IllegalStateException("Credenciales inválidas del doctor demo");
      }
      sesionId = sdao.abrirSesion(uDocId);
      System.out.println("Sesión iniciada (Doctor) id=" + sesionId);

      // 4) Obtener IDs internos (FK) desde Usuario → (doctor/paciente)
      Integer doctorId   = udao.doctorIdPorUsuario(uDocId);
      Integer pacienteId = udao.pacienteIdPorUsuario(uPacId);
      if (doctorId == null || pacienteId == null) {
        throw new IllegalStateException("No se pudo obtener doctorId/pacienteId desde Usuario.");
      }
      System.out.printf("IDs internos  doctorId=%d  pacienteId=%d%n", doctorId, pacienteId);

      // 5) Crear una nota libre usando NotaService/NotaDaoJdbc
      var notaView = notas.agregarNotaPorCodigo(cedPac,
          "El paciente esta bien" );
      System.out.printf("El paciente esta bien",
          notaView.notaId, notaView.notas.size());

      // 6) Crear dos citas (por CÉDULAS)
      LocalDate fecha = LocalDate.now().plusDays(1);
      int cita1 = citas.crearPorCodigos(cedPac, cedDoc, fecha, LocalTime.of(9, 30),
          "Pendiente", "Chequeo general anual");
      int cita2 = citas.crearPorCodigos(cedPac, cedDoc, fecha, LocalTime.of(11, 0),
          "Pendiente", "Control de presión arterial");
      System.out.printf("Citas creadas  #%d y #%d%n", cita1, cita2);

      // 7) Agenda del doctor (por fecha)
      var agenda = citas.agendaDeDoctor(cedDoc, fecha);
      imprimirAgenda(agenda);

      // 8) Citas del paciente (rango que incluye la fecha)
      var citasPac = citas.citasDePaciente(pacienteId, fecha.minusDays(1), fecha.plusDays(1));
      imprimirCitasPaciente(citasPac);

      // 9) Cambiar estado de la primera y cancelar la segunda
      citas.actualizarEstado(cita1, "Confirmada");
      System.out.println("Cita #" + cita1 + " actualizada a Confirmada.");
      citas.cancelarPorPaciente(cita2, pacienteId);
      System.out.println("Cita #" + cita2 + " cancelada por el paciente.");

      // 10) Mostrar estado final para la fecha
      System.out.println("\n-- Estado final del día --");
      imprimirAgenda(citas.agendaDeDoctor(cedDoc, fecha));
      imprimirCitasPaciente(citas.citasDePaciente(pacienteId, fecha, fecha));

      System.out.println("=== ✅ Demo OK ===");
    } catch (Exception e) {
      System.out.println("=== ❌ Demo FAIL ===");
      e.printStackTrace(System.out);
      System.exit(1);
    } finally {
      // 11) Cerrar sesión, si se abrió
      if (sesionId != null) {
        try {
          sdao.cerrarSesion(sesionId);
          System.out.println("Sesión cerrada id=" + sesionId);
        } catch (Exception ignore) { /* no-op */ }
      }
    }
  }

  /* ===================== helpers impresión ===================== */

  private static void imprimirAgenda(List<CitaDaoJdbc.AgendaItem> agenda) {
    System.out.println("\n-- Agenda del Doctor --");
    if (agenda == null || agenda.isEmpty()) {
      System.out.println("(sin turnos)");
      return;
    }
    for (var a : agenda) {
      System.out.printf("#%d  %s  %-22s  %-10s  %s%n",
          a.id, a.hora, a.paciente, a.estado, a.observacion == null ? "" : a.observacion);
    }
  }

  private static void imprimirCitasPaciente(List<CitaDaoJdbc.CitaPacItem> lista) {
    System.out.println("\n-- Citas del Paciente --");
    if (lista == null || lista.isEmpty()) {
      System.out.println("(sin citas)");
      return;
    }
    for (var c : lista) {
      System.out.printf("#%d  %s %s  %-22s  %-10s  %s%n",
          c.id, c.fecha, c.hora, c.doctor, c.estado, c.observacion == null ? "" : c.observacion);
    }
  }
}
