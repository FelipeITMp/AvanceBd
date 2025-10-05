package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

/** Menú de consola (avance) usando CÉDULA. */
public class BdpryFinal {

  public static void main(String[] args) {
    var auth    = new AuthService();
    var citas   = new CitaService();
    var udao    = new UsuarioDaoJdbc();
    var sc      = new Scanner(System.in);

    UsuarioDaoJdbc.User usuarioActual = null;
    Integer sesionId = null;
    Integer pacienteId = null;
    Integer doctorId   = null;

    while (true) {
      System.out.println("\n================ MENU ================");
      if (usuarioActual == null) {
        System.out.println("1) Crear cuenta");
        System.out.println("2) Iniciar sesión");
        System.out.println("0) Salir");
        System.out.print("Opción: ");
        String op = sc.nextLine().trim();
        switch (op) {
          case "1" -> crearCuenta(sc, udao);
          case "2" -> {
            try {
              System.out.print("Usuario: ");   String u = sc.nextLine().trim();
              System.out.print("Password: ");  String p = sc.nextLine();
              var res = auth.login(u, p);
              usuarioActual = res.user();
              sesionId = res.sesionId();
              pacienteId = res.pacienteId();
              doctorId   = res.doctorId();
              System.out.println("Bienvenido " + usuarioActual.username + " (" + usuarioActual.rol + ")");
            } catch (Exception e) {
              System.out.println("Error: " + e.getMessage());
            }
          }
          case "0" -> { auth.logout(sesionId); System.out.println("Chao!"); return; }
          default -> System.out.println("Opción inválida");
        }
        continue;
      }

      System.out.println("Usuario: " + usuarioActual.username + "  Rol: " + usuarioActual.rol);
      if ("Paciente".equals(usuarioActual.rol)) {
        System.out.println("1) Ver mis citas (rango)");
        System.out.println("2) Crear cita (por cédula de doctor)");
        System.out.println("3) Cancelar cita");
        System.out.println("4) Cerrar sesión");
        System.out.print("Opción: ");
        String op = sc.nextLine().trim();
        switch (op) {
          case "1" -> {
            LocalDate desde = leerFechaOpt(sc, "Desde (yyyy-MM-dd, vacío = sin límite): ");
            LocalDate hasta = leerFechaOpt(sc, "Hasta (yyyy-MM-dd, vacío = sin límite): ");
            try {
              List<CitaDaoJdbc.CitaPacItem> lista = citas.citasPaciente(pacienteId, desde, hasta);
              if (lista.isEmpty()) System.out.println("(sin citas)");
              else lista.forEach(c -> System.out.printf("#%d  %s %s  %s  %s  %s%n",
                  c.id, c.fecha, c.hora, c.doctor, c.estado, c.observacion == null ? "" : c.observacion));
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
          }
          case "2" -> {
            try {
              System.out.print("Cédula doctor: "); String cedDoc = sc.nextLine().trim();
              LocalDate fecha = leerFecha(sc, "Fecha (yyyy-MM-dd): ");
              LocalTime hora  = leerHora(sc,  "Hora (HH:mm): ");
              System.out.print("Observación (opcional): "); String obs = sc.nextLine();
              int id = citas.crearPorPaciente(pacienteId, cedDoc, fecha, hora, obs);
              System.out.println("Cita creada id=" + id + " (Pendiente)");
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
          }
          case "3" -> {
            try {
              int id = leerEntero(sc, "Id de la cita: ");
              citas.cancelarPorPaciente(id, pacienteId);
              System.out.println("Cita cancelada.");
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
          }
          case "4" -> { auth.logout(sesionId); usuarioActual = null; sesionId = null; pacienteId = doctorId = null; }
          default -> System.out.println("Opción inválida");
        }
      } else { // Doctor
        System.out.println("1) Ver mi agenda por fecha");
        System.out.println("2) Crear cita (por cédulas)");
        System.out.println("3) Cambiar estado de una cita");
        System.out.println("4) Cerrar sesión");
        System.out.print("Opción: ");
        String op = sc.nextLine().trim();
        switch (op) {
          case "1" -> {
            try {
              System.out.print("Tu cédula (doctor): "); String cedDoc = sc.nextLine().trim();
              LocalDate fecha = leerFecha(sc, "Fecha (yyyy-MM-dd): ");
              var agenda = citas.agendaDoctor(cedDoc, fecha);
              if (agenda.isEmpty()) System.out.println("(sin turnos)");
              else agenda.forEach(a ->
                  System.out.printf("#%d  %s  %-20s  %-10s  %s%n", a.id, a.hora, a.paciente, a.estado, a.observacion == null ? "" : a.observacion));
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
          }
          case "2" -> {
            try {
              System.out.print("Cédula paciente: "); String cedPac = sc.nextLine().trim();
              System.out.print("Tu cédula (doctor): "); String cedDoc = sc.nextLine().trim();
              LocalDate fecha = leerFecha(sc, "Fecha (yyyy-MM-dd): ");
              LocalTime hora  = leerHora(sc,  "Hora (HH:mm): ");
              System.out.print("Estado (Pendiente|Confirmada|Cancelada|Atendida): "); String est = sc.nextLine().trim();
              System.out.print("Observación (opcional): "); String obs = sc.nextLine();
              int id = citas.crearPorCodigos(cedPac, cedDoc, fecha, hora, est, obs);
              System.out.println("Cita creada id=" + id);
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
          }
          case "3" -> {
            try {
              int id = leerEntero(sc, "Id de la cita: ");
              System.out.print("Nuevo estado (Pendiente|Confirmada|Cancelada|Atendida): ");
              String est = sc.nextLine().trim();
              citas.actualizarEstado(id, est);
              System.out.println("Estado actualizado.");
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
          }
          case "4" -> { auth.logout(sesionId); usuarioActual = null; sesionId = null; pacienteId = doctorId = null; }
          default -> System.out.println("Opción inválida");
        }
      }
    }
  }

  /* ===================== helpers ===================== */

  private static void crearCuenta(Scanner sc, UsuarioDaoJdbc udao) {
    System.out.print("Usuario: "); String u = sc.nextLine().trim();
    System.out.print("Password: "); String p = sc.nextLine();
    System.out.print("Rol (Doctor|Paciente): "); String r = sc.nextLine().trim();
    if (!"Doctor".equals(r) && !"Paciente".equals(r)) { System.out.println("Rol inválido."); return; }

    try {
      int id = udao.crearUsuario(u, /*nombre*/ u, p, r);
      System.out.println("Usuario creado id=" + id + " (se generará CED-" + id + " por trigger)");
      if ("Doctor".equals(r)) {
        var ddao = new DoctorDaoJdbc();
        System.out.print("Especialidad (opcional): "); String esp = sc.nextLine();
        System.out.print("Sede (opcional): ");         String sede= sc.nextLine();
        System.out.print("Horario (opcional): ");      String hor = sc.nextLine();
        System.out.print("Género (opcional): ");       String gen = sc.nextLine();
        ddao.completarPerfilPorUsuarioId(id, esp, sede, hor, gen);
        System.out.println("Perfil de Doctor actualizado.");
      } else {
        var pdao = new PacienteDaoJdbc();
        System.out.print("Dirección (opcional): ");    String dir = sc.nextLine();
        LocalDate fn = leerFechaOpt(sc, "Fecha nacimiento (yyyy-MM-dd, vacío = omitir): ");
        System.out.print("Género (opcional): ");       String gen = sc.nextLine();
        pdao.completarPerfilPorUsuarioId(id, dir, fn, gen);
        System.out.println("Perfil de Paciente actualizado.");
      }
    } catch (Exception e) {
      System.out.println("Error creando cuenta: " + e.getMessage());
    }
  }

  private static int leerEntero(Scanner sc, String prompt) {
    while (true) {
      System.out.print(prompt);
      String s = sc.nextLine().trim();
      try { return Integer.parseInt(s); }
      catch (NumberFormatException e) { System.out.println("Número inválido."); }
    }
  }

  private static LocalDate leerFecha(Scanner sc, String prompt) {
    while (true) {
      System.out.print(prompt);
      String s = sc.nextLine().trim();
      try { return LocalDate.parse(s); }
      catch (Exception e) { System.out.println("Fecha inválida."); }
    }
  }

  private static LocalTime leerHora(Scanner sc, String prompt) {
    while (true) {
      System.out.print(prompt);
      String s = sc.nextLine().trim();
      try { return LocalTime.parse(s); }
      catch (Exception e) { System.out.println("Hora inválida."); }
    }
  }

  private static LocalDate leerFechaOpt(Scanner sc, String prompt) {
    System.out.print(prompt);
    String s = sc.nextLine().trim();
    if (s.isEmpty()) return null;
    try { return LocalDate.parse(s); }
    catch (Exception e) { System.out.println("Fecha inválida, ignorando."); return null; }
  }
}
