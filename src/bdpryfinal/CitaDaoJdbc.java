package bdpryfinal;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

//Clase cita
public class CitaDaoJdbc {
  //Clase anidada para mostrar elementos de una agenda
  public static final class AgendaItem {
    public final int id;
    public final String hora;
    public final String paciente;
    public final String estado;
    public final String observacion;
    
    public AgendaItem(int id, String hora, String paciente, String estado, String observacion) {
      this.id = id; this.hora = hora; this.paciente = paciente; this.estado = estado; this.observacion = observacion;
    }
  }
  //Clase anidada para mostrar los elementos de una cita
  public static final class CitaPacItem {
    public final int id;
    public final String fecha;
    public final String hora;
    public final String doctor;
    public final String estado;
    public final String observacion;
    
    public CitaPacItem(int id, String fecha, String hora, String doctor, String estado, String obs) {
      this.id = id; this.fecha = fecha; this.hora = hora; this.doctor = doctor; this.estado = estado; this.observacion = obs;
    }
  }

  // Agenda del doctor por cedula
  public List<AgendaItem> agendaDeDoctor(String cedulaDoctor, LocalDate fecha) {
    String sql = """
      SELECT c.id,
             DATE_FORMAT(c.hora, '%H:%i') AS hhmm,
             COALESCE(
               NULLIF(TRIM(CONCAT_WS(' ', p.nombre1, p.nombre2, p.apellido1, p.apellido2)), ''),
               p.cedula
             ) AS paciente,
             c.estado,
             c.observacion
      FROM Cita c
      JOIN Doctor d   ON d.id = c.doctor_id
      JOIN Paciente p ON p.id = c.paciente_id
      WHERE d.cedula = ? AND c.fecha = ?
      ORDER BY c.hora
      """;
    List<AgendaItem> out = new ArrayList<>();
    //Conexion con la base de datos y envio de solicitud
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, cedulaDoctor);
      ps.setDate(2, Date.valueOf(fecha));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new AgendaItem(
              rs.getInt("id"),
              rs.getString("hhmm"),
              rs.getString("paciente"),
              rs.getString("estado"),
              rs.getString("observacion")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando agenda", e);
    }
  }

  // Crear cita por IDs
  public int crear(int pacienteId, int doctorId, LocalDate fecha, LocalTime hora, String estado, String obs) {
    String sql = "INSERT INTO Cita (paciente_id, doctor_id, fecha, hora, estado, observacion) VALUES (?,?,?,?,?,?)";
    //Insertamos datos en cita y generamos una clave autoincremental
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, pacienteId);
      ps.setInt(2, doctorId);
      ps.setDate(3, Date.valueOf(fecha));
      ps.setTime(4, Time.valueOf(hora));
      ps.setString(5, estado);
      ps.setString(6, obs);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new IllegalStateException("No se generó ID de Cita");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando cita", e);
    }
  }

  // Crear cita con la cedula del paciente y del doctor
  public int crearPorCodigos(String cedulaPaciente, String cedulaDoctor, LocalDate fecha, LocalTime hora, String estado, String obs) {
    String sql = """
      INSERT INTO Cita (paciente_id, doctor_id, fecha, hora, estado, observacion)
      SELECT p.id, d.id, ?, ?, ?, ?
      FROM Paciente p
      JOIN Doctor   d ON 1=1
      WHERE p.cedula=? AND d.cedula=?
      """;
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setDate(1, Date.valueOf(fecha));
      ps.setTime(2, Time.valueOf(hora));
      ps.setString(3, estado);
      ps.setString(4, obs);
      ps.setString(5, cedulaPaciente);
      ps.setString(6, cedulaDoctor);
      int n = ps.executeUpdate();
      if (n == 0) throw new IllegalArgumentException("Paciente o Doctor no encontrados por cédula");
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new IllegalStateException("No se generó ID de Cita");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando cita por códigos", e);
    }
  }

  // Cambia el estado de la cita
  public void actualizarEstado(int citaId, String nuevoEstado) {
    String sql = "UPDATE Cita SET estado=? WHERE id=?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, nuevoEstado);
      ps.setInt(2, citaId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando estado", e);
    }
  }

  // Listar citas del paciente
  public List<CitaPacItem> citasDePaciente(int pacienteId, LocalDate from, LocalDate to) {
    String base = """
      SELECT c.id,
             DATE_FORMAT(c.fecha, '%Y-%m-%d') AS f,
             DATE_FORMAT(c.hora,  '%H:%i')     AS h,
             COALESCE(NULLIF(TRIM(CONCAT_WS(' ', d.nombre1, d.nombre2, d.apellido1, d.apellido2)), ''), d.cedula) AS doctor,
             c.estado,
             c.observacion
      FROM Cita c
      JOIN Doctor d ON d.id = c.doctor_id
      WHERE c.paciente_id=?
      """;
    StringBuilder sb = new StringBuilder(base);
    if (from != null) sb.append(" AND c.fecha >= ?");
    if (to   != null) sb.append(" AND c.fecha <= ?");
    sb.append(" ORDER BY c.fecha, c.hora");
    List<CitaPacItem> out = new ArrayList<>();
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sb.toString())) {
      int i = 1;
      ps.setInt(i++, pacienteId);
      if (from != null) ps.setDate(i++, Date.valueOf(from));
      if (to   != null) ps.setDate(i++, Date.valueOf(to));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new CitaPacItem(
              rs.getInt("id"),
              rs.getString("f"),
              rs.getString("h"),
              rs.getString("doctor"),
              rs.getString("estado"),
              rs.getString("observacion")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando citas del paciente: " + e.getMessage(), e);
    }
  }

  // Crear cita desde la perspectiva del paciente: doctor por CÉDULA
  public int crearPorPaciente(int pacienteId, String cedulaDoctor, LocalDate fecha, LocalTime hora, String observacion) {
    String BDoc = "SELECT id FROM Doctor WHERE cedula = ?";
    try (Connection cn = Db.get()) {
      int doctorId;
      try (PreparedStatement ps = cn.prepareStatement(BDoc)) {
        ps.setString(1, cedulaDoctor);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) throw new IllegalArgumentException("Doctor no encontrado por cédula");
          doctorId = rs.getInt(1);
        }
      }
      return crear(pacienteId, doctorId, fecha, hora, "Pendiente", observacion);
    } catch (SQLException e) {
      throw new RuntimeException("Error creando cita para paciente: " + e.getMessage(), e);
    }
  }

  //Cancelamos una cita desde la perspectiva del paciente
  public void cancelarPorPaciente(int citaId, int pacienteId) {
    String sql = "UPDATE Cita SET estado='Cancelada' WHERE id=? AND paciente_id=? AND estado IN ('Pendiente','Confirmada')";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, citaId);
      ps.setInt(2, pacienteId);
      int n = ps.executeUpdate();
      if (n == 0) throw new IllegalArgumentException("No se pudo cancelar (no es tu cita o ya no es cancelable).");
    } catch (SQLException e) {
      throw new RuntimeException("Error cancelando cita: " + e.getMessage(), e);
    }
  }
}
