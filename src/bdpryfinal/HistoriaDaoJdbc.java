package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO mínimo para Historia Clínica (avance). */
public class HistoriaDaoJdbc {

  public static final class HistoriaItem {
    public final int id;           // historia_nota.id (o similar)
    public final String descripcion;
    public final Timestamp creadaEn;

    public HistoriaItem(int id, String descripcion, Timestamp creadaEn) {
      this.id = id; this.descripcion = descripcion; this.creadaEn = creadaEn;
    }
  }

  /** Asegura la existencia de la historia_clinica del paciente (trigger suele crearla). */
  public int asegurarHistoriaId(int pacienteId) {
    try (Connection cn = Db.get()) {
      try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM historia_clinica WHERE paciente_id=?")) {
        ps.setInt(1, pacienteId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getInt(1);
        }
      }
      try (PreparedStatement ins = cn.prepareStatement("INSERT INTO historia_clinica(paciente_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
        ins.setInt(1, pacienteId);
        ins.executeUpdate();
        try (ResultSet gk = ins.getGeneratedKeys()) {
          if (gk.next()) return gk.getInt(1);
        }
      }
      throw new IllegalStateException("No se pudo asegurar historia para paciente " + pacienteId);
    } catch (SQLException e) {
      throw new RuntimeException("Error asegurando historia: " + e.getMessage(), e);
    }
  }

  /** Inserta una línea/entrada en la historia (si tu esquema tiene historia_nota). */
  public int agregarEntrada(int pacienteId, String descripcion) {
    if (descripcion == null || descripcion.isBlank()) throw new IllegalArgumentException("Descripción requerida");
    try (Connection cn = Db.get()) {
      int historiaId = asegurarHistoriaId(pacienteId);
      // Tabla 'historia_nota' esperada: id, historia_id, descripcion, creada_en
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO historia_nota(historia_id, descripcion) VALUES (?,?)",
          Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, historiaId);
        ps.setString(2, descripcion);
        ps.executeUpdate();
        try (ResultSet gk = ps.getGeneratedKeys()) {
          if (gk.next()) return gk.getInt(1);
        }
      }
      throw new IllegalStateException("No se obtuvo id de la entrada de historia.");
    } catch (SQLException e) {
      throw new RuntimeException("Error agregando entrada de historia: " + e.getMessage(), e);
    }
  }

  /** Lista entradas de la historia (si existe la tabla historia_nota). */
  public List<HistoriaItem> listarHistoria(int pacienteId) {
    final String sql = """
      SELECT hn.id, hn.descripcion, hn.creada_en
      FROM historia_nota hn
      JOIN historia_clinica hc ON hc.id = hn.historia_id
      WHERE hc.paciente_id = ?
      ORDER BY hn.creada_en
      """;
    List<HistoriaItem> out = new ArrayList<>();
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, pacienteId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new HistoriaItem(rs.getInt("id"), rs.getString("descripcion"), rs.getTimestamp("creada_en")));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando historia clínica: " + e.getMessage(), e);
    }
  }
}
