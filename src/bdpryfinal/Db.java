package bdpryfinal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
  //Conexion estatica que va a usar toda la app desde el metodo get()
  private static Connection shared;

  //Creamos la conexion
  public static Connection get() throws SQLException {
    if (shared == null || shared.isClosed()) {
      String url  = "jdbc:mysql://127.0.0.1:3306/clinica?useSSL=false&serverTimezone=UTC";
      String user = "root";
      String pass = "root";
      shared = DriverManager.getConnection(url, user, pass);
    }
    return shared;
  }

  // Cuando queremos cerrar la bd llamamos al metodo CerrarConexion
  public static void CerrarConexion() {
    if (shared != null) {
      try { shared.close();
      } 
      catch (Exception ignored) {}
      shared = null;
    }
  }
}
