package bdpryfinal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad de conexiones JDBC a MySQL.
 * - Carga explícita del driver (avance).
 * - Permite configurar URL/USER/PASS vía System properties (final),
 *   con valores por defecto amigables para XAMPP.
 * - Devuelve una conexión NUEVA en cada get() (compat con try-with-resources de los DAOs).
 */
public final class Db {

  // Fallbacks (puedes sobreescribir con -DDB_URL=..., -DDB_USER=..., -DDB_PASS=...)
  private static final String DEFAULT_URL  =
      "jdbc:mysql://127.0.0.1:3306/clinica?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
  private static final String DEFAULT_USER = "root";
  private static final String DEFAULT_PASS = "";

  static {
    try {
      // Driver del conector MySQL (mysql-connector-j)
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("No se encontró el driver MySQL en el classpath", e);
    }
  }

  private Db() {}

  /** Obtiene una conexión nueva. Recuerda cerrar con try-with-resources. */
  public static Connection get() throws SQLException {
    String url  = System.getProperty("DB_URL",  DEFAULT_URL);
    String user = System.getProperty("DB_USER", DEFAULT_USER);
    String pass = System.getProperty("DB_PASS", DEFAULT_PASS);
    return DriverManager.getConnection(url, user, pass);
  }

  /** (Opcional) Método de conveniencia si alguna vez guardas una conexión y quieres cerrarla sin propagar. */
  public static void closeQuiet(Connection c) {
    if (c != null) {
      try { c.close(); } catch (Exception ignore) {}
    }
  }
}
