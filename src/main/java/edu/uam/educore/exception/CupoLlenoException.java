package edu.uam.educore.exception;

public class CupoLlenoException extends MatriculaException {
  public CupoLlenoException(String codigoSeccion) {
    super("La sección " + codigoSeccion + " ya superó la capacidad máxima.");
  }
}
