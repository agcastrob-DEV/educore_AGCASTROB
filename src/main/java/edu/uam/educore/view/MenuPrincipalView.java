package edu.uam.educore.view;

import edu.uam.educore.dao.ListaEstudianteRepo;
import edu.uam.educore.dao.ListaEmpleadoRepo;
import edu.uam.educore.dao.ListaEdificioRepo;
import edu.uam.educore.dao.ListaSeccionRepo;
import java.util.Scanner;

public class MenuPrincipalView extends VistaBase {

  private final EstudianteView estudianteView;
  private final EmpleadoView empleadoView;
  private final EdificioView edificioView;
  private final SeccionView seccionView;

  public MenuPrincipalView(Scanner scanner) {
    super(scanner);

    // 1. Instanciar los repositorios compartidos en memoria
    ListaEstudianteRepo estudianteRepo = new ListaEstudianteRepo();
    ListaEmpleadoRepo empleadoRepo = new ListaEmpleadoRepo();
    ListaEdificioRepo edificioRepo = new ListaEdificioRepo();
    ListaSeccionRepo seccionRepo = new ListaSeccionRepo();

    // 2. Inicializar las vistas correspondientes con sus dependencias
    this.estudianteView = new EstudianteView(scanner, estudianteRepo);
    this.empleadoView = new EmpleadoView(scanner, empleadoRepo);
    this.edificioView = new EdificioView(scanner, edificioRepo);
    
    // La vista académica cruza los datos usando el controlador de edificios y los repositorios hermanos
    this.seccionView = new SeccionView(
        scanner, 
        seccionRepo, 
        this.edificioView.getController(), 
        empleadoRepo, 
        estudianteRepo
    );
  }

  public void iniciar() {
    mostrarBienvenida();
    boolean corriendo = true;
    while (corriendo) {
      switch (mostrarMenuPrincipal()) {
        case 1 -> estudianteView.iniciar();
        case 2 -> empleadoView.iniciar();
        case 3 -> mostrarSubMenuAcademico();
        case 0 -> {
          mostrarMensaje("¡Hasta pronto!");
          corriendo = false;
        }
        default -> mostrarError("Opción inválida. Ingrese un número del 0 al 3.");
      }
    }
  }

  public void mostrarBienvenida() {
    System.out.println("╔══════════════════════════════════════╗");
    System.out.println("║        EduCore v1.0                  ║");
    System.out.println("║  Sistema de Administración Educativa ║");
    System.out.println("╚══════════════════════════════════════╝");
  }

  public int mostrarMenuPrincipal() {
    System.out.println("\n--- MENÚ PRINCIPAL ---");
    System.out.println("1. Gestión de Estudiantes");
    System.out.println("2. Gestión de Empleados");
    System.out.println("3. Gestión Académica e Infraestructura");
    System.out.println("0. Salir");
    System.out.print("Seleccione una opción: ");
    return leerEntero();
  }

  // Helper interno para agrupar Infraestructura y Secciones bajo la Opción 3 del enunciado
  private void mostrarSubMenuAcademico() {
    boolean enSubMenu = true;
    while (enSubMenu) {
      System.out.println("\n--- GESTIÓN ACADÉMICA E INFRAESTRUCTURA ---");
      System.out.println("1. Infraestructura Física (Edificios y Aulas)");
      System.out.println("2. Control de Grupos (Secciones y Matrícula)");
      System.out.println("0. Volver al Menú Principal");
      System.out.print("Seleccione una opción: ");
      
      int opcion = leerEntero();
      if (opcion == 1) {
        edificioView.iniciar();
      } else if (opcion == 2) {
        seccionView.iniciar();
      } else if (opcion == 0) {
        enSubMenu = false;
      } else {
        mostrarError("Opción inválida.");
      }
    }
  }
}