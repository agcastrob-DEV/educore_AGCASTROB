package edu.uam.educore.util;

import java.time.LocalDate;

public class Validador {
  private Validador() {
  }

  public static boolean validarEmail(String email) {
    return email != null && email.contains("@") && email.contains(".");
  }

  public static boolean validarFechaIngreso(LocalDate fecha) {
    return fecha != null && !fecha.isAfter(LocalDate.now());
  }

  public static boolean validarPorcentajeBeca(double porcentaje) {
    return porcentaje >= 0.0 && porcentaje <= 1.0;
  }

  public static boolean validarCalificacion(double calificacion) {
    return calificacion >= 0.0 && calificacion <= 100.0;
  }
}
