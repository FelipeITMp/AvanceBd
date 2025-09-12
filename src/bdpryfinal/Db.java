package bdpryfinal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
  private static final String URL  =
      "jdbc:mysql://127.0.0.1:3306/clinica?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
  private static final String USER = "root"; // o 'app' si lo creaste
  private static final String PASS = "";     // en XAMPP suele ir vacío

  static {
    try {
      // Driver del conector que añadiste (mysql-connector-j-9.x)
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("No se encontró el driver MySQL en el classpath", e);
    }
  }

  private Db() {}

  /** Obtiene una conexión nueva. Recuerda cerrar con try-with-resources. */
  public static Connection get() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASS);
  }
}
