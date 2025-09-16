package bdpryfinal;

import java.sql.*;
import java.util.Optional;

/**
 * DAO para Usuario, adaptado al "avance" pero con mejoras del "final":
 * - No dependemos de Usuario.nombre (se resuelve por Paciente/Doctor o username).
 * - Overload crearUsuario(username, displayNameIgnorado, password, rol) para compatibilidad con el main.
 * - Login plano (por ahora) con obtención de nombre "mostrable" vía COALESCE.
 */
public class UsuarioDaoJdbc {

  /** Proyección mínima de usuario para la app. */
  public static class User {
    public final int id;
    public final String username;
    public final String nombre; // resuelto desde Paciente/Doctor o username
    public final String rol;

    public User(int id, String username, String nombre, String rol) {
      this.id = id; this.username = username; this.nombre = nombre; this.rol = rol;
    }

    @Override public String toString() {
      return "User{id=%d, username=%s, nombre=%s, rol=%s}".formatted(id, username, nombre, rol);
    }
  }

  /* ======================= Creación / existencia ======================= */

  /**
   * Crea usuario (tabla Usuario con columnas: username, password, rol).
   * En la versión final no se usa 'nombre' en la tabla Usuario.
   */
  public int crearUsuario(String username, String passwordPlano, String rol) {
    final String check = "SELECT 1 FROM Usuario WHERE username = ?";
    final String ins   = "INSERT INTO Usuario(username, password, rol) VALUES (?,?,?)";
    try (Connection con = Db.get()) {
      // Unicidad
      try (PreparedStatement ps = con.prepareStatement(check)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) throw new IllegalStateException("El username ya existe");
        }
      }
      // Insert
      try (PreparedStatement ps = con.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, username);
        ps.setString(2, passwordPlano); // TODO: reemplazar por hash (BCrypt) más adelante
        ps.setString(3, rol);           // 'Doctor' o 'Paciente'
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) return rs.getInt(1);
        }
      }
      throw new IllegalStateException("No se obtuvo id del usuario");
    } catch (SQLIntegrityConstraintViolationException dup) {
      throw new IllegalStateException("El username ya existe");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando usuario", e);
    }
  }

  /** Overload para compatibilidad con tu main: el 'nombre' se ignora (no existe en la tabla). */
  public int crearUsuario(String username, String displayNameIgnorado, String passwordPlano, String rol) {
    return crearUsuario(username, passwordPlano, rol);
  }

  /** (Opcional) Verifica existencia de username. */
  public boolean existeUsername(String username) {
    final String sql = "SELECT 1 FROM Usuario WHERE username = ?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando username", e);
    }
  }

  /* ======================= Lectura / login ======================= */

  /**
   * Busca usuario por username resolviendo 'nombre' con:
   *   COALESCE( nombre completo Paciente, nombre completo Doctor, username )
   */
  public Optional<User> findByUsername(String username) {
    final String sql =
        "SELECT u.id, u.username, u.rol, " +
        "COALESCE( " +
        "  NULLIF(TRIM(CONCAT_WS(' ', p.nombre1,p.nombre2,p.apellido1,p.apellido2)), ''), " +
        "  NULLIF(TRIM(CONCAT_WS(' ', d.nombre1,d.nombre2,d.apellido1,d.apellido2)), ''), " +
        "  u.username " +
        ") AS nombre " +
        "FROM Usuario u " +
        "LEFT JOIN Paciente p ON p.usuario_id = u.id " +
        "LEFT JOIN Doctor   d ON d.usuario_id = u.id " +
        "WHERE u.username = ?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new User(
              rs.getInt("id"),
              rs.getString("username"),
              rs.getString("nombre"),
              rs.getString("rol")
          ));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando usuario " + username, e);
    }
  }

  /**
   * Login plano (por ahora). Si coincide el password, devuelve User con 'nombre' resuelto.
   * Mantiene la firma Optional<User> propia de tu avance.
   */
  public Optional<User> login(String username, String passwordPlano) {
    final String sql =
        "SELECT u.id, u.username, u.rol, u.password, " +
        "COALESCE( " +
        "  NULLIF(TRIM(CONCAT_WS(' ', p.nombre1,p.nombre2,p.apellido1,p.apellido2)), ''), " +
        "  NULLIF(TRIM(CONCAT_WS(' ', d.nombre1,d.nombre2,d.apellido1,d.apellido2)), ''), " +
        "  u.username " +
        ") AS nombre " +
        "FROM Usuario u " +
        "LEFT JOIN Paciente p ON p.usuario_id = u.id " +
        "LEFT JOIN Doctor   d ON d.usuario_id = u.id " +
        "WHERE u.username = ?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return Optional.empty();
        String passDb = rs.getString("password");
        if (passDb != null && passDb.equals(passwordPlano)) {
          return Optional.of(new User(
              rs.getInt("id"),
              rs.getString("username"),
              rs.getString("nombre"),
              rs.getString("rol")
          ));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error en login de " + username, e);
    }
  }

  /* ======================= Utilidades opcionales ======================= */

  /** Devuelve el password tal como está guardado (para flujo de recuperación básico). */
  public String obtenerPasswordPlano(String username) {
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error leyendo contraseña", e);
    }
  }
}
