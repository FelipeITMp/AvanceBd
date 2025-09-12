package bdpryfinal;

import java.sql.*;
import java.util.Optional;

/** Acceso a datos de Paciente usando JDBC. */
public class PacienteDaoJdbc {

  /** Retorna el id del paciente por su identificación (ej. "PAC-001"). */
  public Optional<Integer> idPorIdentificacion(String ident) {
    final String sql = "SELECT id FROM Paciente WHERE identificacion=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, ident);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(rs.getInt(1));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando Paciente.identificacion=" + ident, e);
    }
  }
  
  public void completarPerfilPorUsuarioId(int usuarioId,
   java.time.LocalDate fechaNacimiento,
   String genero, String telefono, String direccion) {
  final String sql = "UPDATE Paciente SET fecha_nacimiento=?, genero=?, telefono=?, direccion=? WHERE usuario_id=?";
  try (java.sql.Connection con = Db.get();
       java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setDate(1, java.sql.Date.valueOf(fechaNacimiento));
    ps.setString(2, genero);
    ps.setString(3, telefono);
    ps.setString(4, direccion);
    ps.setInt(5, usuarioId);
    int rows = ps.executeUpdate();
    if (rows == 0) throw new IllegalStateException("No existe perfil Paciente para usuarioId=" + usuarioId);
  } catch (java.sql.SQLException e) {
    throw new RuntimeException("Error actualizando perfil Paciente (usuarioId=" + usuarioId + ")", e);
  }
}
  
}
