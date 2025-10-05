package bdpryfinal;

import java.util.List;

public class HistoriaService {
  private final HistoriaDaoJdbc dao = new HistoriaDaoJdbc();

  public int agregarEntrada(int pacienteId, String descripcion) {
    return dao.agregarEntrada(pacienteId, descripcion);
  }

  public List<HistoriaDaoJdbc.HistoriaItem> listarHistoria(int pacienteId) {
    return dao.listarHistoria(pacienteId);
  }
}
