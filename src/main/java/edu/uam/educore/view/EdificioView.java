package edu.uam.educore.view;

import edu.uam.educore.controller.EdificioController;
import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.enums.TipoAula;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import java.util.List;
import java.util.Scanner;

public class EdificioView extends VistaBase {

  private final EdificioController controller;

  public EdificioView(Scanner scanner, Repositorio<Edificio> repo) {
    super(scanner);
    this.controller = new EdificioController(repo);
  }

  // Método puente expuesto para que el controlador unificado de la academia lo
  // invoque
  public EdificioController getController() {
    return this.controller;
  }

  // ── Ciclo de la vista ─────────────────────────────────────────────────────

  public void iniciar() {
    boolean activo = true;
    while (activo) {
      int opcion = mostrarMenu();
      if (opcion == 1) {
        registrarEdificio();
      } else if (opcion == 2) {
        listarEdificios();
      } else if (opcion == 3) {
        actualizarEdificio();
      } else if (opcion == 4) {
        eliminarEdificio();
      } else if (opcion == 5) {
        registrarAulaEnEdificio();
      } else if (opcion == 6) {
        eliminarAulaDeEdificio();
      } else if (opcion == 0) {
        activo = false;
      } else {
        mostrarError("Opción inválida.");
      }
    }
  }

  // ── Acciones de Edificio ──────────────────────────────────────────────────

  private void registrarEdificio() {
    System.out.println("\n--- REGISTRAR EDIFICIO ---");
    String codigo = leerTexto("Código del Edificio (ej. AR-01)");
    String nombre = leerTexto("Nombre del Edificio");

    try {
      Edificio nuevo = controller.registrarEdificio(codigo, nombre);
      mostrarMensaje("Edificio registrado con éxito. ID asignado: " + nuevo.getId());
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error inesperado: " + e.getMessage());
    }
  }

  private void listarEdificios() {
    System.out.println("\n--- LISTA DE INFRAESTRUCTURA (EDIFICIOS Y AULAS) ---");
    try {
      List<Edificio> edificios = controller.listarEdificios();
      if (edificios.isEmpty()) {
        System.out.println("  No hay edificios registrados en el sistema.");
        return;
      }
      for (Edificio ed : edificios) {
        System.out.println("\n🏠 " + ed.getInfo());
        List<Aula> aulas = ed.getAulas();
        if (aulas.isEmpty()) {
          System.out.println("     ↳ [Sin aulas asociadas aún]");
        } else {
          for (Aula au : aulas) {
            System.out.println("     ↳ 🚪 " + au.getInfo());
          }
        }
      }
    } catch (Exception e) {
      mostrarError("Error al listar infraestructura: " + e.getMessage());
    }
  }

  private void actualizarEdificio() {
    System.out.println("\n--- ACTUALIZAR EDIFICIO ---");
    int id = leerEntero("ID del edificio a modificar");
    try {
      Edificio existente = controller.buscarEdificioPorId(id);
      if (existente == null) {
        mostrarError("No existe edificio con ID " + id + ".");
        return;
      }
      System.out.println("  Datos actuales: " + existente.getInfo());
      String codigo = leerTexto("Nuevo Código");
      String nombre = leerTexto("Nuevo Nombre");

      controller.actualizarEdificio(id, codigo, nombre);
      mostrarMensaje("Edificio actualizado correctamente.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al actualizar: " + e.getMessage());
    }
  }

  private void eliminarEdificio() {
    System.out.println("\n--- ELIMINAR EDIFICIO ---");
    int id = leerEntero("ID del edificio a eliminar");
    try {
      Edificio existente = controller.buscarEdificioPorId(id);
      if (existente == null) {
        mostrarError("No existe edificio con ID " + id + ".");
        return;
      }
      System.out.println("  " + existente.getInfo());
      String confirmacion = leerTexto("¿Seguro que desea eliminar este edificio? (s/n)");
      if (!confirmacion.equalsIgnoreCase("s")) {
        mostrarMensaje("Operación cancelada.");
        return;
      }
      controller.eliminarEdificio(id);
      mostrarMensaje("Edificio eliminado con éxito.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al eliminar: " + e.getMessage());
    }
  }

  // ── Acciones de Aula (Composición) ────────────────────────────────────────

  private void registrarAulaEnEdificio() {
    System.out.println("\n--- ASOCIAR NUEVA AULA ---");
    int edificioId = leerEntero("ID del Edificio donde se ubica el aula");
    try {
      Edificio ed = controller.buscarEdificioPorId(edificioId);
      if (ed == null) {
        mostrarError("No existe edificio con ID " + edificioId + ".");
        return;
      }
      String numero = leerTexto("Número/Código de Aula (ej. Aula 402)");
      int capacidad = leerEntero("Capacidad máxima de alumnos");
      TipoAula tipo = seleccionarTipoAula();

      Aula nueva = controller.registrarAula(edificioId, numero, capacidad, tipo);
      mostrarMensaje("Aula asociada con éxito. ID Global de Aula asignado: " + nueva.getId());
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al registrar aula: " + e.getMessage());
    }
  }

  private void eliminarAulaDeEdificio() {
    System.out.println("\n--- ELIMINAR AULA ---");
    int edificioId = leerEntero("ID del Edificio");
    int aulaId = leerEntero("ID Técnico del Aula a borrar");
    try {
      String confirmacion = leerTexto("¿Confirma la remoción del aula? (s/n)");
      if (!confirmacion.equalsIgnoreCase("s")) {
        mostrarMensaje("Operación cancelada.");
        return;
      }
      controller.eliminarAula(edificioId, aulaId);
      mostrarMensaje("Aula removida correctamente del edificio.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al remover aula: " + e.getMessage());
    }
  }

  // ── Menús e Inputs Especiales ─────────────────────────────────────────────

  private int mostrarMenu() {
    System.out.println("\n--- GESTIÓN DE INFRAESTRUCTURA ---");
    System.out.println("1. Registrar Edificio");
    System.out.println("2. Listar Edificios y Aulas");
    System.out.println("3. Actualizar Edificio");
    System.out.println("4. Eliminar Edificio");
    System.out.println("5. Registrar/Asociar Aula a Edificio");
    System.out.println("6. Eliminar Aula de Edificio");
    System.out.println("0. Volver");
    System.out.print("Opción: ");
    return leerEntero();
  }

  private TipoAula seleccionarTipoAula() {
    while (true) {
      System.out.println("Seleccione el Tipo de Aula:");
      System.out.println("1. Regular");
      System.out.println("2. Laboratorio");
      System.out.println("3. Auditorio");
      int opc = leerEntero("Opción");
      switch (opc) {
        case 1:
          return TipoAula.REGULAR;
        case 2:
          return TipoAula.LABORATORIO;
        case 3:
          return TipoAula.AUDITORIO;
        default:
          mostrarError("Selección inválida. Intente de nuevo.");
      }
    }
  }
}