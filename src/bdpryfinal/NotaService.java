package bdpryfinal;

import java.util.List;

public class NotaService {

  private final NotaDaoJdbc dao = new NotaDaoJdbc();

  public int agregarTexto(int pacienteId, String texto) {
    return dao.agregarTexto(pacienteId, texto);
  }

  public List<NotaDaoJdbc.NotaItem> listarNotas(int pacienteId) {
    return dao.listarNotas(pacienteId);
  }
}
