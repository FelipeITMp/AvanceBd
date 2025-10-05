package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CitaService {

  private final CitaDaoJdbc citaDao = new CitaDaoJdbc();

  public List<CitaDaoJdbc.AgendaItem> agendaDoctor(String cedulaDoctor, LocalDate fecha) {
    if (cedulaDoctor == null || cedulaDoctor.isBlank()) {
      throw new IllegalArgumentException("Cédula de doctor requerida");
    }
    if (fecha == null) {
      throw new IllegalArgumentException("Fecha requerida");
    }
    return citaDao.agendaDeDoctor(cedulaDoctor, fecha);
  }

  public int crear(int pacienteId, int doctorId, LocalDate fecha, LocalTime hora, String estado, String observacion) {
    validarCampos(fecha, hora, estado);
    return citaDao.crear(pacienteId, doctorId, fecha, hora, estado, observacion);
  }

  public int crearPorCodigos(String cedulaPaciente, String cedulaDoctor,
                             LocalDate fecha, LocalTime hora, String estado, String observacion) {
    if (cedulaPaciente == null || cedulaPaciente.isBlank())
      throw new IllegalArgumentException("Cédula de paciente requerida");
    if (cedulaDoctor == null || cedulaDoctor.isBlank())
      throw new IllegalArgumentException("Cédula de doctor requerida");
    validarCampos(fecha, hora, estado);
    return citaDao.crearPorCodigos(cedulaPaciente, cedulaDoctor, fecha, hora, estado, observacion);
  }

  public int crearPorPaciente(int pacienteId, String cedulaDoctor,
                              LocalDate fecha, LocalTime hora, String observacion) {
    if (pacienteId <= 0) throw new IllegalArgumentException("Paciente inválido");
    if (cedulaDoctor == null || cedulaDoctor.isBlank())
      throw new IllegalArgumentException("Cédula de doctor requerida");
    if (fecha == null) throw new IllegalArgumentException("Fecha requerida");
    if (hora == null) throw new IllegalArgumentException("Hora requerida");
    return citaDao.crearPorPaciente(pacienteId, cedulaDoctor, fecha, hora, observacion);
  }

  public void actualizarEstado(int citaId, String nuevoEstado) {
    if (citaId <= 0) throw new IllegalArgumentException("Cita inválida");
    if (nuevoEstado == null || nuevoEstado.isBlank()) throw new IllegalArgumentException("Estado requerido");
    citaDao.actualizarEstado(citaId, nuevoEstado);
  }

  public List<CitaDaoJdbc.CitaPacItem> citasPaciente(int pacienteId, LocalDate desde, LocalDate hasta) {
    if (pacienteId <= 0) throw new IllegalArgumentException("Paciente inválido");
    if (desde != null && hasta != null && hasta.isBefore(desde))
      throw new IllegalArgumentException("Rango de fechas inválido");
    return citaDao.citasDePaciente(pacienteId, desde, hasta);
  }

  public void cancelarPorPaciente(int citaId, int pacienteId) {
    if (citaId <= 0) throw new IllegalArgumentException("Cita inválida");
    if (pacienteId <= 0) throw new IllegalArgumentException("Paciente inválido");
    citaDao.cancelarPorPaciente(citaId, pacienteId);
  }

  private void validarCampos(LocalDate fecha, LocalTime hora, String estado) {
    if (fecha == null) throw new IllegalArgumentException("Fecha requerida");
    if (hora == null) throw new IllegalArgumentException("Hora requerida");
    if (estado == null || estado.isBlank()) throw new IllegalArgumentException("Estado requerido");
  }
}
