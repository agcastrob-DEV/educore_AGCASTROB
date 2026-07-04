package edu.uam.educore.model.personas;

import edu.uam.educore.enums.TipoEmpleado;
import java.time.LocalDate;

public class Empleado extends Persona {

  private double salario;
  private LocalDate fechaIngreso;
  private TipoEmpleado tipo;

  public Empleado(int id, String nombre, String apellidos, String email, double salario, LocalDate fechaIngreso, TipoEmpleado tipo) {
    super(id, nombre, apellidos, email);
    this.salario = salario;
    this.fechaIngreso = fechaIngreso;
    this.tipo = tipo;
  }

  public double getSalario() {
    return salario;
  }

  public void setSalario(double salario) {
    this.salario = salario;
  }

  public LocalDate getFechaIngreso() {
    return fechaIngreso;
  }

  public void setFechaIngreso(LocalDate fechaIngreso) {
    this.fechaIngreso = fechaIngreso;
  }

  @Override
  public String getTipo() {
    // Retorna una cadena limpia y legible para la interfaz de usuario, adaptada según el enum
    switch (this.tipo) {
      case DOCENTE: return "Docente";
      case ADMINISTRATIVO: return "Administrativo";
      case GUARDA: return "Guarda de Seguridad";
      case MISCELANEO: return "Misceláneo";
      case MANTENIMIENTO: return "Mantenimiento";
      default: return "Empleado";
    }
  }

  public TipoEmpleado getTipoEnum() {
    return tipo;
  }

  public void setTipo(TipoEmpleado tipo) {
    this.tipo = tipo;
  }

  @Override
  public String getInfo() {
    // Sigue exactamente el formato visual de Estudiante.java utilizando el formato de colones (₡)
    return String.format(
        "[%s] %s %s | Email: %s | Salario: ₡%.2f | Ingreso: %s",
        getTipo(), getNombre(), getApellidos(), getEmail(), salario, fechaIngreso);
  }
}