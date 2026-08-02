package edu.uam.educore.exception;

public class SeccionNoEncontradaException extends MatriculaException {
  public SeccionNoEncontradaException(String codigoSeccion) {
    super("Sección con código " + codigoSeccion + " no existe.");
  }
}
