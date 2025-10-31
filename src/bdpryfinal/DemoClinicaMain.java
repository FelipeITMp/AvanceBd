package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//Demo de la clinica
public class DemoClinicaMain {

  public static void main(String[] args) {
    //Creamos los objetos necesarios
    var udao     = new UsuarioDaoJdbc();
    var ddao     = new DoctorDaoJdbc();
    var pdao     = new PacienteDaoJdbc();
    var citas    = new CitaService();
    var notas    = new NotaService();
    var sdao     = new SesionDaoJdbc(); // Abrimos o cerramos sesion
    var historia = new HistoriaService(); //Historia clínica


    // Ingresamos datos a la bd
    String userDoc = "dr.alvarez";
    String passDoc = "segura2025";
    String cedDoc  = "1718132645";

    String userPac = "maria.rojas";
    String passPac = "maria2025";
    String cedPac  = "0923456789";

    Integer sesionId = null; //Inicializamos la sesion como null

    try {
      //Creamos los usuarios correspondientes
      int uDocId = udao.crearUsuario(userDoc, passDoc, "Doctor");
      int uPacId = udao.crearUsuario(userPac, passPac, "Paciente");
      System.out.printf("Usuarios creados  Doctor#%d  Paciente#%d%n", uDocId, uPacId);

      // Insertamos los datos faltantes para el perfil del doctor
      ddao.insertarPerfilDoctor(uDocId, cedDoc,
          "Diego", "Andrés", "Álvarez", "García",
          "Cardiología", "Sede Centro", "08:00-12:00",
          "dr.alvarez"+ "@gmail.com",
          "09945462125",
          "M"
      );
      //Insertamos el perfil del paciente
      pdao.insertarPerfilPaciente(uPacId, cedPac,
          "María", "Fernanda", "Rojas", "Vega",
          "maria.rojas" + "@gmail.com",
          "0984454512",
          "F",
          "Av. Libertad 123",
          LocalDate.of(1991, 5, 17)
      );

      System.out.println("Perfiles actualizados con cédula.");
      
      //Abrimos sesion como doctor
      if (!udao.VerfPassword(userDoc, passDoc)) {
        throw new IllegalStateException("Credenciales inválidas del doctor demo");
      }
      sesionId = sdao.abrirSesion(uDocId);
      System.out.println("Sesión iniciada (Doctor) id=" + sesionId);

      // Guardamos los id de doctor y paciente
      Integer doctorId   = udao.doctorIdPorUsuario(uDocId);
      Integer pacienteId = udao.pacienteIdPorUsuario(uPacId);
      if (doctorId == null || pacienteId == null) {
        throw new IllegalStateException("No se pudo obtener doctorId/pacienteId desde Usuario.");
      }
      System.out.println("IDs internos  doctorId = "+doctorId+" pacienteId = " + pacienteId);
      
      //Creamos una nota para el paciente (bloc de notas)
      var notaView = notas.agregarNotaPorCodigo(cedPac,
          "El paciente esta sano" );
      System.out.println("Id de la nota = "+notaView.notaId+" Cantida de notas = "+notaView.notas.size());
      //System.out.printf("El paciente esta bien",notaView.notaId, notaView.notas.size());

      // Agregamos una nota en la historia clinica del paciente
      var histView = historia.agregarNotaPorCodigo(
          cedPac,
          "Ninguna",               // alergias
          "Ninguno",         // medicamentos
          "Chequeo general anual",              // motivo de consulta
          "Ninguna"  // recomendaciones
      );
      System.out.println("Historia clínica actualizada (historiaId="
          + histView.historiaId + ", notas=" + histView.notas.size() + ")");

      // Creamos dos citas con la cedula del paciente y el doctor
      LocalDate fecha = LocalDate.now().plusDays(1);
      int cita1 = citas.crearPorCodigos(cedPac, cedDoc, fecha, LocalTime.of(9, 30),
          "Pendiente", "Chequeo general anual");
      int cita2 = citas.crearPorCodigos(cedPac, cedDoc, fecha, LocalTime.of(11, 0),
          "Pendiente", "Control de presión arterial");
      System.out.printf("Citas creadas  #%d y #%d%n", cita1, cita2);

      // Cambiamos el estado de la primera cita y cancelamos la segunda
      citas.actualizarEstado(cita1, "Confirmada");
      System.out.println("Cita #" + cita1 + " actualizada a Confirmada.");
      citas.cancelarPorPaciente(cita2, pacienteId);
      System.out.println("Cita #" + cita2 + " cancelada por el paciente.");
      
    } catch (Exception e) {
      System.out.println("Hubo un error");
      e.printStackTrace(System.out);
      System.exit(1);
    } finally {
      //Cerramos sesion
      if (sesionId != null) {
        try {
          sdao.cerrarSesion(sesionId);
          System.out.println("Sesión cerrada id=" + sesionId);
        } catch (Exception ignore) { }
      }
    }
  }

}
