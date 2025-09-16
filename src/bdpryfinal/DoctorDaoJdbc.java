package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de Doctor usando JDBC, acoplado al avance pero con mejoras de la versión final. */
public class DoctorDaoJdbc {

  /** Item ligero para UI (listas / ComboBox) */
  public static final class DoctorItem {
    public final int id;
    public final String identificacion; // código interno (DOC-001)
    public final String nombre;         // nombre completo armado
    public final String especialidad;

    public DoctorItem(int id, String identificacion, String nombre, String especialidad) {
      this.id = id;
      this.identificacion = identificacion;
      this.nombre = nombre;
      this.especialidad = especialidad;
    }

    @Override public String toString() {
      return nombre + (especialidad == null || especialidad.isBlank() ? "" : " (" + especialidad + ")")
          + " [" + identificacion + "]";
    }
  }

  /* ===================== Consultas básicas ===================== */

  /** Retorna el id del doctor por su código/identificación (p. ej. "DOC-001"). */
  public Optional<Integer> idPorIdentificacion(String ident) {
    final String sql = "SELECT id FROM Doctor WHERE identificacion = ?";
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

  /**
   * Devuelve todos los doctores ordenados por nombre completo.
   * Arma el nombre con CONCAT_WS(nombre1, nombre2, apellido1, apellido2) como en la versión final.
   */
  public List<DoctorItem> listarTodos() {
    final String sql =
        "SELECT id, identificacion, " +
        "CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2) AS nombre_comp, " +
        "especialidad " +
        "FROM Doctor " +
        "ORDER BY nombre_comp";
    List<DoctorItem> out = new ArrayList<>();
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        out.add(new DoctorItem(
            rs.getInt("id"),
            rs.getString("identificacion"),
            rs.getString("nombre_comp"),
            rs.getString("especialidad")
        ));
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando doctores", e);
    }
  }

  /* ===================== Registro / Perfil ===================== */

  /**
   * Inserta/actualiza el perfil del doctor (upsert) — versión completa del “final”.
   * Columnas esperadas en Doctor:
   *   identificacion, cedula, nombre1, nombre2, apellido1, apellido2,
   *   especialidad, sede, horario, correo, telefono, genero, usuario_id
   *
   * Usa ON DUPLICATE KEY UPDATE para convivir con el trigger que crea un placeholder (UNIQUE(usuario_id)).
   */
  public void insertarPerfilDoctor(int usuarioId,
                                   String identificacion,
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
    final String sql =
        "INSERT INTO Doctor (" +
        "  identificacion, cedula, nombre1, nombre2, apellido1, apellido2, " +
        "  especialidad, sede, horario, correo, telefono, genero, usuario_id" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE " +
        "  identificacion=VALUES(identificacion), " +
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

    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1,  nullIfBlank(identificacion));
      ps.setString(2,  nullIfBlank(cedula));
      ps.setString(3,  nullIfBlank(nombre1));
      ps.setString(4,  nullIfBlank(nombre2));
      ps.setString(5,  nullIfBlank(apellido1));
      ps.setString(6,  nullIfBlank(apellido2));
      ps.setString(7,  nullIfBlank(especialidad));
      ps.setString(8,  nullIfBlank(sede));
      ps.setString(9,  nullIfBlank(horario));
      ps.setString(10, nullIfBlank(correo));
      ps.setString(11, nullIfBlank(telefono));
      ps.setString(12, nullIfBlank(genero));
      ps.setInt(13, usuarioId);

      ps.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("Error insertando/actualizando perfil Doctor", e);
    }
  }

  /**
   * Completar/actualizar perfil por usuarioId — **versión de 3 parámetros**
   * para ser 100% compatible con tu menú actual (avance).
   * Ajusta únicamente especialidad, sede, horario.
   */
  public void completarPerfilPorUsuarioId(int usuarioId,
                                          String especialidad, String sede, String horario) {
    final String sql = "UPDATE Doctor SET especialidad=?, sede=?, horario=? WHERE usuario_id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, nullIfBlank(especialidad));
      ps.setString(2, nullIfBlank(sede));
      ps.setString(3, nullIfBlank(horario));
      ps.setInt(4, usuarioId);

      int rows = ps.executeUpdate();
      if (rows == 0) throw new IllegalStateException("No existe perfil Doctor para usuarioId=" + usuarioId);
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando perfil Doctor (usuarioId=" + usuarioId + ")", e);
    }
  }

  /* ===================== Helpers internos ===================== */

  private static String nullIfBlank(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
