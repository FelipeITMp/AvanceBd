package bdpryfinal;


public final class SessionContext {
  //Usuario de la sesion actual
  public static UsuarioDaoJdbc.User currentUser;

  // Identificadores de sesión y posible paciente o doctor
  public static  Integer currentSesionId;
  public static Integer pacienteId; //si rol= Paciente
  public static Integer doctorId; //si rol= Doctor

  //Limpiamos los datos que tengamos almacenados
  public static void clear() {
    currentUser = null;
    currentSesionId = null;
    pacienteId = null;
    doctorId = null;
  }
}
