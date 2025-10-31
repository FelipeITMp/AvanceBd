package bdpryfinal;

import java.sql.*;

//Clase de sesion
public class SesionDaoJdbc {
    
  //Metodo interno para abrir sesion
  public int abrirSesion(int usuarioId) {
    //Solicitud de insercion de un usuario en la tabla Sesion
    final String sql = "INSERT INTO Sesion(usuario_id, estado) VALUES (?, 'Activa')";
    //Hacemos la insercion
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
    //Solicitud para cambiar el estado de sesion a cerrada
    final String sql = "UPDATE Sesion SET estado='Cerrada' WHERE id=?";
    //Hacemos el cambio de estado
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, sesionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error cerrando sesión: " + e.getMessage(), e);
    }
  }
}
