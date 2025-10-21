package bdpryfinal;

import java.util.List;

public class HistoriaService {
//Clase anidada
  public static final class HistoriaView {
    public final PacienteDaoJdbc.PacienteItem paciente;
    public final int historiaId;
    public final List<HistoriaDaoJdbc.NotaHistoria> notas;
    public HistoriaView(PacienteDaoJdbc.PacienteItem p, int historiaId, List<HistoriaDaoJdbc.NotaHistoria> notas) {
      this.paciente = p; this.historiaId = historiaId; this.notas = notas;
    }
  }

  //Creamos dos objetos para manejar los metodos
  private final PacienteDaoJdbc pacDao = new PacienteDaoJdbc();
  private final HistoriaDaoJdbc histDao = new HistoriaDaoJdbc();

  // Abre (o crea) la historia por cedula del paciente y lista sus notas
  public HistoriaView abrirPorCodigoPaciente(String cedPaciente) {
    if (cedPaciente == null || cedPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente requerido");
    var p = pacDao.encontrarPorCedula(cedPaciente);
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int hid = histDao.CrearHistoriaId(p.id);
    var notas = histDao.listarNotas(hid);
    return new HistoriaView(p, hid, notas);
  }

  //Agregamos nota completa
  public HistoriaView agregarNotaPorCodigo(String cedPaciente,
                                           String alergias,
                                           String medicamentos,
                                           String motivoConsulta,
                                           String recomendaciones) {
    var p = pacDao.encontrarPorCedula(cedPaciente);
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int hid = histDao.CrearHistoriaId(p.id);
    histDao.agregarNota(hid, alergias, medicamentos, motivoConsulta, recomendaciones);
    var notas = histDao.listarNotas(hid);
    return new HistoriaView(p, hid, notas);
  }
}
