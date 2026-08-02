package edu.uam.educore.exception;

public class MatriculaDuplicadaException extends MatriculaException {
  public MatriculaDuplicadaException(String carnet, String codigoSeccion) {
    super("El estudiante " + carnet + " ya se encuentra matriculado en la sección " + codigoSeccion + ".");
  }
}
