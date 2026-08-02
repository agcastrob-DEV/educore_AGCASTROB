package edu.uam.educore.socket;

import edu.uam.educore.db.Conexion;
import edu.uam.educore.db.ConfiguracionBD;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servidor de Reportes. Ante la orden REPORTE cuenta las entidades del sistema en la base de datos,
 * escribe un TXT con el resumen en el directorio de salida y devuelve su contenido por el socket.
 */
public class ServidorReportes {

  private final ConfiguracionBD config;
  private final Path salidaDir;

  public ServidorReportes(ConfiguracionBD config, String salidaDir) {
    this.config = config;
    this.salidaDir = Path.of(salidaDir);
  }

  public static void main(String[] args) throws Exception {
    ConfiguracionBD config = ConfiguracionBD.desdeArchivo(".env");
    String salida = System.getenv("SALIDA_DIR");
    int puerto = Integer.parseInt(System.getenv("REPORTE_PORT"));
    new ServidorReportes(config, salida).escuchar(puerto);
  }

  public void escuchar(int puerto) throws IOException {
    try (ServerSocket servidor = new ServerSocket(puerto)) {
      System.out.println("Reportes escuchando en " + puerto);
      while (true) {
        try (Socket cliente = servidor.accept();
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out =
                new PrintWriter(cliente.getOutputStream(), true, StandardCharsets.UTF_8)) {
          atender(in, out);
        } catch (Exception e) {
          System.err.println("Error atendiendo cliente: " + e.getMessage());
        }
      }
    }
  }

  private void atender(BufferedReader in, PrintWriter out) throws IOException {
    String linea = in.readLine();
    if (linea == null || !linea.trim().equals("REPORTE")) {
      out.println("400 comando invalido");
      return;
    }
    try {
      String contenido = generarYGuardar();
      String[] lineas = contenido.split("\n");
      out.println("200 " + lineas.length);
      for (String l : lineas) {
        out.println(l);
      }
    } catch (Exception e) {
      out.println("500 " + e.getMessage());
    }
  }

  /**
   * TODO(estudiante · T4): generar el reporte.
   *
   * <p>Contar en la base de datos (estudiante, empleado, seccion, aula, matricula), armar un texto,
   * ESCRIBIRLO como TXT en salidaDir (Files.createDirectories + Files.writeString con timestamp) y
   * devolver su contenido. Referencia del patrón: ServidorMatricula para la parte de socket;
   * consultas COUNT(*).
   */
  private String generarYGuardar() throws Exception {
    int cantEstudiantes = 0;
    int cantEmpleados = 0;
    int cantEdificios = 0;
    int cantAulas = 0;
    int cantSecciones = 0;
    int cantMatriculas = 0;

    try (Connection con = Conexion.getConnection(config.url(), config.usuario(), config.contrasena())) {
      String queryEst = "SELECT COUNT(*) FROM estudiante";
      String queryEmp = "SELECT COUNT(*) FROM empleado";
      String queryEd = "SELECT COUNT(*) FROM edificio";
      String queryAu = "SELECT COUNT(*) FROM aula";
      String querySec = "SELECT COUNT(*) FROM seccion";
      String queryMat = "SELECT COUNT(*) FROM matricula";

      try (PreparedStatement ps = con.prepareStatement(queryEst);
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) cantEstudiantes = rs.getInt(1);
      }
      try (PreparedStatement ps = con.prepareStatement(queryEmp);
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) cantEmpleados = rs.getInt(1);
      }
      try (PreparedStatement ps = con.prepareStatement(queryEd);
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) cantEdificios = rs.getInt(1);
      }
      try (PreparedStatement ps = con.prepareStatement(queryAu);
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) cantAulas = rs.getInt(1);
      }
      try (PreparedStatement ps = con.prepareStatement(querySec);
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) cantSecciones = rs.getInt(1);
      }
      try (PreparedStatement ps = con.prepareStatement(queryMat);
           ResultSet rs = ps.executeQuery()) {
        if (rs.next()) cantMatriculas = rs.getInt(1);
      }
    }

    LocalDateTime ahora = LocalDateTime.now();
    DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    String timestamp = ahora.format(timestampFormatter);
    String nombreArchivo = "reporte_" + timestamp + ".txt";

    DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    StringBuilder sb = new StringBuilder();
    sb.append("=========================================\n");
    sb.append("EDUCORE - REPORTE GENERAL DEL SISTEMA\n");
    sb.append("Generado: ").append(ahora.format(displayFormatter)).append("\n");
    sb.append("=========================================\n");
    sb.append("Estudiantes registrados: ").append(cantEstudiantes).append("\n");
    sb.append("Empleados registrados: ").append(cantEmpleados).append("\n");
    sb.append("Edificios registrados: ").append(cantEdificios).append("\n");
    sb.append("Aulas registradas: ").append(cantAulas).append("\n");
    sb.append("Secciones registradas: ").append(cantSecciones).append("\n");
    sb.append("Matrículas registradas: ").append(cantMatriculas).append("\n");
    sb.append("=========================================");

    String reporte = sb.toString();

    Files.createDirectories(salidaDir);
    Path archivoDestino = salidaDir.resolve(nombreArchivo);
    Files.writeString(archivoDestino, reporte, StandardCharsets.UTF_8);

    return reporte;
  }
}
