package edu.uam.educore.view;

import edu.uam.educore.controller.EdificioController;
import edu.uam.educore.controller.SeccionController;
import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.model.academico.Seccion;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.model.personas.Estudiante;
import java.util.List;
import java.util.Scanner;

public class SeccionView extends VistaBase {

  private final SeccionController seccionController;
  private final EdificioController edificioController;
  private final Repositorio<Empleado> empleadoRepo;
  private final Repositorio<Estudiante> estudianteRepo;

  public SeccionView(
      Scanner scanner,
      Repositorio<Seccion> seccionRepo,
      EdificioController edificioController,
      Repositorio<Empleado> empleadoRepo,
      Repositorio<Estudiante> estudianteRepo) {
    super(scanner);
    this.seccionController = new SeccionController(seccionRepo);
    this.edificioController = edificioController;
    this.empleadoRepo = empleadoRepo;
    this.estudianteRepo = estudianteRepo;
  }

  // ── Ciclo de la vista ─────────────────────────────────────────────────────

  public void iniciar() {
    boolean activo = true;
    while (activo) {
      int opcion = mostrarMenu();
      if (opcion == 1) {
        abrirSeccion();
      } else if (opcion == 2) {
        listarSecciones();
      } else if (opcion == 3) {
        matricularEstudiante();
      } else if (opcion == 4) {
        desmatricularEstudiante();
      } else if (opcion == 5) {
        eliminarSeccion();
      } else if (opcion == 0) {
        activo = false;
      } else {
        mostrarError("Opción inválida.");
      }
    }
  }

  // ── Acciones de Negocio Académico ─────────────────────────────────────────

  private void abrirSeccion() {
    System.out.println("\n--- ABRIR NUEVA SECCIÓN ACADÉMICA ---");
    String codigo = leerTexto("Código de Sección (ej. SC-401)");
    String curso = leerTexto("Nombre del Curso");

    int profId = leerEntero("ID del Empleado (Docente)");
    int edId = leerEntero("ID del Edificio para el Aula");
    int aulaId = leerEntero("ID Técnico del Aula dentro del edificio");

    try {
      // Cruzar datos: Buscar entidades externas
      Empleado docente = empleadoRepo.buscarPorId(profId).orElse(null);
      if (docente == null) {
        mostrarError("No se encontró ningún empleado con el ID " + profId);
        return;
      }

      Edificio ed = edificioController.buscarEdificioPorId(edId);
      if (ed == null) {
        mostrarError("No se encontró el edificio con el ID " + edId);
        return;
      }

      Aula aulaSeleccionada = ed.getAulas().stream()
          .filter(a -> a.getId() == aulaId)
          .findFirst()
          .orElse(null);

      if (aulaSeleccionada == null) {
        mostrarError("El aula con ID " + aulaId + " no existe en el edificio " + ed.getNombre());
        return;
      }

      Seccion s = seccionController.abrirSeccion(codigo, curso, docente, aulaSeleccionada);

      mostrarMensaje("Sección abierta con éxito. ID Asignado: " + s.getId());
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error inesperado al abrir sección: " + e.getMessage());
    }
  }

  private void listarSecciones() {
    System.out.println("\n--- LISTA DE SECCIONES ACTIVAS ---");
    try {
      List<Seccion> secciones = seccionController.listarSecciones();
      if (secciones.isEmpty()) {
        System.out.println("  No hay secciones abiertas actualmente.");
        return;
      }
      for (Seccion s : secciones) {
        System.out.println("\n📖 " + s.getInfo());
        List<Estudiante> alumnos = s.getEstudiantesMatriculados();
        if (alumnos.isEmpty()) {
          System.out.println("     ↳ [Sin alumnos matriculados]");
        } else {
          System.out.println("     ↳ Alumnos Matriculados:");
          for (Estudiante est : alumnos) {
            System.out.println("       • ID: " + est.getId() + " - " + est.getNombre() + " " + est.getApellidos());
          }
        }
      }
    } catch (Exception e) {
      mostrarError("Error al listar: " + e.getMessage());
    }
  }

  private void matricularEstudiante() {
    System.out.println("\n--- MATRICULAR ESTUDIANTE EN SECCIÓN ---");
    int seccionId = leerEntero("ID de la Sección");
    int estudianteId = leerEntero("ID del Estudiante a matricular");

    try {
      Estudiante est = estudianteRepo.buscarPorId(estudianteId).orElse(null);
      if (est == null) {
        mostrarError("No existe ningún estudiante registrado con el ID " + estudianteId);
        return;
      }

      seccionController.matricularEstudiante(seccionId, est);
      mostrarMensaje("Estudiante matriculado con éxito en la sección " + seccionId);
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error en matrícula: " + e.getMessage());
    }
  }

  private void desmatricularEstudiante() {
    System.out.println("\n--- RETIRAR ESTUDIANTE DE SECCIÓN ---");
    int seccionId = leerEntero("ID de la Sección");
    int estudianteId = leerEntero("ID del Estudiante a retirar");

    try {
      seccionController.desmatricularEstudiante(seccionId, estudianteId);
      mostrarMensaje("Estudiante retirado correctamente de la sección.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al desmatricular: " + e.getMessage());
    }
  }

  private void eliminarSeccion() {
    System.out.println("\n--- CERRAR/ELIMINAR SECCIÓN ---");
    int id = leerEntero("ID de la sección a eliminar");
    try {
      String conf = leerTexto("¿Seguro que desea cerrar esta sección? (s/n)");
      if (!conf.equalsIgnoreCase("s")) {
        mostrarMensaje("Operación cancelada.");
        return;
      }
      seccionController.eliminarSeccion(id);
      mostrarMensaje("Sección eliminada con éxito.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al eliminar: " + e.getMessage());
    }
  }

  // ── Menús ─────────────────────────────────────────────────────────────────

  private int mostrarMenu() {
    System.out.println("\n--- CONTROL ACADÉMICO (SECCIONES Y MATRÍCULA) ---");
    System.out.println("1. Abrir Sección (Asignar Curso, Docente y Aula)");
    System.out.println("2. Listar Secciones y Alumnos");
    System.out.println("3. Matricular Estudiante");
    System.out.println("4. Retirar Estudiante");
    System.out.println("5. Eliminar/Cerrar Sección");
    System.out.println("0. Volver");
    System.out.print("Opción: ");
    return leerEntero();
  }
}