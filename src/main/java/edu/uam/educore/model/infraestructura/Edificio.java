package edu.uam.educore.model.infraestructura;

import java.util.ArrayList;
import java.util.List;

public class Edificio {

  private int id;
  private String codigo;
  private String nombre;
  private List<Aula> aulas;

  public Edificio(int id, String codigo, String nombre) {
    this.id = id;
    this.codigo = codigo;
    this.nombre = nombre;
    this.aulas = new ArrayList<>();
  }

  public int getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public List<Aula> getAulas() {
    return new ArrayList<>(aulas); // Protege la encapsulación retornando una copia
  }

  public void agregarAula(Aula aula) {
    this.aulas.add(aula);
  }

  public void removerAula(int aulaId) {
    this.aulas.removeIf(a -> a.getId() == aulaId);
  }

  public String getInfo() {
    return String.format(
        "Edificio ID: %d | Código: %s | Nombre: %s | Cantidad de Aulas: %d",
        id, codigo, nombre, aulas.size());
  }
}