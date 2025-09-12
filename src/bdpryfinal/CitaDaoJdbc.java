package bdpryfinal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de Cita: listar agenda, crear y actualizar estado. */
public class CitaDaoJdbc {

  /** Fila para mostrar en la agenda (simplificada). */
  public static class AgendaItem {
    public final String hora;        // HH:mm
    public final String paciente;    // nombre del paciente
    public final String estado;      // 'Pendiente','Confirmada',...
    public final String observacion; // puede ser null

    public AgendaItem(String hora, String paciente, String estado, String observacion) {
      this.hora = hora;
      this.paciente = paciente;
      this.estado = estado;
      this.observacion = observacion;
    }

    @Override public String toString() {
      return hora + " | " + paciente + " | " + estado +
             (observacion == null || observacion.isBlank() ? "" : " | " + observacion);
    }
  }

  /** Devuelve la agenda del doctor (por id) en una fecha dada. */
  public List<AgendaItem> agendaDeDoctorEnFecha(int doctorId, LocalDate fecha) {
    final String sql =
        "SELECT TIME_FORMAT(c.hora, '%H:%i') AS hhmm, p.nombre AS paciente, c.estado, c.observacion " +
        "FROM Cita c JOIN Paciente p ON p.id = c.paciente_id " +
        "WHERE c.doctor_id = ? AND c.fecha = ? " +
        "ORDER BY c.hora";

    List<AgendaItem> out = new ArrayList<>();
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, doctorId);
      ps.setDate(2, Date.valueOf(fecha));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new AgendaItem(
              rs.getString("hhmm"),
              rs.getString("paciente"),
              rs.getString("estado"),
              rs.getString("observacion")
          ));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error leyendo agenda del doctorId=" + doctorId + " en " + fecha, e);
    }
    return out;
  }

  /** Crea una cita. Retorna el id generado.
   *  Lanza IllegalStateException si la franja del doctor ya está ocupada. */
  public int crearCita(int pacienteId, int doctorId,
                       LocalDate fecha, LocalTime hora,
                       String estado, String observacion) {
    final String sql = "INSERT INTO Cita(paciente_id, doctor_id, fecha, hora, estado, observacion) " +
                       "VALUES (?,?,?,?,?,?)";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, pacienteId);
      ps.setInt(2, doctorId);
      ps.setDate(3, Date.valueOf(fecha));
      ps.setTime(4, Time.valueOf(hora));
      ps.setString(5, estado);         // 'Pendiente','Confirmada','Cancelada','Atendida'
      ps.setString(6, observacion);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getInt(1);
      }
    } catch (SQLIntegrityConstraintViolationException dup) { // UNIQUE (doctor_id,fecha,hora)
      throw new IllegalStateException("La franja del doctor ya está ocupada");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando cita", e);
    }
  }

  /** Actualiza el estado de una cita existente. */
  public void actualizarEstado(int citaId, String nuevoEstado) {
    final String sql = "UPDATE Cita SET estado=? WHERE id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, nuevoEstado);
      ps.setInt(2, citaId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando estado de cita id=" + citaId, e);
    }
  }
}
