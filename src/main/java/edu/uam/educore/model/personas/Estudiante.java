package edu.uam.educore.model.personas;

public abstract class Estudiante extends Persona {

  /**
   * Tarifa base de matrícula por período (colones). Constante de dominio
   * compartida por todos los
   * tipos de estudiante: vive aquí (no duplicada en cada subclase) para que un
   * solo cambio
   * actualice a toda la jerarquía.
   */
  protected static final double TARIFA_BASE = 150000.0;

  private String carnet;

  public Estudiante(int id, String nombre, String apellidos, String email, String carnet) {
    super(id, nombre, apellidos, email);
    this.carnet = carnet;
  }

  public String getCarnet() {
    return carnet;
  }

  public void setCarnet(String carnet) {
    this.carnet = carnet;
  }

  public abstract double calcularMatricula();

  public abstract boolean puedeMatricular();

  @Override
  public String getInfo() {
    return String.format(
        "[%s] %s %s | Carnet: %s | Matrícula: ₡%.2f",
        getTipo(), getNombre(), getApellidos(), carnet, calcularMatricula());
  }
}
