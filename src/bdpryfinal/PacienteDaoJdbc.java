package bdpryfinal;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PacienteDaoJdbc {

  /** Item ligero para UI/listados */
  public static final class PacienteItem {
    public final int id;
    public final String cedula;
    public final String nombre;

    public PacienteItem(int id, String cedula, String nombre) {
      this.id = id;
      this.cedula = cedula;
      this.nombre = nombre;
    }

    @Override public String toString() {
      return (nombre == null || nombre.isBlank() ? cedula : (nombre + " [" + cedula + "]"));
    }
  }

  /* ===================== Registro / Perfil ===================== */

  public void insertarPerfilPaciente(int usuarioId,
                                     String cedula,
                                     String nombre1,
                                     String nombre2,
                                     String apellido1,
                                     String apellido2,
                                     String correo,
                                     String telefono,
                                     String genero,
                                     String direccion,
                                     LocalDate fechaNacimiento) {

    final String sql =
        "INSERT INTO Paciente (" +
        "  cedula, nombre1, nombre2, apellido1, apellido2, " +
        "  correo, telefono, genero, direccion, fecha_nacimiento, usuario_id" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE " +
        "  cedula=VALUES(cedula), " +
        "  nombre1=VALUES(nombre1), " +
        "  nombre2=VALUES(nombre2), " +
        "  apellido1=VALUES(apellido1), " +
        "  apellido2=VALUES(apellido2), " +
        "  correo=VALUES(correo), " +
        "  telefono=VALUES(telefono), " +
        "  genero=VALUES(genero), " +
        "  direccion=VALUES(direccion), " +
        "  fecha_nacimiento=VALUES(fecha_nacimiento)";

    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1,  blankToNull(cedula));
      ps.setString(2,  blankToNull(nombre1));
      ps.setString(3,  blankToNull(nombre2));
      ps.setString(4,  blankToNull(apellido1));
      ps.setString(5,  blankToNull(apellido2));
      ps.setString(6,  blankToNull(correo));
      ps.setString(7,  blankToNull(telefono));
      ps.setString(8,  blankToNull(genero));
      ps.setString(9,  blankToNull(direccion));
      if (fechaNacimiento == null) ps.setNull(10, Types.DATE);
      else ps.setDate(10, Date.valueOf(fechaNacimiento));
      ps.setInt(11, usuarioId);

      ps.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("Error insertando/actualizando perfil Paciente", e);
    }
  }

  public void completarPerfilPorUsuarioId(int usuarioId,
                                          String direccion,
                                          LocalDate fechaNacimiento,
                                          String genero) {
    final String sql = "UPDATE Paciente SET direccion=?, fecha_nacimiento=?, genero=? WHERE usuario_id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, blankToNull(direccion));
      if (fechaNacimiento == null) ps.setNull(2, Types.DATE);
      else ps.setDate(2, Date.valueOf(fechaNacimiento));
      ps.setString(3, blankToNull(genero));
      ps.setInt(4, usuarioId);

      int rows = ps.executeUpdate();
      if (rows == 0) throw new IllegalStateException("No existe perfil Paciente para usuarioId=" + usuarioId);
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando perfil Paciente (usuarioId=" + usuarioId + ")", e);
    }
  }

  /* ===================== Consultas utilitarias ===================== */

  /** Buscar por cédula (forma canónica del avance final). */
  public PacienteItem encontrarPorCedula(String cedula) {
    final String sql =
        "SELECT id, cedula, " +
        "TRIM(CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2)) AS nombre " +
        "FROM Paciente WHERE cedula = ?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, cedula);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("Paciente no encontrado por cédula: " + cedula);
        return new PacienteItem(
            rs.getInt("id"),
            rs.getString("cedula"),
            rs.getString("nombre")
        );
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando paciente por cédula", e);
    }
  }

  /** Alias de compatibilidad con el final (misma semántica que 'encontrarPorCedula'). */
  public PacienteItem EncontrarPorCodigo(String codigo) {
    return encontrarPorCedula(codigo);
  }

  /** ID interno por cédula (útil para joins rápidos). */
  public int idPorCedula(String cedula) {
    final String sql = "SELECT id FROM Paciente WHERE cedula = ?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, cedula);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("Paciente no encontrado por cédula: " + cedula);
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando id de paciente por cédula", e);
    }
  }

  /** Búsqueda por nombre (like) devolviendo cédula y nombre compuesto. */
  public List<PacienteItem> buscarPorNombre(String filtro) {
    final String sql =
        "SELECT id, cedula, " +
        "TRIM(CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2)) AS nombre " +
        "FROM Paciente " +
        "WHERE CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2) LIKE ? " +
        "ORDER BY nombre";
    List<PacienteItem> out = new ArrayList<>();
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      String like = "%" + (filtro == null ? "" : filtro.trim()) + "%";
      ps.setString(1, like);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new PacienteItem(
              rs.getInt("id"),
              rs.getString("cedula"),
              rs.getString("nombre")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando pacientes por nombre", e);
    }
  }

  private static String blankToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
