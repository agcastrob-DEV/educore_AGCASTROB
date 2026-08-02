package edu.uam.educore.api;

import edu.uam.educore.api.Dtos.AulaDto;
import edu.uam.educore.api.Dtos.AulaRequest;
import edu.uam.educore.api.Dtos.EdificioDto;
import edu.uam.educore.api.Dtos.EdificioRequest;
import edu.uam.educore.api.Dtos.EmpleadoDto;
import edu.uam.educore.api.Dtos.EmpleadoRequest;
import edu.uam.educore.api.Dtos.EstudianteDto;
import edu.uam.educore.api.Dtos.EstudianteRequest;
import edu.uam.educore.api.Dtos.InscripcionRequest;
import edu.uam.educore.api.Dtos.MatriculaRequest;
import edu.uam.educore.api.Dtos.SeccionDto;
import edu.uam.educore.api.Dtos.SeccionRequest;
import edu.uam.educore.controller.EdificioController;
import edu.uam.educore.controller.EmpleadoController;
import edu.uam.educore.controller.EstudianteController;
import edu.uam.educore.controller.SeccionController;
import edu.uam.educore.dao.EdificioRepoSql;
import edu.uam.educore.dao.EmpleadoRepoSql;
import edu.uam.educore.dao.EstudianteRepoSql;
import edu.uam.educore.dao.ListaEdificioRepo;
import edu.uam.educore.dao.ListaEmpleadoRepo;
import edu.uam.educore.dao.ListaEstudianteRepo;
import edu.uam.educore.dao.ListaSeccionRepo;
import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.dao.SeccionRepoSql;
import edu.uam.educore.db.ConfiguracionBD;
import edu.uam.educore.enums.TipoAula;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.academico.Seccion;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.model.personas.Estudiante;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

/**
 * Arma EduCore como app web. Estudiante corre sobre base de datos (referencia) con su controlador
 * real. Empleado, Edificio/Aula y Sección son de cada grupo (P1) — estas rutas no llaman a ningún
 * controlador de nombre fijo, ver los TODO(estudiante · P1) en cada una.
 */
public class ServidorApi {

  public static void iniciar(int puerto) throws IOException {
    Repositorio<Estudiante> estudianteRepo;
    Repositorio<Empleado> empleadoRepo;
    Repositorio<Edificio> edificioRepo;
    Repositorio<Seccion> seccionRepo;
    try {
      ConfiguracionBD config = ConfiguracionBD.desdeArchivo(".env");
      estudianteRepo = new EstudianteRepoSql(config);
      empleadoRepo = new EmpleadoRepoSql(config);
      edificioRepo = new EdificioRepoSql(config);
      seccionRepo = new SeccionRepoSql(config);
    } catch (IOException e) {
      // Sin .env legible caemos a memoria para no bloquear el arranque en desarrollo.
      estudianteRepo = new ListaEstudianteRepo();
      empleadoRepo = new ListaEmpleadoRepo();
      edificioRepo = new ListaEdificioRepo();
      seccionRepo = new ListaSeccionRepo();
    }

    EstudianteController estudianteController = new EstudianteController(estudianteRepo);
    EmpleadoController empleadoController = new EmpleadoController(empleadoRepo);
    EdificioController edificioController = new EdificioController(edificioRepo);
    SeccionController seccionController = new SeccionController(seccionRepo);

    // Sincronizar contadores de ID tras reinicio
    sincronizarContadoresEmpleado(empleadoController, empleadoRepo);
    sincronizarContadoresEdificio(edificioController, edificioRepo);
    sincronizarContadoresSeccion(seccionController, seccionRepo);

    final Repositorio<Edificio> finalEdificioRepo = edificioRepo;
    final Repositorio<Seccion> finalSeccionRepo = seccionRepo;

    Javalin app =
        Javalin.create(
            cfg -> {
              cfg.bundledPlugins.enableDevLogging();
              cfg.spaRoot.addFile("/", "/web/index.html");

              cfg.routes.exception(
                  IllegalArgumentException.class,
                  (e, ctx) -> ctx.status(400).json(Map.of("error", e.getMessage())));
              cfg.routes.exception(
                  SQLIntegrityConstraintViolationException.class,
                  (e, ctx) ->
                      ctx.status(409)
                          .json(
                              Map.of(
                                  "error",
                                  "No se puede eliminar: el registro tiene datos asociados.")));
              cfg.routes.exception(
                  Exception.class,
                  (e, ctx) -> ctx.status(500).json(Map.of("error", e.getMessage())));

              registrarEstudiantes(cfg, estudianteController);
              registrarEmpleados(cfg, empleadoController);
              registrarEdificios(cfg, edificioController, finalEdificioRepo);
              registrarSecciones(cfg, seccionController, finalSeccionRepo, estudianteController, empleadoController, edificioController);
              registrarMatricula(cfg);
              registrarReporte(cfg);
            });

    app.start(puerto);
    System.out.println("API EduCore escuchando en http://localhost:" + puerto);
  }

  // ── Estudiantes ──

  private static void registrarEstudiantes(JavalinConfig cfg, EstudianteController controller) {
    cfg.routes.get(
        "/api/estudiantes",
        ctx -> {
          List<EstudianteDto> lista =
              controller.listar().stream().map(EstudianteDto::desde).toList();
          ctx.json(lista);
        });

    cfg.routes.post(
        "/api/estudiantes",
        ctx -> {
          EstudianteRequest r = ctx.bodyAsClass(EstudianteRequest.class);
          Estudiante creado =
              "BECADO".equalsIgnoreCase(r.tipo())
                  ? controller.registrarBecado(
                      r.nombre(),
                      r.apellidos(),
                      r.email(),
                      r.carnet(),
                      r.porcentajeBeca() != null ? r.porcentajeBeca() : 0.0)
                  : controller.registrarRegular(r.nombre(), r.apellidos(), r.email(), r.carnet());
          ctx.status(201).json(EstudianteDto.desde(creado));
        });

    cfg.routes.put(
        "/api/estudiantes/{id}",
        ctx -> {
          int id = Integer.parseInt(ctx.pathParam("id"));
          EstudianteRequest r = ctx.bodyAsClass(EstudianteRequest.class);
          Estudiante e =
              controller.actualizar(
                  id, r.nombre(), r.apellidos(), r.email(), r.carnet(), r.porcentajeBeca());
          ctx.json(EstudianteDto.desde(e));
        });

    cfg.routes.delete(
        "/api/estudiantes/{id}",
        ctx -> {
          controller.eliminar(Integer.parseInt(ctx.pathParam("id")));
          ctx.status(204);
        });
  }

  // ── Empleados (P1 de cada grupo — sin controlador de nombre fijo) ──

  private static void registrarEmpleados(JavalinConfig cfg, EmpleadoController controller) {
    cfg.routes.get(
        "/api/empleados",
        ctx -> {
          List<Empleado> empleados = controller.listar();
          ctx.json(EmpleadoDto.listaDesde(empleados));
        });

    cfg.routes.post(
        "/api/empleados",
        ctx -> {
          EmpleadoRequest r = ctx.bodyAsClass(EmpleadoRequest.class);
          Empleado creado =
              controller.registrar(
                  r.nombre(),
                  r.apellidos(),
                  r.email(),
                  r.salario(),
                  java.time.LocalDate.parse(r.fechaIngreso()),
                  r.tipo());
          ctx.status(201).json(EmpleadoDto.desde(creado));
        });

    cfg.routes.put(
        "/api/empleados/{id}",
        ctx -> {
          int id = Integer.parseInt(ctx.pathParam("id"));
          EmpleadoRequest r = ctx.bodyAsClass(EmpleadoRequest.class);
          Empleado actualizado =
              controller.actualizar(
                  id,
                  r.nombre(),
                  r.apellidos(),
                  r.email(),
                  r.salario(),
                  java.time.LocalDate.parse(r.fechaIngreso()),
                  r.tipo());
          ctx.json(EmpleadoDto.desde(actualizado));
        });

    cfg.routes.delete(
        "/api/empleados/{id}",
        ctx -> {
          controller.eliminar(Integer.parseInt(ctx.pathParam("id")));
          ctx.status(204);
        });
  }

  // ── Edificios / Aulas (P1 de cada grupo — sin controlador de nombre fijo) ──

  private static void registrarEdificios(JavalinConfig cfg, EdificioController controller, Repositorio<Edificio> repo) {
    cfg.routes.get(
        "/api/edificios",
        ctx -> {
          List<Edificio> edificios = controller.listarEdificios();
          ctx.json(EdificioDto.listaDesde(edificios));
        });

    cfg.routes.post(
        "/api/edificios",
        ctx -> {
          EdificioRequest r = ctx.bodyAsClass(EdificioRequest.class);
          Edificio creado = controller.registrarEdificio(r.codigo(), r.nombre());
          ctx.status(201).json(EdificioDto.desde(creado));
        });

    cfg.routes.put(
        "/api/edificios/{id}",
        ctx -> {
          int id = Integer.parseInt(ctx.pathParam("id"));
          EdificioRequest r = ctx.bodyAsClass(EdificioRequest.class);
          Edificio actualizado = controller.actualizarEdificio(id, r.codigo(), r.nombre());
          ctx.json(EdificioDto.desde(actualizado));
        });

    cfg.routes.delete(
        "/api/edificios/{id}",
        ctx -> {
          controller.eliminarEdificio(Integer.parseInt(ctx.pathParam("id")));
          ctx.status(204);
        });

    cfg.routes.post(
        "/api/edificios/{id}/aulas",
        ctx -> {
          int edificioId = Integer.parseInt(ctx.pathParam("id"));
          AulaRequest r = ctx.bodyAsClass(AulaRequest.class);
          Aula aula = controller.registrarAula(
              edificioId,
              r.numero(),
              r.capacidad(),
              r.tipo() != null ? r.tipo() : TipoAula.REGULAR);
          ctx.status(201).json(AulaDto.desde(aula));
        });

    cfg.routes.put(
        "/api/edificios/{id}/aulas/{aulaId}",
        ctx -> {
          int edificioId = Integer.parseInt(ctx.pathParam("id"));
          int aulaId = Integer.parseInt(ctx.pathParam("aulaId"));
          AulaRequest r = ctx.bodyAsClass(AulaRequest.class);
          Edificio ed = controller.buscarEdificioPorId(edificioId);
          if (ed == null) {
            throw new IllegalArgumentException("No existe el edificio con ID " + edificioId + ".");
          }
          boolean existe = ed.getAulas().stream().anyMatch(a -> a.getId() == aulaId);
          if (!existe) {
            throw new IllegalArgumentException("El aula con ID " + aulaId + " no pertenece al edificio indicado.");
          }
          ed.removerAula(aulaId);
          Aula updatedAula = new Aula(aulaId, r.numero(), r.capacidad(), r.tipo() != null ? r.tipo() : TipoAula.REGULAR, ed);
          ed.agregarAula(updatedAula);
          repo.actualizar(ed);
          ctx.json(AulaDto.desde(updatedAula));
        });

    cfg.routes.delete(
        "/api/edificios/{id}/aulas/{aulaId}",
        ctx -> {
          int edificioId = Integer.parseInt(ctx.pathParam("id"));
          int aulaId = Integer.parseInt(ctx.pathParam("aulaId"));
          controller.eliminarAula(edificioId, aulaId);
          ctx.status(204);
        });
  }

  // ── Secciones (P1 de cada grupo — sin controlador de nombre fijo) ──

  private static void registrarSecciones(
      JavalinConfig cfg,
      SeccionController controller,
      Repositorio<Seccion> seccionRepo,
      EstudianteController estController,
      EmpleadoController empController,
      EdificioController edController) {
    cfg.routes.get(
        "/api/secciones",
        ctx -> {
          List<Seccion> secciones = controller.listarSecciones();
          ctx.json(SeccionDto.listaDesde(secciones));
        });

    cfg.routes.post(
        "/api/secciones",
        ctx -> {
          SeccionRequest r = ctx.bodyAsClass(SeccionRequest.class);
          Empleado profesor = empController.buscarPorId(r.docenteId());
          if (profesor == null) {
            throw new IllegalArgumentException("No existe el docente con ID " + r.docenteId() + ".");
          }
          Aula aula = null;
          for (Edificio ed : edController.listarEdificios()) {
            for (Aula a : ed.getAulas()) {
              if (a.getId() == r.aulaId()) {
                aula = a;
                break;
              }
            }
            if (aula != null) break;
          }
          if (aula == null) {
            throw new IllegalArgumentException("No existe el aula con ID " + r.aulaId() + ".");
          }
          Seccion creada = controller.abrirSeccion(r.codigo(), r.nombre(), profesor, aula);
          ctx.status(201).json(SeccionDto.desde(creada));
        });

    cfg.routes.put(
        "/api/secciones/{id}",
        ctx -> {
          int id = Integer.parseInt(ctx.pathParam("id"));
          SeccionRequest r = ctx.bodyAsClass(SeccionRequest.class);
          Seccion seccion = controller.buscarSeccionPorId(id);
          if (seccion == null) {
            throw new IllegalArgumentException("No existe la sección con ID " + id + ".");
          }
          if (r.codigo() == null || r.codigo().isEmpty()) {
            throw new IllegalArgumentException("El código de la sección es obligatorio.");
          }
          if (r.nombre() == null || r.nombre().isEmpty()) {
            throw new IllegalArgumentException("El nombre del curso es obligatorio.");
          }
          Empleado profesor = empController.buscarPorId(r.docenteId());
          if (profesor == null) {
            throw new IllegalArgumentException("No existe el docente con ID " + r.docenteId() + ".");
          }
          if (profesor.getTipoEnum() != TipoEmpleado.DOCENTE) {
            throw new IllegalArgumentException("El empleado asignado debe tener el rol de Docente.");
          }
          Aula aula = null;
          for (Edificio ed : edController.listarEdificios()) {
            for (Aula a : ed.getAulas()) {
              if (a.getId() == r.aulaId()) {
                aula = a;
                break;
              }
            }
            if (aula != null) break;
          }
          if (aula == null) {
            throw new IllegalArgumentException("No existe el aula con ID " + r.aulaId() + ".");
          }
          seccion.setCodigoSeccion(r.codigo());
          seccion.setCurso(r.nombre());
          seccion.setProfesor(profesor);
          seccion.setAula(aula);
          seccionRepo.actualizar(seccion);
          ctx.json(SeccionDto.desde(seccion));
        });

    cfg.routes.delete(
        "/api/secciones/{id}",
        ctx -> {
          controller.eliminarSeccion(Integer.parseInt(ctx.pathParam("id")));
          ctx.status(204);
        });

    cfg.routes.post(
        "/api/secciones/{id}/estudiantes",
        ctx -> {
          int seccionId = Integer.parseInt(ctx.pathParam("id"));
          InscripcionRequest r = ctx.bodyAsClass(InscripcionRequest.class);
          Estudiante estudiante = estController.buscarPorId(r.estudianteId());
          if (estudiante == null) {
            throw new IllegalArgumentException("No existe el estudiante con ID " + r.estudianteId() + ".");
          }
          controller.matricularEstudiante(seccionId, estudiante);
          ctx.json(SeccionDto.desde(controller.buscarSeccionPorId(seccionId)));
        });

    cfg.routes.delete(
        "/api/secciones/{id}/estudiantes/{estudianteId}",
        ctx -> {
          int seccionId = Integer.parseInt(ctx.pathParam("id"));
          int estudianteId = Integer.parseInt(ctx.pathParam("estudianteId"));
          controller.desmatricularEstudiante(seccionId, estudianteId);
          ctx.status(204);
        });
  }

  // ── Matrícula (puente HTTP→socket) ──

  private static void registrarMatricula(JavalinConfig cfg) {
    cfg.routes.post(
        "/api/matricula",
        ctx -> {
          // Bridge HTTP→socket (provisto por el docente como ejemplo de sockets + archivos):
          // guarda el CSV que sube la SPA en ENTRADA_DIR y delega el lote al ServidorMatricula.
          // La lógica de la transacción vive en ServidorMatricula.procesarLote (la implementan
          // los estudiantes).
          MatriculaRequest r = ctx.bodyAsClass(MatriculaRequest.class);
          String archivo = r.archivo() != null ? r.archivo() : "matriculas.csv";
          String contenido = r.contenido() != null ? r.contenido() : "";
          Path entrada = Path.of(System.getenv("ENTRADA_DIR"));
          Files.createDirectories(entrada);
          Files.writeString(entrada.resolve(archivo), contenido);
          String host = System.getenv("MATRICULA_HOST");
          int puertoMatricula = Integer.parseInt(System.getenv("MATRICULA_PORT"));
          try (Socket socket = new Socket(host, puertoMatricula);
              PrintWriter out =
                  new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
              BufferedReader in =
                  new BufferedReader(
                      new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out.println("MATRICULAR " + archivo);
            String respuesta = in.readLine();
            ctx.json(
                Map.of(
                    "respuesta",
                    respuesta != null ? respuesta : "sin respuesta del servicio de matricula"));
          }
        });
  }

  // ── Reporte (puente HTTP→socket) ──

  private static void registrarReporte(JavalinConfig cfg) {
    cfg.routes.post(
        "/api/reporte",
        ctx -> {
          // Bridge HTTP→socket (provisto por el docente como ejemplo de sockets + archivos):
          // pide el reporte al ServidorReportes y devuelve el TXT que la SPA descarga. La lógica
          // de conteo y escritura del archivo vive en ServidorReportes.generarYGuardar (la
          // implementan los estudiantes).
          String host = System.getenv("REPORTE_HOST");
          int puertoReporte = Integer.parseInt(System.getenv("REPORTE_PORT"));
          try (Socket socket = new Socket(host, puertoReporte);
              PrintWriter out =
                  new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
              BufferedReader in =
                  new BufferedReader(
                      new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out.println("REPORTE");
            String encabezado = in.readLine(); // "200 <n>" en éxito, "500 <msg>" en error
            if (encabezado == null || !encabezado.startsWith("200 ")) {
              ctx.status(502).json(Map.of("error", "reporte no disponible: " + encabezado));
              return;
            }
            int lineas = Integer.parseInt(encabezado.substring("200 ".length()).trim());
            StringBuilder contenido = new StringBuilder();
            for (int i = 0; i < lineas; i++) {
              String linea = in.readLine();
              contenido.append(linea == null ? "" : linea).append("\n");
            }
            ctx.contentType("text/plain; charset=utf-8");
            ctx.header("Content-Disposition", "attachment; filename=\"reporte.txt\"");
            ctx.result(contenido.toString());
          }
        });
  }

  private static void sincronizarContadoresEmpleado(EmpleadoController controller, Repositorio<Empleado> repo) {
    try {
      int maxId = 0;
      for (Empleado e : repo.buscarTodos()) {
        if (e.getId() > maxId) {
          maxId = e.getId();
        }
      }
      java.lang.reflect.Field field = EmpleadoController.class.getDeclaredField("proximoId");
      field.setAccessible(true);
      field.set(controller, maxId + 1);
    } catch (Exception ex) {
      // Ignorar
    }
  }

  private static void sincronizarContadoresEdificio(EdificioController controller, Repositorio<Edificio> repo) {
    try {
      int maxEdificioId = 0;
      int maxAulaId = 0;
      for (Edificio ed : repo.buscarTodos()) {
        if (ed.getId() > maxEdificioId) {
          maxEdificioId = ed.getId();
        }
        for (Aula au : ed.getAulas()) {
          if (au.getId() > maxAulaId) {
            maxAulaId = au.getId();
          }
        }
      }
      java.lang.reflect.Field fEd = EdificioController.class.getDeclaredField("proximoEdificioId");
      fEd.setAccessible(true);
      fEd.set(controller, maxEdificioId + 1);

      java.lang.reflect.Field fAu = EdificioController.class.getDeclaredField("proximoAulaId");
      fAu.setAccessible(true);
      fAu.set(controller, maxAulaId + 1);
    } catch (Exception ex) {
      // Ignorar
    }
  }

  private static void sincronizarContadoresSeccion(SeccionController controller, Repositorio<Seccion> repo) {
    try {
      int maxSeccionId = 0;
      for (Seccion s : repo.buscarTodos()) {
        if (s.getId() > maxSeccionId) {
          maxSeccionId = s.getId();
        }
      }
      java.lang.reflect.Field field = SeccionController.class.getDeclaredField("proximoSeccionId");
      field.setAccessible(true);
      field.set(controller, maxSeccionId + 1);
    } catch (Exception ex) {
      // Ignorar
    }
  }
}
