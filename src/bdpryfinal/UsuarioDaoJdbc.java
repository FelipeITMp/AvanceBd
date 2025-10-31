package bdpryfinal;

import java.sql.*;

public class UsuarioDaoJdbc {

  //Clase anidada user para mostrar datos de usuario
  public static final class User {
    public final int id;
    public final String username;
    public final String nombre;
    public final String rol;
    
    //Metodo constructor de user
    public User(int id, String username, String nombre, String rol) {
      this.id = id;
      this.username = username;
      this.nombre = nombre;
      this.rol = rol;
    }
  }

// Metodo para crear un usuario en la base de datos
public int crearUsuario(String username, String password, String rol) {
    String sql = "INSERT INTO Usuario (username, password, rol) VALUES (?,?,?)";
    //Creacion del usuario
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, username);
        ps.setString(2, password);
        ps.setString(3, rol);
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new IllegalStateException("No se generó id de usuario");
    } catch (SQLException e) {
        throw new RuntimeException("Error creando usuario: " + e.getMessage(), e);
    }
}

//Metodo para verificar si existe un usuario
  public boolean existeUsername(String username) {
    //Buscamos una fila de la tabla usuario que contenga el username solicitado  
    final String sql = "SELECT 1 FROM Usuario WHERE username = ?";
    //hacemos la solicitud
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando username", e);
    }
  }

  //Metodo para encontrar un usuario por username
  public User EncontrarPorUser(String username) {
      //Hacemos una query para que nos retorne todos los datos de un usuario si lo encuentra
      
    final String sql = 
        "SELECT u.id, u.username, u.rol, " +
        "COALESCE(" +
        "  NULLIF(TRIM(CONCAT_WS(' ', p.nombre1,p.nombre2,p.apellido1,p.apellido2)), '')," +
        "  NULLIF(TRIM(CONCAT_WS(' ', d.nombre1,d.nombre2,d.apellido1,d.apellido2)), '')," +
        "  u.username" +
        ") AS nombre " +
        "FROM Usuario u " +
        "LEFT JOIN Paciente p ON p.usuario_id = u.id " +
        "LEFT JOIN Doctor   d ON d.usuario_id = u.id " +
        "WHERE u.username = ?";
    
    //Hacemos la respectiva conexion y busqueda
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) { 
        if (rs.next()) {
          return new User(
              rs.getInt("id"),
              rs.getString("username"),
              rs.getString("nombre"),
              rs.getString("rol")
          );
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando usuario", e); //Si hay error al establecer la conexion llama a este metodo
    }
  }

  //Verificamos la contraseña de un usuario
  public boolean VerfPassword(String username, String passwordPlano) {
    //Buscamos en la tabla usuario el username y buscamos que la contraseña coincida
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
    //Ejecutamos la solicitud
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return false; // si no hay filas para mostrar entonces retorna flaso sino continua
        String passDb = rs.getString("password"); //Aca obtenemos la contraseña en plano
        return passDb != null && passDb.equals(passwordPlano); //En este apartado verificamos que la contraseña no sea nula y que sea igual a la que le pasamos
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando password", e);
    }
  }

  // Devuelve paciente_id por usuario_id, o null si no está enlazado
  public Integer pacienteIdPorUsuario(int usuarioId) {
    //Busca un usuario si contiene el rol paciente en la tabla usuario_id
    final String sql = "SELECT id FROM Paciente WHERE usuario_id = ?";
    //Hacemos la busqueda
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error resolviendo paciente por usuario", e);
    }
  }

  // Devuelve doctor_id por usuario_id, o null si no está enlazado.
  public Integer doctorIdPorUsuario(int usuarioId) {
    //Busca un usuario si contiene el rol paciente en la tabla usuario_id
    final String sql = "SELECT id FROM Doctor WHERE usuario_id = ?";
    //Hacemos la busqueda
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error resolviendo doctor por usuario", e);
    }
  }
}
