package bdpryfinal;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//Clase paciente
public class PacienteDaoJdbc {

  //Clase anidada
  public static final class PacienteItem {
    public final int id;
    public final String cedula;
    public final String nombre;
    
    //Metodo constructor de clase anidada
    public PacienteItem(int id, String cedula, String nombre) {
      this.id = id;
      this.cedula = cedula;
      this.nombre = nombre;
    }

    @Override public String toString() {
      return (nombre == null || nombre.isBlank() ? cedula : (nombre + " [" + cedula + "]"));
    }
  }

// Insercion de perfil
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
//Solicitud de insercion
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
    
    //Conexion e insercion de datos a la bd
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

  //Completacion de datos que pueden llegar a ser nulos
  public void completarPerfilPorUsuarioId(int usuarioId,
                                          String direccion,
                                          LocalDate fechaNacimiento,
                                          String genero) {
    //Solicitud de actualizacion en la tabla Paciente
    final String sql = "UPDATE Paciente SET direccion=?, fecha_nacimiento=?, genero=? WHERE usuario_id=?";
    //Actualizacion de datos
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


  // Buscar un paciente por su cedula
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
  
  //Buscar el id del paciente por su cedula
  public int idPorCedula(String cedula) {
    //Solicitud de busqueda por cedula
    final String sql = "SELECT id FROM Paciente WHERE cedula = ?";
    //Busqueda y retorno de id del paciente por cedula
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

  //Lista de posibles coincidencias con un nombre de paciente
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
  
  //Metodo para verificar si un String esta en blanco
  private static String blankToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
