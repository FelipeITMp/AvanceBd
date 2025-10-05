package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO para notas de paciente (bloc de notas simple). */
public class NotaDaoJdbc {

  public static final class NotaItem {
    public final int id;            // id de la entrada (paciente_nota.id)
    public final String texto;
    public final Timestamp creadaEn;

    public NotaItem(int id, String texto, Timestamp creadaEn) {
      this.id = id; this.texto = texto; this.creadaEn = creadaEn;
    }
  }

  /** Asegura que exista la fila en 'nota' para el paciente y devuelve su id. */
  private int asegurarNotaId(int pacienteId, Connection cn) throws SQLException {
    try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM nota WHERE paciente_id = ?")) {
      ps.setInt(1, pacienteId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt(1);
      }
    }
    try (PreparedStatement ins = cn.prepareStatement("INSERT INTO nota(paciente_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
      ins.setInt(1, pacienteId);
      ins.executeUpdate();
      try (ResultSet gk = ins.getGeneratedKeys()) {
        if (gk.next()) return gk.getInt(1);
      }
    }
    throw new IllegalStateException("No se pudo asegurar nota_id del paciente " + pacienteId);
  }

  /** Agrega un texto como una entrada (paciente_nota). */
  public int agregarTexto(int pacienteId, String texto) {
    if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Texto requerido");
    try (Connection cn = Db.get()) {
      int notaId = asegurarNotaId(pacienteId, cn);
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO paciente_nota(nota_id, texto) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, notaId);
        ps.setString(2, texto);
        ps.executeUpdate();
        try (ResultSet gk = ps.getGeneratedKeys()) {
          if (gk.next()) return gk.getInt(1);
        }
      }
      throw new IllegalStateException("No se obtuvo id de la nota del paciente.");
    } catch (SQLException e) {
      throw new RuntimeException("Error agregando nota: " + e.getMessage(), e);
    }
  }

  /** Lista entradas de notas de un paciente (orden cronológico). */
  public List<NotaItem> listarNotas(int pacienteId) {
    final String sql = """
      SELECT pn.id, pn.texto, pn.creada_en
      FROM paciente_nota pn
      JOIN nota n ON n.id = pn.nota_id
      WHERE n.paciente_id = ?
      ORDER BY pn.creada_en
      """;
    List<NotaItem> out = new ArrayList<>();
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, pacienteId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new NotaItem(rs.getInt("id"), rs.getString("texto"), rs.getTimestamp("creada_en")));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando notas: " + e.getMessage(), e);
    }
  }
}
