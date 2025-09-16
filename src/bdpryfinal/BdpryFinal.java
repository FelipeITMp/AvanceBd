package bdpryfinal;

import java.util.Scanner;

public class BdpryFinal {

  public static void main(String[] args) {
    var auth   = new AuthService();
    var citaSrv= new CitaService();
    var udao   = new UsuarioDaoJdbc();
    var sc     = new Scanner(System.in);

    UsuarioDaoJdbc.User usuarioActual = null;
    Integer sesionId = null;

    while (true) {
      System.out.println("\n================ MENU ================");
      if (usuarioActual == null) {
        System.out.println("1) Crear cuenta");
        System.out.println("2) Iniciar sesion");
        System.out.println("0) Salir");
        System.out.print("Opcion: ");
        String op = sc.nextLine().trim();

        switch (op) {

          case "1" -> {
            System.out.print("Username: ");   String u = sc.nextLine().trim();
            System.out.print("Nombre (display, se ignora en BD): "); String n = sc.nextLine().trim();
            System.out.print("Password: ");   String p = sc.nextLine();
            System.out.print("Rol (Doctor|Paciente): "); String r = sc.nextLine().trim();

            if (!r.equals("Doctor") && !r.equals("Paciente")) {
              System.out.println("Rol invalido."); break;
            }

            try {
              // Overload que ignora 'nombre' (la tabla Usuario no lo guarda)
              int nuevoUsuarioId = udao.crearUsuario(u, n, p, r);
              System.out.println("Usuario creado id=" + nuevoUsuarioId);

              if (r.equals("Doctor")) {
                // Datos minimos para el perfil del doctor
                System.out.print("Especialidad (ej. Cardiologia): "); String esp = sc.nextLine().trim();
                System.out.print("Sede (ej. Central/Sur): ");         String sede= sc.nextLine().trim();
                System.out.print("Horario (ej. Lun-Vie 8:00-12:00): ");String hor = sc.nextLine().trim();

                new DoctorDaoJdbc().completarPerfilPorUsuarioId(nuevoUsuarioId, esp, sede, hor);
                System.out.println("Perfil de Doctor actualizado.");
              } else {
                // Datos minimos para el perfil del paciente
                System.out.print("Fecha de nacimiento (yyyy-MM-dd): ");
                java.time.LocalDate fnac = leerFecha(sc, "");
                System.out.print("Genero (F/M/Otro): ");  String gen = sc.nextLine().trim();
                System.out.print("Telefono: ");           String tel = sc.nextLine().trim();
                System.out.print("Direccion: ");          String dir = sc.nextLine().trim();

                new PacienteDaoJdbc().completarPerfilPorUsuarioId(nuevoUsuarioId, fnac, gen, tel, dir);
                System.out.println("Perfil de Paciente actualizado.");
              }
            } catch (IllegalStateException e) {
              System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
              System.out.println("Error creando cuenta: " + e.getMessage());
            }
          }

          case "2" -> {
            System.out.print("Username: "); String u = sc.nextLine().trim();
            System.out.print("Password: "); String p = sc.nextLine();
            try {
              var r = auth.login(u, p);
              usuarioActual = r.user();
              sesionId = r.sesionId();
              System.out.println("Login OK: " + usuarioActual);
            } catch (IllegalArgumentException e) {
              System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
              System.out.println("Error en login: " + e.getMessage());
            }
          }

          case "0" -> { System.out.println("Adios!"); return; }

          default -> System.out.println("Opcion invalida.");
        }

      } else {
        System.out.println("Usuario: " + usuarioActual.username + " (" + usuarioActual.rol + ")");
        System.out.println("1) Ver agenda por doctor y fecha");
        System.out.println("2) Crear cita (por codigos PAC/DOC)");
        System.out.println("3) Cambiar estado de una cita");
        System.out.println("4) Ver ultimas sesiones mias");
        System.out.println("9) Cerrar sesion");
        System.out.println("0) Salir");
        System.out.print("Opcion: ");
        String op = sc.nextLine().trim();

        switch (op) {
          case "1" -> { // Ver agenda por doctor/fecha
            String doc = elegirDoctorCodigo(sc);
            if (doc == null) break;
            var fecha = leerFechaYMD(sc);
            try {
              var items = citaSrv.agendaDeDoctor(doc, fecha);
              if (items.isEmpty()) System.out.println("(sin citas)");
              else items.forEach(System.out::println);
            } catch (Exception e) {
              System.out.println("Error: " + e.getMessage());
            }
          }

          case "2" -> { // Crear cita por codigos
            System.out.print("Codigo paciente (ej. PAC-001): ");
            String pac = sc.nextLine().trim();

            String doc = elegirDoctorCodigo(sc);
            if (doc == null) break;

            var fecha = leerFechaYMD(sc);
            var hora  = leerHora(sc, "Hora (HH:mm): ");
            String estado = elegirEstado(sc);
            System.out.print("Observacion: ");
            String obs = sc.nextLine();

            try {
              int id = citaSrv.crearPorCodigos(pac, doc, fecha, hora, estado, obs);
              System.out.println("Cita creada id=" + id);
            } catch (IllegalStateException | IllegalArgumentException e) {
              System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
              System.out.println("Error creando cita: " + e.getMessage());
            }
          }

          case "3" -> { // Cambiar estado
            System.out.print("Id de la cita: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            String estado = elegirEstado(sc);
            try {
              citaSrv.actualizarEstado(id, estado);
              System.out.println("Cita " + id + " -> " + estado);
            } catch (Exception e) {
              System.out.println("Error: " + e.getMessage());
            }
          }

          case "4" -> { // Mis ultimas sesiones
            try {
              var sdao = new SesionDaoJdbc();
              sdao.ultimasDelUsuario(usuarioActual.id, 10).forEach(System.out::println);
            } catch (Exception e) {
              System.out.println("Error: " + e.getMessage());
            }
          }

          case "9" -> { // Cerrar sesion
            auth.logout(sesionId);
            System.out.println("Sesion cerrada");
            usuarioActual = null; sesionId = null;
          }

          case "0" -> { System.out.println("Adios!"); return; }

          default -> System.out.println("Opcion invalida.");
        }
      }
    }
  }

  // ---------- helpers ----------

  private static java.time.LocalDate leerFecha(Scanner sc, String prompt) {
    while (true) {
      System.out.print(prompt);
      String s = sc.nextLine().trim();
      try { return java.time.LocalDate.parse(s); }
      catch (Exception e) { System.out.println("Formato invalido, usa yyyy-MM-dd"); }
    }
  }

  private static java.time.LocalTime leerHora(Scanner sc, String prompt) {
    while (true) {
      System.out.print(prompt);
      String s = sc.nextLine().trim() + ":00";
      try { return java.time.LocalTime.parse(s); }
      catch (Exception e) { System.out.println("Formato invalido, usa HH:mm"); }
    }
  }

  private static String elegirEstado(Scanner sc) {
    System.out.println("Estado: 1) Pendiente  2) Confirmada  3) Cancelada  4) Atendida");
    while (true) {
      System.out.print("Elige (1-4): ");
      String s = sc.nextLine().trim();
      switch (s) {
        case "1": return "Pendiente";
        case "2": return "Confirmada";
        case "3": return "Cancelada";
        case "4": return "Atendida";
        default:  System.out.println("Opcion invalida.");
      }
    }
  }

  private static String elegirDoctorCodigo(Scanner sc) {
    var ddao = new DoctorDaoJdbc();
    var lista = ddao.listarTodos();
    if (lista.isEmpty()) {
      System.out.println("No hay doctores registrados.");
      return null;
    }
    System.out.println("Elige un doctor:");
    for (int i = 0; i < lista.size(); i++) {
      var d = lista.get(i);
      System.out.printf("%d) %s - %s%s%n", i, d.identificacion, d.nombre,
          (d.especialidad == null || d.especialidad.isBlank()) ? "" : " (" + d.especialidad + ")");
    }
    int idx = leerEnteroEnRango(sc, "Opcion [0-" + (lista.size()-1) + "]: ", 0, lista.size()-1);
    return lista.get(idx).identificacion;
  }

  private static java.time.LocalDate leerFechaYMD(Scanner sc) {
    int anio  = leerEnteroEnRango(sc, "Anio (yyyy): ", 1900, 2100);
    int mes   = leerEnteroEnRango(sc, "Mes (1-12): ", 1, 12);
    while (true) {
      int dia = leerEnteroEnRango(sc, "Dia (1-31): ", 1, 31);
      try { return java.time.LocalDate.of(anio, mes, dia); }
      catch (Exception e) { System.out.println("Fecha invalida, intenta de nuevo."); }
    }
  }

  private static int leerEnteroEnRango(Scanner sc, String prompt, int min, int max) {
    while (true) {
      System.out.print(prompt);
      String s = sc.nextLine().trim();
      try {
        int v = Integer.parseInt(s);
        if (v < min || v > max) { System.out.println("Fuera de rango."); continue; }
        return v;
      } catch (NumberFormatException e) {
        System.out.println("Numero invalido.");
      }
    }
  }
}
