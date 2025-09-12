package bdpryfinal;

import java.sql.*;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de Doctor usando JDBC sin frameworks. */
public class DoctorDaoJdbc {

  /** Retorna el id del doctor por su codigo/identificacion (p. ej. "DOC-001"). */
  public Optional<Integer> idPorIdentificacion(String ident) {
    final String sql = "SELECT id FROM Doctor WHERE identificacion=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, ident);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(rs.getInt(1));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando Doctor.identificacion=" + ident, e);
    }
  }
  
  public void completarPerfilPorUsuarioId(int usuarioId, String especialidad, String sede, String horario) {
  final String sql = "UPDATE Doctor SET especialidad=?, sede=?, horario=? WHERE usuario_id=?";
  try (java.sql.Connection con = Db.get();
       java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setString(1, especialidad);
    ps.setString(2, sede);
    ps.setString(3, horario);
    ps.setInt(4, usuarioId);
    int rows = ps.executeUpdate();
    if (rows == 0) throw new IllegalStateException("No existe perfil Doctor para usuarioId=" + usuarioId);
  } catch (java.sql.SQLException e) {
    throw new RuntimeException("Error actualizando perfil Doctor (usuarioId=" + usuarioId + ")", e);
  }
}
  
  // Dentro de DoctorDaoJdbc.java


public static class DoctorItem {
  public final int id;
  public final String identificacion;
  public final String nombre;
  public final String especialidad;

  public DoctorItem(int id, String identificacion, String nombre, String especialidad) {
    this.id = id; this.identificacion = identificacion; this.nombre = nombre; this.especialidad = especialidad;
  }
}

public List<DoctorItem> listarTodos() {
  final String sql = "SELECT id, identificacion, nombre, especialidad FROM Doctor ORDER BY nombre";
  List<DoctorItem> out = new ArrayList<>();
  try (Connection con = Db.get();
       PreparedStatement ps = con.prepareStatement(sql);
       ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {
      out.add(new DoctorItem(
          rs.getInt("id"),
          rs.getString("identificacion"),
          rs.getString("nombre"),
          rs.getString("especialidad")
      ));
    }
  } catch (SQLException e) {
    throw new RuntimeException("Error listando doctores", e);
  }
  return out;
}

}
