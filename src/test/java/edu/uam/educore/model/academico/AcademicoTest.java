package edu.uam.educore.model.academico;

import static org.junit.jupiter.api.Assertions.*;

import edu.uam.educore.controller.SeccionController;
import edu.uam.educore.dao.ListaSeccionRepo;
import edu.uam.educore.enums.TipoAula;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.model.personas.Estudiante;
import edu.uam.educore.model.personas.EstudianteRegular;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcademicoTest {

  private ListaSeccionRepo seccionRepo;
  private SeccionController controller;
  private Empleado docenteValido;
  private Empleado administrativoInvalido;
  private Aula aulaPequena;

  @BeforeEach
  void setUp() {
    this.seccionRepo = new ListaSeccionRepo();
    this.controller = new SeccionController(seccionRepo);

    // Configurar datos de prueba básicos
    LocalDate fecha = LocalDate.now();
    this.docenteValido = new Empleado(10, "Carlos", "Alvarado", "carlos@uam.edu", 600000.0, fecha,
        TipoEmpleado.DOCENTE);
    this.administrativoInvalido = new Empleado(11, "Ana", "Mora", "ana@uam.edu", 450000.0, fecha,
        TipoEmpleado.ADMINISTRATIVO);

    Edificio ed = new Edificio(1, "ED-01", "Bloque A");
    this.aulaPequena = new Aula(1, "Aula 101", 1, TipoAula.REGULAR, ed); // Capacidad: 1 persona
  }

  @Test
  void abrirSeccion_con_docente_valido_crea_correctamente() throws Exception {
    Seccion s = controller.abrirSeccion("SC-100", "Programación I", docenteValido, aulaPequena);

    assertEquals(1, s.getId());
    assertEquals("SC-100", s.getCodigoSeccion());
    assertEquals("Programación I", s.getCurso());
    assertEquals(docenteValido.getId(), s.getProfesor().getId());
    assertEquals(aulaPequena.getId(), s.getAula().getId());
  }

  @Test
  void abrirSeccion_con_empleado_no_docente_lanza_excepcion() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      controller.abrirSeccion("SC-200", "Contabilidad I", administrativoInvalido, aulaPequena);
    });

    assertTrue(ex.getMessage().contains("debe tener el rol de Docente"));
  }

  @Test
  void matricularEstudiante_respeta_limite_de_capacidad_del_aula() throws Exception {
    Seccion s = controller.abrirSeccion("SC-300", "Bases de Datos", docenteValido, aulaPequena);

    // Pasamos el carnet como String entre comillas tal como lo define el modelo
    // original
    Estudiante e1 = new EstudianteRegular(1, "Andres", "Castro", "andres@uam.edu", "2026-001");
    Estudiante e2 = new EstudianteRegular(2, "Kevin", "Solano", "kevin@uam.edu", "2026-002");

    // Primera matrícula entra bien (capacidad del aula es 1)
    assertDoesNotThrow(() -> controller.matricularEstudiante(s.getId(), e1));

    // Segunda matrícula debe rebotar por falta de espacio
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      controller.matricularEstudiante(s.getId(), e2);
    });

    assertTrue(ex.getMessage().contains("Capacidad máxima alcanzada"));
  }
}