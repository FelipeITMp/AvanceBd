package bdpryfinal;

/** Servicio de autenticación (acoplado a la versión de avance). */
public class AuthService {
  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
  private final SesionDaoJdbc  sesionDao  = new SesionDaoJdbc();

  /** Resultado de login: usuario autenticado + id de sesión abierta. */
  public record LoginResult(UsuarioDaoJdbc.User user, int sesionId) {}

  /**
   * Login con validaciones similares a la versión final:
   * - username requerido (no nulo/blank)
   * - password null -> "" (para evitar NPE)
   * - abre sesión y devuelve el par (usuario, sesionId)
   */
  public LoginResult login(String username, String passwordPlano) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Usuario requerido");
    }
    if (passwordPlano == null) passwordPlano = "";

    var userOpt = usuarioDao.login(username, passwordPlano);
    if (userOpt.isEmpty()) {
      // Mensaje compacto y consistente con la final
      throw new IllegalArgumentException("Credenciales invalidas");
    }

    int sid = sesionDao.abrirSesion(userOpt.get().id);
    return new LoginResult(userOpt.get(), sid);
  }

  /**
   * Cerrar sesión tolerante:
   * - acepta null y lo ignora
   * - no propaga errores menores del UPDATE
   */
  public void logout(Integer sesionId) {
    if (sesionId == null) return;
    try { sesionDao.cerrarSesion(sesionId); } catch (Exception ignore) {}
  }
}
