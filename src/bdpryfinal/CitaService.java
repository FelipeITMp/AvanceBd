package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CitaService {
  private final DoctorDaoJdbc   doctorDao   = new DoctorDaoJdbc();
  private final PacienteDaoJdbc pacienteDao = new PacienteDaoJdbc();
  private final CitaDaoJdbc     citaDao     = new CitaDaoJdbc();

  /** Crea una cita usando códigos (DOC-001, PAC-001). Retorna id generado. */
  public int crearPorCodigos(String pacCodigo, String docCodigo,
                             LocalDate fecha, LocalTime hora,
                             String estado, String observacion) {
    int doctorId = doctorDao.idPorIdentificacion(docCodigo)
        .orElseThrow(() -> new IllegalArgumentException("Doctor no existe: " + docCodigo));
    int pacienteId = pacienteDao.idPorIdentificacion(pacCodigo)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe: " + pacCodigo));

    return citaDao.crearCita(pacienteId, doctorId, fecha, hora, estado, observacion);
  }

  /** Agenda de un doctor por fecha usando su código (DOC-001). */
  public List<CitaDaoJdbc.AgendaItem> agendaDeDoctor(String docCodigo, LocalDate fecha) {
    int doctorId = doctorDao.idPorIdentificacion(docCodigo)
        .orElseThrow(() -> new IllegalArgumentException("Doctor no existe: " + docCodigo));
    return citaDao.agendaDeDoctorEnFecha(doctorId, fecha);
  }

  /** Cambia el estado de una cita por id. */
  public void actualizarEstado(int citaId, String nuevoEstado) {
    citaDao.actualizarEstado(citaId, nuevoEstado);
  }
}
