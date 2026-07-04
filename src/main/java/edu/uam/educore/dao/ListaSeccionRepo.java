package edu.uam.educore.dao;

import edu.uam.educore.model.academico.Seccion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListaSeccionRepo extends Repositorio<Seccion> {

  private final List<Seccion> lista = new ArrayList<>();

  @Override
  public void guardar(Seccion s) throws Exception {
    lista.add(s);
  }

  @Override
  public void actualizar(Seccion actualizada) throws Exception {
    for (int i = 0; i < lista.size(); i++) {
      if (lista.get(i).getId() == actualizada.getId()) {
        lista.set(i, actualizada);
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
  public Optional<Seccion> buscarPorId(int id) throws Exception {
    for (Seccion s : lista) {
      if (s.getId() == id) {
        return Optional.of(s);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Seccion> buscarTodos() throws Exception {
    return new ArrayList<>(lista); // Retorna copia de seguridad
  }
}