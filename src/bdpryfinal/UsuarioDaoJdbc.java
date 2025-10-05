package bdpryfinal;

import java.sql.*;

/** DAO para tabla Usuario (login/registro) y utilidades relacionadas. */
public class UsuarioDaoJdbc {

  public static final class User {
    public final int id;
    public final String username;
    public final String nombre; // nombre completo (si existe), o username
    public final String rol;

    public User(int id, String username, String nombre, String rol) {
      this.id = id; this.username = username; this.nombre = nombre; this.rol = rol;
    }
  }

  /** Crea un usuario. La columna 'nombre' NO existe en usuario; se usa para nombrar doctor/paciente vía triggers. */
  public int crearUsuario(String username, String nombre, String password, String rol) {
    final String sql = "INSERT INTO Usuario(username, password, rol) VALUES (?,?,?)";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, username);
      ps.setString(2, password);
      ps.setString(3, rol);
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) {
        if (gk.next()) return gk.getInt(1);
      }
      throw new IllegalStateException("No se obtuvo id del usuario.");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando usuario: " + e.getMessage(), e);
    }
  }

  /** Devuelve un user por username con un 'nombre' derivado de doctor/paciente si existe. */
  public User encontrarPorUser(String username) {
    final String sql = """
      SELECT u.id, u.username, u.rol,
             COALESCE(
               NULLIF(TRIM(CONCAT_WS(' ', d.nombre1,d.nombre2,d.apellido1,d.apellido2)),''),
               NULLIF(TRIM(CONCAT_WS(' ', p.nombre1,p.nombre2,p.apellido1,p.apellido2)),''),
               u.username
             ) AS nombre
      FROM Usuario u
      LEFT JOIN Doctor d ON d.usuario_id = u.id
      LEFT JOIN Paciente p ON p.usuario_id = u.id
      WHERE u.username = ?
      """;
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("nombre"),
            rs.getString("rol")
        );
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando usuario: " + e.getMessage(), e);
    }
  }

  public boolean verificarPassword(String username, String password) {
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return false;
        String pw = rs.getString(1);
        return (pw != null && pw.equals(password));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando password: " + e.getMessage(), e);
    }
  }

  public Integer pacienteIdPorUsuarioId(int usuarioId) {
    final String sql = "SELECT id FROM Paciente WHERE usuario_id = ?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando paciente_id: " + e.getMessage(), e);
    }
  }

  public Integer doctorIdPorUsuarioId(int usuarioId) {
    final String sql = "SELECT id FROM Doctor WHERE usuario_id = ?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando doctor_id: " + e.getMessage(), e);
    }
  }

  public int abrirSesion(int usuarioId) {
    return new SesionDaoJdbc().abrirSesion(usuarioId);
  }
  public void cerrarSesion(int sesionId) {
    new SesionDaoJdbc().cerrarSesion(sesionId);
  }
}
