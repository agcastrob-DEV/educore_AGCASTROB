package edu.uam.educore.model.infraestructura;

import static org.junit.jupiter.api.Assertions.*;

import edu.uam.educore.controller.EdificioController;
import edu.uam.educore.dao.ListaEdificioRepo;
import edu.uam.educore.enums.TipoAula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InfraestructuraTest {

  private ListaEdificioRepo repo;
  private EdificioController controller;

  @BeforeEach
  void setUp() {
    this.repo = new ListaEdificioRepo();
    this.controller = new EdificioController(repo);
  }

  @Test
  void registrarEdificio_asigna_propiedades_correctamente() throws Exception {
    Edificio ed = controller.registrarEdificio("ED-01", "Pabellón de Ingeniería");
    
    assertEquals(1, ed.getId());
    assertEquals("ED-01", ed.getCodigo());
    assertEquals("Pabellón de Ingeniería", ed.getNombre());
    assertTrue(ed.getAulas().isEmpty());
  }

  @Test
  void registrarAula_se_asocia_correctamente_al_edificio_y_guarda_datos() throws Exception {
    Edificio ed = controller.registrarEdificio("ED-02", "Ciencias Básicas");
    Aula au = controller.registrarAula(ed.getId(), "Lab 102", 30, TipoAula.LABORATORIO);
    
    assertEquals(1, au.getId());
    assertEquals("Lab 102", au.getNumero());
    assertEquals(30, au.getCapacidad());
    assertEquals(TipoAula.LABORATORIO, au.getTipoEnum());
    assertEquals("Laboratorio", au.getTipoTexto());
    
    // Validar composición bidireccional
    assertEquals(ed.getId(), au.getEdificio().getId());
    assertEquals(1, ed.getAulas().size());
    assertEquals("Lab 102", ed.getAulas().get(0).getNumero());
  }

  @Test
  void eliminarEdificio_con_aulas_lanza_excepcion_de_integridad() throws Exception {
    Edificio ed = controller.registrarEdificio("ED-03", "Aulas Generales");
    controller.registrarAula(ed.getId(), "Aula 201", 40, TipoAula.REGULAR);
    
    // Regla de oro del PDF: No se puede borrar si tiene aulas activas
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      controller.eliminarEdificio(ed.getId());
    });
    
    assertTrue(ex.getMessage().contains("todavía tiene aulas asociadas"));
  }
}