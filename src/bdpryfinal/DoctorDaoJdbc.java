package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDaoJdbc {

  //Clase anidada para mostrar datos de doctor
  public static final class DoctorItem {
    public final int id;
    public final String cedula;
    public final String nombre;
    public final String especialidad;
//Metodo constructor
    public DoctorItem(int id, String cedula, String nombre, String especialidad) {
      this.id = id;
      this.cedula = cedula;
      this.nombre = nombre;
      this.especialidad = especialidad;
    }

    @Override public String toString() {
      return nombre + (especialidad == null || especialidad.isBlank() ? "" : " (" + especialidad + ")")
          + " [" + cedula + "]";
    }
  }

//Insertamos el perfil del doctor

  public void insertarPerfilDoctor(int usuarioId,
                                   String cedula,
                                   String nombre1,
                                   String nombre2,
                                   String apellido1,
                                   String apellido2,
                                   String especialidad,
                                   String sede,
                                   String horario,
                                   String correo,
                                   String telefono,
                                   String genero) {
//Solicitud de insercion de doctor
    final String sql =
        "INSERT INTO Doctor (" +
        "  cedula, nombre1, nombre2, apellido1, apellido2, " +
        "  especialidad, sede, horario, correo, telefono, genero, usuario_id" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE " +
        "  cedula=VALUES(cedula), " +
        "  nombre1=VALUES(nombre1), " +
        "  nombre2=VALUES(nombre2), " +
        "  apellido1=VALUES(apellido1), " +
        "  apellido2=VALUES(apellido2), " +
        "  especialidad=VALUES(especialidad), " +
        "  sede=VALUES(sede), " +
        "  horario=VALUES(horario), " +
        "  correo=VALUES(correo), " +
        "  telefono=VALUES(telefono), " +
        "  genero=VALUES(genero)";
//Conexion e insercion de doctor
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1,  blankToNull(cedula));
      ps.setString(2,  blankToNull(nombre1));
      ps.setString(3,  blankToNull(nombre2));
      ps.setString(4,  blankToNull(apellido1));
      ps.setString(5,  blankToNull(apellido2));
      ps.setString(6,  blankToNull(especialidad));
      ps.setString(7,  blankToNull(sede));
      ps.setString(8,  blankToNull(horario));
      ps.setString(9,  blankToNull(correo));
      ps.setString(10, blankToNull(telefono));
      ps.setString(11, blankToNull(genero));
      ps.setInt(12,    usuarioId);

      ps.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("Error insertando/actualizando perfil Doctor", e);
    }
  }
//Completamos el perfil del doctor con los datos que pueden llegar a ser nulos
  public void completarPerfilPorUsuarioId(int usuarioId,
                                          String especialidad, String sede, String horario, String genero) {
    //Actualizamos el doctor
    final String sql = "UPDATE Doctor SET especialidad=?, sede=?, horario=?, genero=? WHERE usuario_id=?";
    try (Connection con = Db.get(); PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, blankToNull(especialidad));
      ps.setString(2, blankToNull(sede));
      ps.setString(3, blankToNull(horario));
      ps.setString(4, blankToNull(genero));
      ps.setInt(5, usuarioId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error completando perfil Doctor", e);
    }
  }

//Listamos todos los doctores

  public List<DoctorItem> listarTodos() {
      //Solicitud para buscar todos los doctores ordenados por el nombre
    final String sql =
        "SELECT id, cedula, " +
        "COALESCE(NULLIF(TRIM(CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2)), ''), cedula) AS nombre_comp, " +
        "especialidad " +
        "FROM Doctor " +
        "ORDER BY nombre_comp";
    List<DoctorItem> out = new ArrayList<>();
    //Nos conectamos a la bd y extraemos los datos correspondientes
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        out.add(new DoctorItem(
            rs.getInt("id"),
            rs.getString("cedula"),
            rs.getString("nombre_comp"),
            rs.getString("especialidad")
        ));
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando doctores", e);
    }
  }

  //Verificamos si una cadena es nula y retorna un string vacio
  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }
}
