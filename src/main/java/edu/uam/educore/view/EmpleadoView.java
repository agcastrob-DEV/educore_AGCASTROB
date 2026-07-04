package edu.uam.educore.view;

import edu.uam.educore.controller.EmpleadoController;
import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.personas.Empleado;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class EmpleadoView extends VistaBase {

  private final EmpleadoController controller;

  public EmpleadoView(Scanner scanner, Repositorio<Empleado> repo) {
    super(scanner);
    this.controller = new EmpleadoController(repo);
  }

  // ── Ciclo de la vista ─────────────────────────────────────────────────────

  public void iniciar() {
    boolean activo = true;
    while (activo) {
      int opcion = mostrarMenu();
      if (opcion == 1) {
        registrar();
      } else if (opcion == 2) {
        listar();
      } else if (opcion == 3) {
        buscar();
      } else if (opcion == 4) {
        actualizar();
      } else if (opcion == 5) {
        eliminar();
      } else if (opcion == 0) {
        activo = false;
      } else {
        mostrarError("Opción inválida.");
      }
    }
  }

  // ── Acciones ──────────────────────────────────────────────────────────────

  private void registrar() {
    System.out.println("\n--- REGISTRAR EMPLEADO ---");
    String nombre = leerTexto("Nombre");
    String apellidos = leerTexto("Apellidos");
    String email = leerTexto("Email");
    double salario = leerDecimal("Salario Mensual (₡)");
    LocalDate fechaIngreso = leerFecha("Fecha de ingreso (AAAA-MM-DD)");
    TipoEmpleado tipo = seleccionarTipoEmpleado();

    try {
      Empleado nuevo = controller.registrar(nombre, apellidos, email, salario, fechaIngreso, tipo);
      mostrarMensaje("Empleado registrado con éxito. ID asignado: " + nuevo.getId());
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error inesperado al registrar: " + e.getMessage());
    }
  }

  private void listar() {
    System.out.println("\n--- LISTA DE EMPLEADOS ---");
    try {
      List<Empleado> empleados = controller.listar();
      if (empleados.isEmpty()) {
        System.out.println("  No hay empleados registrados en el sistema.");
        return;
      }
      for (Empleado e : empleados) {
        System.out.println("  " + e.getInfo());
      }
    } catch (Exception e) {
      mostrarError("Error al listar empleados: " + e.getMessage());
    }
  }

  private void buscar() {
    System.out.println("\n--- BUSCAR EMPLEADO ---");
    int id = leerEntero("ID del empleado a buscar");
    try {
      Empleado e = controller.buscarPorId(id);
      if (e == null) {
        mostrarError("No existe empleado con ID " + id + ".");
        return;
      }
      System.out.println("\n  Resultado: " + e.getInfo());
    } catch (Exception ex) {
      mostrarError("Error al buscar: " + ex.getMessage());
    }
  }

  private void actualizar() {
    System.out.println("\n--- ACTUALIZAR EMPLEADO ---");
    int id = leerEntero("ID del empleado a modificar");
    try {
      Empleado existente = controller.buscarPorId(id);
      if (existente == null) {
        mostrarError("No existe empleado con ID " + id + ".");
        return;
      }
      System.out.println("\n  Datos actuales: " + existente.getInfo());
      
      String nombre = leerTexto("Nuevo Nombre");
      String apellidos = leerTexto("Nuevos Apellidos");
      String email = leerTexto("Nuevo Email");
      double salario = leerDecimal("Nuevo Salario Mensual (₡)");
      LocalDate fechaIngreso = leerFecha("Nueva Fecha de ingreso (AAAA-MM-DD)");
      TipoEmpleado tipo = seleccionarTipoEmpleado();

      controller.actualizar(id, nombre, apellidos, email, salario, fechaIngreso, tipo);
      mostrarMensaje("Empleado con ID " + id + " actualizado correctamente.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al actualizar: " + e.getMessage());
    }
  }

  private void eliminar() {
    System.out.println("\n--- ELIMINAR EMPLEADO ---");
    int id = leerEntero("ID del empleado a eliminar");
    try {
      Empleado existente = controller.buscarPorId(id);
      if (existente == null) {
        mostrarError("No existe empleado con ID " + id + ".");
        return;
      }
      System.out.println("\n  " + existente.getInfo());
      String confirmacion = leerTexto("¿Confirma la eliminación? (s/n)");
      if (!confirmacion.equalsIgnoreCase("s")) {
        mostrarMensaje("Operación cancelada.");
        return;
      }
      controller.eliminar(id);
      mostrarMensaje("Empleado con ID " + id + " eliminado.");
    } catch (IllegalArgumentException e) {
      mostrarError(e.getMessage());
    } catch (Exception e) {
      mostrarError("Error al eliminar: " + e.getMessage());
    }
  }

  // ── Menús e Inputs Especiales ─────────────────────────────────────────────

  private int mostrarMenu() {
    System.out.println("\n--- GESTIÓN DE EMPLEADOS ---");
    System.out.println("1. Registrar empleado");
    System.out.println("2. Listar todos");
    System.out.println("3. Buscar por ID");
    System.out.println("4. Actualizar empleado");
    System.out.println("5. Eliminar empleado");
    System.out.println("0. Volver al menú principal");
    System.out.print("Opción: ");
    return leerEntero();
  }

  private TipoEmpleado seleccionarTipoEmpleado() {
    while (true) {
      System.out.println("Seleccione el Tipo de Empleado:");
      System.out.println("1. Docente");
      System.out.println("2. Administrativo");
      System.out.println("3. Guarda");
      System.out.println("4. Misceláneo");
      System.out.println("5. Mantenimiento");
      int opc = leerEntero("Opción");
      
      switch (opc) {
        case 1: return TipoEmpleado.DOCENTE;
        case 2: return TipoEmpleado.ADMINISTRATIVO;
        case 3: return TipoEmpleado.GUARDA;
        case 4: return TipoEmpleado.MISCELANEO;
        case 5: return TipoEmpleado.MANTENIMIENTO;
        default: mostrarError("Selección inválida. Intente de nuevo.");
      }
    }
  }
}