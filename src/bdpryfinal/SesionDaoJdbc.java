package bdpryfinal;

import java.sql.*;

/** DAO básico para tabla Sesion. */
public class SesionDaoJdbc {

  public int abrirSesion(int usuarioId) {
    final String sql = "INSERT INTO Sesion(usuario_id, estado) VALUES (?, 'Activa')";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, usuarioId);
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) {
        if (gk.next()) return gk.getInt(1);
      }
      throw new IllegalStateException("No se obtuvo id de la sesión.");
    } catch (SQLException e) {
      throw new RuntimeException("Error abriendo sesión: " + e.getMessage(), e);
    }
  }

  public void cerrarSesion(int sesionId) {
    final String sql = "UPDATE Sesion SET estado='Cerrada' WHERE id=?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, sesionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error cerrando sesión: " + e.getMessage(), e);
    }
  }
}
