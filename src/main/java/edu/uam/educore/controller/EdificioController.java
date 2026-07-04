package edu.uam.educore.controller;

import edu.uam.educore.dao.Repositorio;
import edu.uam.educore.enums.TipoAula;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import java.util.List;
import java.util.Optional;

public class EdificioController {

  private final Repositorio<Edificio> repo;
  private int proximoEdificioId = 1;
  private int proximoAulaId = 1; // Contador GLOBAL y ÚNICO para todas las aulas del sistema

  public EdificioController(Repositorio<Edificio> repo) {
    this.repo = repo;
  }

  // ── MÉTODOS DE EDIFICIO ───────────────────────────────────────────────────

  public Edificio registrarEdificio(String codigo, String nombre) throws Exception {
    validarEdificioBase(codigo, nombre);
    
    Edificio nuevo = new Edificio(proximoEdificioId, codigo, nombre);
    repo.guardar(nuevo);
    proximoEdificioId++;
    return nuevo;
  }

  public List<Edificio> listarEdificios() throws Exception {
    return repo.buscarTodos();
  }

  public Edificio buscarEdificioPorId(int id) throws Exception {
    Optional<Edificio> resultado = repo.buscarPorId(id);
    return resultado.orElse(null);
  }

  public Edificio actualizarEdificio(int id, String codigo, String nombre) throws Exception {
    Edificio ed = buscarEdificioPorId(id);
    if (ed == null) {
      throw new IllegalArgumentException("No existe edificio con ID " + id + ".");
    }
    
    validarEdificioBase(codigo, nombre);
    
    ed.setCodigo(codigo);
    ed.setNombre(nombre);
    repo.actualizar(ed);
    return ed;
  }

  public void eliminarEdificio(int id) throws Exception {
    Edificio ed = buscarEdificioPorId(id);
    if (ed == null) {
      throw new IllegalArgumentException("No existe edificio con ID " + id + ".");
    }
    
    // Regla de integridad referencial del PDF
    if (!ed.getAulas().isEmpty()) {
      throw new IllegalArgumentException("No se puede eliminar el edificio porque todavía tiene aulas asociadas.");
    }
    
    repo.eliminar(id);
  }

  // ── MÉTODOS DE AULA (COMPOSICIÓN) ─────────────────────────────────────────

  public Aula registrarAula(int edificioId, String numero, int capacidad, TipoAula tipo) throws Exception {
    Edificio ed = buscarEdificioPorId(edificioId);
    if (ed == null) {
      throw new IllegalArgumentException("No existe el edificio con ID " + edificioId + " para asociar el aula.");
    }
    
    validarAulaBase(numero, capacidad);
    
    Aula nueva = new Aula(proximoAulaId, numero, capacidad, tipo, ed);
    ed.agregarAula(nueva);
    repo.actualizar(ed); // Persiste el cambio del edificio en el repositorio
    
    proximoAulaId++; // Incrementa el secuencial global
    return nueva;
  }

  public void eliminarAula(int edificioId, int aulaId) throws Exception {
    Edificio ed = buscarEdificioPorId(edificioId);
    if (ed == null) {
      throw new IllegalArgumentException("No existe el edificio con ID " + edificioId + ".");
    }
    
    boolean existeAula = ed.getAulas().stream().anyMatch(a -> a.getId() == aulaId);
    if (!existeAula) {
      throw new IllegalArgumentException("El aula con ID " + aulaId + " no pertenece al edificio indicado.");
    }
    
    ed.removerAula(aulaId);
    repo.actualizar(ed);
  }

  // ── HELPERS INTERNOS DE VALIDACIÓN ────────────────────────────────────────

  private void validarEdificioBase(String codigo, String nombre) {
    if (codigo == null || codigo.isEmpty()) {
      throw new IllegalArgumentException("El código del edificio es obligatorio.");
    }
    if (nombre == null || nombre.isEmpty()) {
      throw new IllegalArgumentException("El nombre del edificio es obligatorio.");
    }
  }

  private void validarAulaBase(String numero, int capacidad) {
    if (numero == null || numero.isEmpty()) {
      throw new IllegalArgumentException("El número o código de aula es obligatorio.");
    }
    if (capacidad <= 0) {
      throw new IllegalArgumentException("La capacidad del aula debe ser mayor a 0 personas.");
    }
  }
}