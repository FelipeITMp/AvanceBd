package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO simple para Paciente (registro, búsquedas y utilidades), acoplado al avance con mejoras del final. */
public class PacienteDaoJdbc {

  /** DTO ligero para mostrar/usar en listas */
  public static final class PacienteItem {
    public final int id;
    public final String identificacion;
    public final String nombre; // nombre completo armado

    public PacienteItem(int id, String identificacion, String nombre) {
      this.id = id;
      this.identificacion = identificacion;
      this.nombre = nombre;
    }

    @Override public String toString() { return nombre + " [" + identificacion + "]"; }
  }

  /* ===================== Consultas básicas del avance ===================== */

  /** Retorna el id del paciente por su identificación (ej. "PAC-001"). */
  public Optional<Integer> idPorIdentificacion(String ident) {
    final String sql = "SELECT id FROM Paciente WHERE identificacion = ?";
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

  /* ===================== Registro / Perfil ===================== */

  /**
   * Inserta o actualiza el perfil del paciente (upsert) — versión completa del “final”.
   * Columnas esperadas:
   *  identificacion, cedula, nombre1, nombre2, apellido1, apellido2,
   *  correo, telefono, genero, direccion, fecha_nacimiento, usuario_id
   *
   * Usa ON DUPLICATE KEY UPDATE para convivir con trigger que cree placeholders (UNIQUE(usuario_id)).
   */
  public void insertarPerfilPaciente(int usuarioId,
                                     String identificacion,
                                     String cedula,
                                     String nombre1,
                                     String nombre2,
                                     String apellido1,
                                     String apellido2,
                                     String correo,
                                     String telefono,
                                     String genero,
                                     String direccion,
                                     java.time.LocalDate fechaNacimiento) {
    final String sql =
        "INSERT INTO Paciente (" +
        "  identificacion, cedula, nombre1, nombre2, apellido1, apellido2, " +
        "  correo, telefono, genero, direccion, fecha_nacimiento, usuario_id" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE " +
        "  identificacion=VALUES(identificacion), " +
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

    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {

      ps.setString(1,  identificacion);
      ps.setString(2,  cedula);
      ps.setString(3,  nombre1);
      ps.setString(4,  nullIfBlank(nombre2));
      ps.setString(5,  apellido1);
      ps.setString(6,  apellido2);
      ps.setString(7,  nullIfBlank(correo));
      ps.setString(8,  nullIfBlank(telefono));
      ps.setString(9,  nullIfBlank(genero));
      ps.setString(10, nullIfBlank(direccion));
      if (fechaNacimiento != null)
        ps.setDate(11, Date.valueOf(fechaNacimiento));
      else
        ps.setNull(11, Types.DATE);
      ps.setInt(12, usuarioId); // 🔑 importante: enlazar usuario

      ps.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("Error insertando/actualizando perfil Paciente", e);
    }
  }

  /**
   * Completa/actualiza campos del perfil por usuario_id (compat con tu menú del avance).
   * Firma de 5 parámetros: fechaNacimiento, genero, telefono, direccion.
   */
  public void completarPerfilPorUsuarioId(int usuarioId,
                                          java.time.LocalDate fechaNacimiento,
                                          String genero, String telefono,
                                          String direccion) {
    final String sql =
        "UPDATE Paciente SET fecha_nacimiento=?, genero=?, telefono=?, direccion=? " +
        "WHERE usuario_id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      if (fechaNacimiento != null) ps.setDate(1, Date.valueOf(fechaNacimiento));
      else                         ps.setNull(1, Types.DATE);
      ps.setString(2, nullIfBlank(genero));
      ps.setString(3, nullIfBlank(telefono));
      ps.setString(4, nullIfBlank(direccion));
      ps.setInt(5, usuarioId);

      int rows = ps.executeUpdate();
      if (rows == 0) throw new IllegalStateException("No existe perfil Paciente para usuarioId=" + usuarioId);
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando perfil Paciente (usuarioId=" + usuarioId + ")", e);
    }
  }

  /** Overload opcional con correo (si luego lo necesitas). */
  public void completarPerfilPorUsuarioId(int usuarioId,
                                          java.time.LocalDate fechaNacimiento,
                                          String genero, String telefono,
                                          String direccion, String correo) {
    final String sql =
        "UPDATE Paciente SET fecha_nacimiento=?, genero=?, telefono=?, direccion=?, correo=? " +
        "WHERE usuario_id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      if (fechaNacimiento != null) ps.setDate(1, Date.valueOf(fechaNacimiento));
      else                         ps.setNull(1, Types.DATE);
      ps.setString(2, nullIfBlank(genero));
      ps.setString(3, nullIfBlank(telefono));
      ps.setString(4, nullIfBlank(direccion));
      ps.setString(5, nullIfBlank(correo));
      ps.setInt(6, usuarioId);

      int rows = ps.executeUpdate();
      if (rows == 0) throw new IllegalStateException("No existe perfil Paciente para usuarioId=" + usuarioId);
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando perfil Paciente (usuarioId=" + usuarioId + ")", e);
    }
  }

  /* ===================== Consultas utilitarias (mejoras del final) ===================== */

  /** Busca el ID por código (identificación o cédula). Lanza IllegalArgumentException si no existe. */
  public static int findIdByCodigo(String codigoPaciente) {
    if (codigoPaciente == null || codigoPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente requerido");
    String sql = "SELECT id FROM Paciente WHERE identificacion = ? OR cedula = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      String cod = codigoPaciente.trim();
      ps.setString(1, cod);
      ps.setString(2, cod);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new IllegalArgumentException("Paciente no encontrado: " + codigoPaciente);
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando paciente por código: " + e.getMessage(), e);
    }
  }

  /** Devuelve un item por código (identificación o cédula), o null si no existe. */
  public PacienteItem findByCodigo(String codigoPaciente) {
    String sql =
        "SELECT id, identificacion, nombre1, nombre2, apellido1, apellido2 " +
        "FROM Paciente WHERE identificacion = ? OR cedula = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      String cod = codigoPaciente == null ? "" : codigoPaciente.trim();
      ps.setString(1, cod);
      ps.setString(2, cod);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String nombre = joinWithSpaces(
              rs.getString("nombre1"),
              rs.getString("nombre2"),
              rs.getString("apellido1"),
              rs.getString("apellido2")
          );
          return new PacienteItem(rs.getInt("id"), rs.getString("identificacion"), nombre);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando paciente", e);
    }
  }

  /** Búsqueda por nombre "completo" usando LIKE sobre la concatenación. */
  public List<PacienteItem> buscarPorNombre(String filtro) {
    String sql = "SELECT id, identificacion, " +
                 "CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2) AS nombre_comp " +
                 "FROM Paciente " +
                 "WHERE CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2) LIKE ? " +
                 "ORDER BY nombre_comp";
    List<PacienteItem> out = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, "%" + (filtro == null ? "" : filtro.trim()) + "%");
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new PacienteItem(
              rs.getInt("id"),
              rs.getString("identificacion"),
              rs.getString("nombre_comp")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando pacientes", e);
    }
  }

  /* ===================== Helpers internos ===================== */

  private static String joinWithSpaces(String... parts) {
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
      if (p != null) {
        String t = p.trim();
        if (!t.isEmpty()) {
          if (sb.length() > 0) sb.append(' ');
          sb.append(t);
        }
      }
    }
    return sb.toString();
  }

  private static String nullIfBlank(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
