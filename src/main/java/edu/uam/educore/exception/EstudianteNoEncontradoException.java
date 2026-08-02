package edu.uam.educore.exception;

public class EstudianteNoEncontradoException extends MatriculaException {
  public EstudianteNoEncontradoException(String carnet) {
    super("Estudiante con carnet " + carnet + " no existe.");
  }
}
