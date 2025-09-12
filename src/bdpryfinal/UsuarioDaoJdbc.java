package bdpryfinal;

import java.sql.*;
import java.util.Optional;

public class UsuarioDaoJdbc {

  /** Proyección mínima de usuario para la app. */
  public static class User {
    public final int id;
    public final String username;
    public final String nombre;
    public final String rol;

    public User(int id, String username, String nombre, String rol) {
      this.id = id; this.username = username; this.nombre = nombre; this.rol = rol;
    }

    @Override public String toString() {
      return "User{id=%d, username=%s, nombre=%s, rol=%s}".formatted(id, username, nombre, rol);
    }
  }

  /** Busca por username. */
  public Optional<User> findByUsername(String username) {
    final String sql = "SELECT id, username, nombre, rol FROM Usuario WHERE username=?";
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
      throw new RuntimeException("Error consultando usuario " + username, e);
    }
  }

  /** Login plano (compara texto). Más adelante cambiamos a hash (BCrypt). */
  public Optional<User> login(String username, String passwordPlano) {
    final String sql = "SELECT id, username, nombre, rol, password FROM Usuario WHERE username=?";
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
  
  public int crearUsuario(String username, String nombre, String passwordPlano, String rol) {
  final String sql = "INSERT INTO Usuario(username, nombre, password, rol) VALUES (?,?,?,?)";
  try (Connection con = Db.get();
       PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    ps.setString(1, username);
    ps.setString(2, nombre);
    ps.setString(3, passwordPlano); // TODO: luego lo cambiamos por hash
    ps.setString(4, rol);           // 'Doctor' o 'Paciente'
    ps.executeUpdate();
    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
  } catch (SQLIntegrityConstraintViolationException dup) {
    throw new IllegalStateException("El username ya existe");
  } catch (SQLException e) {
    throw new RuntimeException("Error creando usuario", e);
  }
}
  
}
