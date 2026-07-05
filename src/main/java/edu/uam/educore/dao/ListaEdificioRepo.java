package edu.uam.educore.dao;

import edu.uam.educore.model.infraestructura.Edificio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListaEdificioRepo extends Repositorio<Edificio> {

  private final List<Edificio> lista = new ArrayList<>();

  @Override
  public void guardar(Edificio e) throws Exception {
    lista.add(e);
  }

  @Override
  public void actualizar(Edificio actualizado) throws Exception {
    for (int i = 0; i < lista.size(); i++) {
      if (lista.get(i).getId() == actualizado.getId()) {
        lista.set(i, actualizado);
        return;
      }
    }
  }

  @Override
  public void eliminar(int id) throws Exception {
    for (int i = 0; i < lista.size(); i++) {
      if (lista.get(i).getId() == id) {
        lista.remove(i);
        return;
      }
    }
  }

  @Override
  public Optional<Edificio> buscarPorId(int id) throws Exception {
    for (Edificio e : lista) {
      if (e.getId() == id) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Edificio> buscarTodos() throws Exception {
    // Retorna una copia para proteger los datos originales de modificaciones
    // externas
    return new ArrayList<>(lista);
  }
}