package edu.uam.educore.controller;

import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.util.Validador;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EmpleadoController {

  private final Repositorio<Empleado> repo;
  private int proximoId = 1;

  public EmpleadoController(Repositorio<Empleado> repo) {
    this.repo = repo;
  }

  public Empleado registrar(String nombre, String apellidos, String email, double salario, LocalDate fechaIngreso, TipoEmpleado tipo) throws Exception {
    validarBase(nombre, apellidos, email, salario, fechaIngreso);
    
    Empleado e = new Empleado(proximoId, nombre, apellidos, email, salario, fechaIngreso, tipo);
    repo.guardar(e);
    proximoId++;
    return e;
  }

  public List<Empleado> listar() throws Exception {
    return repo.buscarTodos();
  }

  public Empleado buscarPorId(int id) throws Exception {
    Optional<Empleado> resultado = repo.buscarPorId(id);
    if (resultado.isPresent()) {
      return resultado.get();
    }
    return null;
  }

  public Empleado actualizar(int id, String nombre, String apellidos, String email, double salario, LocalDate fechaIngreso, TipoEmpleado tipo) throws Exception {
    Empleado e = buscarPorId(id);
    if (e == null) {
      throw new IllegalArgumentException("No existe empleado con ID " + id + ".");
    }
    
    validarBase(nombre, apellidos, email, salario, fechaIngreso);
    
    e.setNombre(nombre);
    e.setApellidos(apellidos);
    e.setEmail(email);
    e.setSalario(salario);
    e.setFechaIngreso(fechaIngreso);
    e.setTipo(tipo);
    
    repo.actualizar(e);
    return e;
  }

  public void eliminar(int id) throws Exception {
    Empleado e = buscarPorId(id);
    if (e == null) {
      throw new IllegalArgumentException("No existe empleado con ID " + id + ".");
    }
    repo.eliminar(id);
  }

  // ── Helpers internos de Validación ────────────────────────────────────────

  private void validarBase(String nombre, String apellidos, String email, double salario, LocalDate fechaIngreso) {
    if (nombre == null || nombre.isEmpty()) {
      throw new IllegalArgumentException("El nombre es un campo obligatorio.");
    }
    if (apellidos == null || apellidos.isEmpty()) {
      throw new IllegalArgumentException("Los apellidos son campos obligatorios.");
    }
    if (!Validador.validarEmail(email)) {
      throw new IllegalArgumentException("El correo electrónico no tiene un formato válido (debe contener '@' y '.').");
    }
    if (salario < 0) {
      throw new IllegalArgumentException("El salario no puede ser un monto negativo.");
    }
    if (!Validador.validarFechaIngreso(fechaIngreso)) {
      throw new IllegalArgumentException("La fecha de ingreso es inválida (no puede ser una fecha futura ni nula).");
    }
  }
}