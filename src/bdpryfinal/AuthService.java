package bdpryfinal;

public class AuthService {
  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
  private final SesionDaoJdbc  sesionDao  = new SesionDaoJdbc();

  /** Resultado de login: usuario autenticado + id de sesión abierta. */
  public record LoginResult(UsuarioDaoJdbc.User user, int sesionId) {}

  /** Login simple (texto plano por ahora). Abre sesión y devuelve datos. */
  public LoginResult login(String username, String passwordPlano) {
    var userOpt = usuarioDao.login(username, passwordPlano);
    if (userOpt.isEmpty()) {
      throw new IllegalArgumentException("Usuario o contraseña inválidos");
    }
    int sid = sesionDao.abrirSesion(userOpt.get().id);
    return new LoginResult(userOpt.get(), sid);
  }

  /** Cerrar sesión. */
  public void logout(int sesionId) {
    sesionDao.cerrarSesion(sesionId);
  }
}
