package edu.uam.educore.model.personas;

import static org.junit.jupiter.api.Assertions.*;

import edu.uam.educore.enums.TipoEmpleado;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmpleadoTest {

  @Test
  void constructor_asigna_valores_correctamente() {
    LocalDate fecha = LocalDate.of(2026, 1, 1);
    Empleado e = new Empleado(1, "Andres", "Castro", "andres@uam.edu", 500000.0, fecha, TipoEmpleado.DOCENTE);

    assertEquals(1, e.getId());
    assertEquals("Andres", e.getNombre());
    assertEquals("Castro", e.getApellidos());
    assertEquals("andres@uam.edu", e.getEmail());
    assertEquals(500000.0, e.getSalario(), 0.01);
    assertEquals(fecha, e.getFechaIngreso());
    assertEquals(TipoEmpleado.DOCENTE, e.getTipoEnum());
  }

  @Test
  void getTipo_retorna_texto_correcto_segun_enum() {
    LocalDate fecha = LocalDate.now();
    Empleado e1 = new Empleado(1, "Juan", "Perez", "j@uam.edu", 400000.0, fecha, TipoEmpleado.DOCENTE);
    Empleado e2 = new Empleado(2, "Maria", "Solano", "m@uam.edu", 350000.0, fecha, TipoEmpleado.GUARDA);

    assertEquals("Docente", e1.getTipo());
    assertEquals("Guarda de Seguridad", e2.getTipo());
  }

  @Test
  void getInfo_contiene_formato_y_datos_clave() {
    LocalDate fecha = LocalDate.of(2025, 5, 10);
    Empleado e = new Empleado(3, "Julia", "Flores", "julia@uam.edu", 620000.50, fecha, TipoEmpleado.ADMINISTRATIVO);

    String info = e.getInfo();

    assertTrue(info.contains("[Administrativo]"));
    assertTrue(info.contains("Julia Flores"));
    assertTrue(info.contains("julia@uam.edu"));
    assertTrue(info.contains("₡620000,50") || info.contains("₡620000.50")); // Tolerancia al locale del sistema
    assertTrue(info.contains("2025-05-10"));
  }
}