package bdpryfinal;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class QuickCitaMain {

  private static class Pair {
    final int id; final String cedula;
    Pair(int id, String cedula) { this.id = id; this.cedula = cedula; }
  }

  private static Pair ultDoctor() throws SQLException {
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement("SELECT id, cedula FROM Doctor ORDER BY id DESC LIMIT 1");
         ResultSet rs = ps.executeQuery()) {
      if (!rs.next()) throw new IllegalStateException("No hay doctores en BD.");
      return new Pair(rs.getInt("id"), rs.getString("cedula"));
    }
  }

  private static Pair ultPaciente() throws SQLException {
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement("SELECT id, cedula FROM Paciente ORDER BY id DESC LIMIT 1");
         ResultSet rs = ps.executeQuery()) {
      if (!rs.next()) throw new IllegalStateException("No hay pacientes en BD.");
      return new Pair(rs.getInt("id"), rs.getString("cedula"));
    }
  }

  public static void main(String[] args) {
    System.out.println("=== QuickCitaMain (cédula) ===");
    var svc = new CitaService();

    try {
      Pair doc = ultDoctor();
      Pair pac = ultPaciente();
      System.out.printf("Usando Doctor  #%d [%s], Paciente #%d [%s]%n", doc.id, doc.cedula, pac.id, pac.cedula);

      LocalDate fecha = LocalDate.now().plusDays(1);
      LocalTime hora  = LocalTime.of(10, 0);

      // 1) Crear cita por CÉDULAS
      int citaId = svc.crearPorCodigos(pac.cedula, doc.cedula, fecha, hora, "Pendiente", "Prueba rápida");
      System.out.println("Cita creada id=" + citaId);

      // 2) Ver agenda del doctor
      List<CitaDaoJdbc.AgendaItem> agenda = svc.agendaDoctor(doc.cedula, fecha);
      System.out.println("-- Agenda Doctor --");
      for (var a : agenda) {
        System.out.printf("#%d  %s  %-20s  %-10s  %s%n",
            a.id, a.hora, a.paciente, a.estado, a.observacion == null ? "" : a.observacion);
      }

      // 3) Ver citas del paciente (rango)
      var citasPac = svc.citasPaciente(pac.id, fecha.minusDays(1), fecha.plusDays(1));
      System.out.println("-- Citas Paciente --");
      for (var c : citasPac) {
        System.out.printf("#%d  %s %s  %-20s  %-10s  %s%n",
            c.id, c.fecha, c.hora, c.doctor, c.estado, c.observacion == null ? "" : c.observacion);
      }

      // 4) Cambiar estado → Confirmada
      svc.actualizarEstado(citaId, "Confirmada");
      System.out.println("Estado actualizado a Confirmada.");

      // 5) Cancelar por paciente
      svc.cancelarPorPaciente(citaId, pac.id);
      System.out.println("Cita cancelada por paciente.");

      System.out.println("=== ✅ QuickCita OK ===");
    } catch (Exception e) {
      System.out.println("=== ❌ QuickCita FAIL ===");
      e.printStackTrace(System.out);
      System.exit(1);
    }
  }
}
