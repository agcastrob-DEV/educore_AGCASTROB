package edu.uam.educore.socket;

import edu.uam.educore.db.Conexion;
import edu.uam.educore.db.ConfiguracionBD;
import edu.uam.educore.exception.*;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Servidor de Matrícula. Recibe por socket la orden MATRICULAR &lt;archivo&gt;, lee ese CSV del
 * directorio de entrada (una matrícula por renglón: carnet,codigoSeccion) y matricula todo el lote
 * en UNA transacción: si un renglón falla, revierte el lote completo.
 */
public class ServidorMatricula {

  private final ConfiguracionBD config;
  private final Path entradaDir;

  public ServidorMatricula(ConfiguracionBD config, String entradaDir) {
    this.config = config;
    this.entradaDir = Path.of(entradaDir);
  }

  public static void main(String[] args) throws Exception {
    ConfiguracionBD config = ConfiguracionBD.desdeArchivo(".env");
    String entrada = System.getenv("ENTRADA_DIR");
    int puerto = Integer.parseInt(System.getenv("MATRICULA_PORT"));
    new ServidorMatricula(config, entrada).escuchar(puerto);
  }

  public void escuchar(int puerto) throws IOException {
    try (ServerSocket servidor = new ServerSocket(puerto)) {
      System.out.println("Matricula escuchando en " + puerto);
      while (true) {
        try (Socket cliente = servidor.accept();
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out =
                new PrintWriter(cliente.getOutputStream(), true, StandardCharsets.UTF_8)) {
          atender(in, out);
        } catch (IOException e) {
          System.err.println("Error atendiendo cliente: " + e.getMessage());
        }
      }
    }
  }

  private void atender(BufferedReader in, PrintWriter out) throws IOException {
    String linea = in.readLine();
    if (linea == null || !linea.startsWith("MATRICULAR ")) {
      out.println("400 comando invalido");
      return;
    }
    String archivo = linea.substring("MATRICULAR ".length()).trim();
    try {
      int k = procesarLote(archivo);
      out.println("201 " + k);
    } catch (Exception e) {
      out.println("400 " + e.getMessage());
    }
  }

  /**
   * TODO(estudiante · T4/T5): matricular el lote en UNA transacción.
   *
   * <p>Pasos:
   *
   * <ol>
   *   <li>Leer el CSV de entrada (entradaDir.resolve(archivo)) con un BufferedReader; cada renglón
   *       es "carnet,codigoSeccion".
   *   <li>Abrir conexión y con.setAutoCommit(false).
   *   <li>Por cada renglón: buscar el estudiante por carnet (si no existe, es un error), buscar la
   *       sección por código y su cupo (aula.capacidad), validar cupo y duplicado, e insertar en
   *       matricula.
   *   <li>Si todo pasa: con.commit() y devolver la cantidad. Si algo falla: con.rollback() y
   *       relanzar.
   * </ol>
   *
   * <p>Cómo distinguir los cuatro casos de error (carnet inexistente, sección inexistente, cupo
   * lleno, matrícula duplicada) queda a su criterio de diseño — no hay una jerarquía de
   * excepciones provista. Ver "Puntos extra" en el enunciado si quieren diseñar la suya.
   *
   * <p>Referencia del patrón JDBC: EstudianteRepoSql.
   */
  private int procesarLote(String archivo) throws Exception {
    Path rutaCsv = entradaDir.resolve(archivo);
    if (!Files.exists(rutaCsv)) {
      throw new IOException("El archivo " + archivo + " no existe en el directorio de entrada.");
    }

    List<String[]> lineas = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(rutaCsv, StandardCharsets.UTF_8)) {
      String l;
      while ((l = reader.readLine()) != null) {
        l = l.trim();
        if (l.isEmpty()) {
          continue;
        }
        if (l.startsWith("carnet,codigoSeccion") || l.startsWith("carnet,codigo")) {
          continue; // Cabecera
        }
        String[] parts = l.split(",");
        if (parts.length >= 2) {
          lineas.add(new String[] {parts[0].trim(), parts[1].trim()});
        }
      }
    }

    int count = 0;
    try (Connection con = Conexion.getConnection(config.url(), config.usuario(), config.contrasena())) {
      con.setAutoCommit(false);
      try {
        String selectEst = "SELECT id FROM estudiante WHERE carnet = ?";
        String selectSec = "SELECT s.id AS seccion_id, a.capacidad AS capacidad "
            + "FROM seccion s "
            + "JOIN aula a ON s.aula_id = a.id "
            + "WHERE s.codigo = ?";
        String selectCount = "SELECT COUNT(*) AS total FROM matricula WHERE seccion_id = ?";
        String selectCheckMat = "SELECT COUNT(*) AS total FROM matricula WHERE estudiante_id = ? AND seccion_id = ?";
        String insertMat = "INSERT INTO matricula (estudiante_id, seccion_id) VALUES (?, ?)";

        try (PreparedStatement psEst = con.prepareStatement(selectEst);
             PreparedStatement psSec = con.prepareStatement(selectSec);
             PreparedStatement psCount = con.prepareStatement(selectCount);
             PreparedStatement psCheckMat = con.prepareStatement(selectCheckMat);
             PreparedStatement psInsert = con.prepareStatement(insertMat)) {

          for (String[] row : lineas) {
            String carnet = row[0];
            String codigoSeccion = row[1];

            // 1. Buscar estudiante
            psEst.setString(1, carnet);
            int estudianteId = -1;
            try (ResultSet rs = psEst.executeQuery()) {
              if (rs.next()) {
                estudianteId = rs.getInt("id");
              }
            }
            if (estudianteId == -1) {
              throw new EstudianteNoEncontradoException(carnet);
            }

            // 2. Buscar sección y su capacidad de aula
            psSec.setString(1, codigoSeccion);
            int seccionId = -1;
            int capacidad = 0;
            try (ResultSet rs = psSec.executeQuery()) {
              if (rs.next()) {
                seccionId = rs.getInt("seccion_id");
                capacidad = rs.getInt("capacidad");
              }
            }
            if (seccionId == -1) {
              throw new SeccionNoEncontradaException(codigoSeccion);
            }

            // 3. Verificar cupo
            psCount.setInt(1, seccionId);
            int inscritos = 0;
            try (ResultSet rs = psCount.executeQuery()) {
              if (rs.next()) {
                inscritos = rs.getInt("total");
              }
            }
            if (inscritos >= capacidad) {
              throw new CupoLlenoException(codigoSeccion);
            }

            // 4. Verificar duplicado
            psCheckMat.setInt(1, estudianteId);
            psCheckMat.setInt(2, seccionId);
            boolean duplicada = false;
            try (ResultSet rs = psCheckMat.executeQuery()) {
              if (rs.next()) {
                duplicada = (rs.getInt("total") > 0);
              }
            }
            if (duplicada) {
              throw new MatriculaDuplicadaException(carnet, codigoSeccion);
            }

            // 5. Insertar matrícula
            psInsert.setInt(1, estudianteId);
            psInsert.setInt(2, seccionId);
            psInsert.executeUpdate();
            count++;
          }
        }

        con.commit();
        return count;
      } catch (Exception ex) {
        con.rollback();
        throw ex;
      } finally {
        con.setAutoCommit(true);
      }
    }
  }
}
