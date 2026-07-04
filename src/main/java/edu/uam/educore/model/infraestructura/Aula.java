package edu.uam.educore.model.infraestructura;

import edu.uam.educore.enums.TipoAula;

public class Aula {

  private int id;
  private String numero;
  private int capacidad;
  private TipoAula tipo;
  private Edificio edificio;

  public Aula(int id, String numero, int capacidad, TipoAula tipo, Edificio edificio) {
    this.id = id;
    this.numero = numero;
    this.capacidad = capacidad;
    this.tipo = tipo;
    this.edificio = edificio;
  }

  public int getId() {
    return id;
  }

  public String getNumero() {
    return numero;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }

  public int getCapacidad() {
    return capacidad;
  }

  public void setCapacidad(int capacidad) {
    this.capacidad = capacidad;
  }

  public TipoAula getTipoEnum() {
    return tipo;
  }

  public String getTipoTexto() {
    switch (this.tipo) {
      case REGULAR: return "Regular";
      case LABORATORIO: return "Laboratorio";
      case AUDITORIO: return "Auditorio";
      default: return "Aula";
    }
  }

  public void setTipo(TipoAula tipo) {
    this.tipo = tipo;
  }

  public Edificio getEdificio() {
    return edificio;
  }

  public void setEdificio(Edificio edificio) {
    this.edificio = edificio;
  }

  public String getInfo() {
    return String.format(
        "Aula ID: %d | Número: %s | Capacidad: %d personas | Tipo: %s",
        id, numero, capacidad, getTipoTexto());
  }
}