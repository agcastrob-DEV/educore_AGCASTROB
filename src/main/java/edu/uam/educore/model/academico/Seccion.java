package edu.uam.educore.model.academico;

import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.model.personas.Estudiante;
import edu.uam.educore.model.infraestructura.Aula;
import java.util.ArrayList;
import java.util.List;

public class Seccion {

  private int id;
  private String codigoSeccion; // ej. "A-01"
  private String curso; // ej. "Estructuras de Datos"
  private Empleado profesor; // Debe ser de tipo DOCENTE
  private Aula aula;
  private List<Estudiante> estudiantesMatriculados;

  public Seccion(int id, String codigoSeccion, String curso, Empleado profesor, Aula aula) {
    this.id = id;
    this.codigoSeccion = codigoSeccion;
    this.curso = curso;
    this.profesor = profesor;
    this.aula = aula;
    this.estudiantesMatriculados = new ArrayList<>();
  }

  public int getId() {
    return id;
  }

  public String getCodigoSeccion() {
    return codigoSeccion;
  }

  public void setCodigoSeccion(String codigoSeccion) {
    this.codigoSeccion = codigoSeccion;
  }

  public String getCurso() {
    return curso;
  }

  public void setCurso(String curso) {
    this.curso = curso;
  }

  public Empleado getProfesor() {
    return profesor;
  }

  public void setProfesor(Empleado profesor) {
    this.profesor = profesor;
  }

  public Aula getAula() {
    return aula;
  }

  public void setAula(Aula aula) {
    this.aula = aula;
  }

  public List<Estudiante> getEstudiantesMatriculados() {
    return new ArrayList<>(estudiantesMatriculados);
  }

  public void matricularEstudiante(Estudiante e) {
    this.estudiantesMatriculados.add(e);
  }

  public void desmatricularEstudiante(int estudianteId) {
    this.estudiantesMatriculados.removeIf(e -> e.getId() == estudianteId);
  }

  public String getInfo() {
    return String.format(
        "Sección ID: %d | Código: %s | Curso: %s | Prof: %s | Aula: %s (%s) | Matriculados: %d",
        id, codigoSeccion, curso,
        (profesor != null ? profesor.getNombre() + " " + profesor.getApellidos() : "Sin asignar"),
        (aula != null ? aula.getNumero() : "Sin asignar"),
        (aula != null && aula.getEdificio() != null ? aula.getEdificio().getCodigo() : "N/A"),
        estudiantesMatriculados.size());
  }
}