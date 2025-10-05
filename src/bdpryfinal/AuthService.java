package bdpryfinal;

import java.sql.*;

public class AuthService {

  public record LoginResult(UsuarioDaoJdbc.User user, int sesionId, Integer pacienteId, Integer doctorId) {}

  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
  private final SesionDaoJdbc  sesionDao  = new SesionDaoJdbc();

  public LoginResult login(String username, String password) {
    if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario requerido");
    if (password == null) password = "";
    var u = usuarioDao.encontrarPorUser(username);
    if (u == null) throw new IllegalArgumentException("Credenciales inválidas");
    if (!usuarioDao.verificarPassword(username, password)) throw new IllegalArgumentException("Credenciales inválidas");

    int sesionId = sesionDao.abrirSesion(u.id);

    Integer pid = usuarioDao.pacienteIdPorUsuarioId(u.id);
    Integer did = usuarioDao.doctorIdPorUsuarioId(u.id);

    return new LoginResult(u, sesionId, pid, did);
  }

  public void logout(Integer sesionId) {
    if (sesionId == null) return;
    try { sesionDao.cerrarSesion(sesionId); } catch (Exception ignore) {}
  }
}
