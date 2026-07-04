package edu.uam.educore.controller;

import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.academico.Seccion;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.model.personas.Estudiante;
import java.util.List;
import java.util.Optional;

public class SeccionController {

  private final Repositorio<Seccion> seccionRepo;
  private int proximoSeccionId = 1;

  public SeccionController(Repositorio<Seccion> seccionRepo) {
    this.seccionRepo = seccionRepo;
  }

  // ── MÉTODOS CRUD BÁSICOS ──────────────────────────────────────────────────

  public Seccion abrirSeccion(String codigoSeccion, String curso, Empleado profesor, Aula aula) throws Exception {
    validarSeccionBase(codigoSeccion, curso);
    validarProfesorDocente(profesor);

    Seccion nueva = new Seccion(proximoSeccionId, codigoSeccion, curso, profesor, aula);
    seccionRepo.guardar(nueva);
    proximoSeccionId++;
    return nueva;
  }

  public List<Seccion> listarSecciones() throws Exception {
    return seccionRepo.buscarTodos();
  }

  public Seccion buscarSeccionPorId(int id) throws Exception {
    Optional<Seccion> resultado = seccionRepo.buscarPorId(id);
    return resultado.orElse(null);
  }

  public void eliminarSeccion(int id) throws Exception {
    Seccion seccion = buscarSeccionPorId(id);
    if (seccion == null) {
      throw new IllegalArgumentException("No existe la sección con ID " + id + ".");
    }
    // Regla opcional de seguridad: Evitar borrar secciones con alumnos matriculados
    if (!seccion.getEstudiantesMatriculados().isEmpty()) {
      throw new IllegalArgumentException("No se puede eliminar la sección porque ya tiene estudiantes matriculados.");
    }
    seccionRepo.eliminar(id);
  }

  // ── LOGICA DE MATRÍCULA ───────────────────────────────────────────────────

  public void matricularEstudiante(int seccionId, Estudiante estudiante) throws Exception {
    Seccion seccion = buscarSeccionPorId(seccionId);
    if (seccion == null) {
      throw new IllegalArgumentException("No existe la sección con ID " + seccionId + ".");
    }
    if (estudiante == null) {
      throw new IllegalArgumentException("El estudiante a matricular no es válido.");
    }

    // 1. Validar que no esté ya matriculado
    boolean yaMatriculado = seccion.getEstudiantesMatriculados().stream()
        .anyMatch(e -> e.getId() == estudiante.getId());
    if (yaMatriculado) {
      throw new IllegalArgumentException("El estudiante ya se encuentra matriculado en esta sección.");
    }

    // 2. Validar límite de capacidad física del Aula asignada
    if (seccion.getAula() != null) {
      int capacidadMaxima = seccion.getAula().getCapacidad();
      int alumnosActuales = seccion.getEstudiantesMatriculados().size();
      if (alumnosActuales >= capacidadMaxima) {
        throw new IllegalArgumentException("No hay espacio en el aula. Capacidad máxima alcanzada: " + capacidadMaxima);
      }
    }

    seccion.matricularEstudiante(estudiante);
    seccionRepo.actualizar(seccion);
  }

  public void desmatricularEstudiante(int seccionId, int estudianteId) throws Exception {
    Seccion seccion = buscarSeccionPorId(seccionId);
    if (seccion == null) {
      throw new IllegalArgumentException("No existe la sección con ID " + seccionId + ".");
    }

    boolean estaMatriculado = seccion.getEstudiantesMatriculados().stream()
        .anyMatch(e -> e.getId() == estudianteId);
    if (!estaMatriculado) {
      throw new IllegalArgumentException("El estudiante no pertenece a esta sección.");
    }

    seccion.desmatricularEstudiante(estudianteId);
    seccionRepo.actualizar(seccion);
  }

  // ── VALIDACIONES INTERNAS ─────────────────────────────────────────────────

  private void validarSeccionBase(String codigo, String curso) {
    if (codigo == null || codigo.isEmpty()) {
      throw new IllegalArgumentException("El código de la sección es obligatorio.");
    }
    if (curso == null || curso.isEmpty()) {
      throw new IllegalArgumentException("El nombre del curso es obligatorio.");
    }
  }

  private void validarProfesorDocente(Empleado profesor) {
    if (profesor != null && profesor.getTipoEnum() != TipoEmpleado.DOCENTE) {
      throw new IllegalArgumentException("El empleado asignado debe tener el rol de Docente.");
    }
  }
}