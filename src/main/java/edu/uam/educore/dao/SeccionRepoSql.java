package edu.uam.educore.dao;

import edu.uam.educore.db.Conexion;
import edu.uam.educore.db.ConfiguracionBD;
import edu.uam.educore.enums.TipoAula;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.academico.Seccion;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import edu.uam.educore.model.personas.Empleado;
import edu.uam.educore.model.personas.Estudiante;
import edu.uam.educore.model.personas.EstudianteBecado;
import edu.uam.educore.model.personas.EstudianteRegular;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeccionRepoSql extends Repositorio<Seccion> {

  private final ConfiguracionBD config;

  public SeccionRepoSql(ConfiguracionBD config) {
    this.config = config;
  }

  private Connection abrir() throws Exception {
    return Conexion.getConnection(config.url(), config.usuario(), config.contrasena());
  }

  private void setObjectId(Object obj, int id) {
    try {
      java.lang.reflect.Field field = obj.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(obj, id);
    } catch (Exception ex) {
      // Ignorar
    }
  }

  @Override
  public void guardar(Seccion s) throws Exception {
    String sql = "INSERT INTO seccion (codigo, nombre, docente_id, aula_id) VALUES (?, ?, ?, ?)";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, s.getCodigoSeccion());
      ps.setString(2, s.getCurso());
      ps.setInt(3, s.getProfesor().getId());
      ps.setInt(4, s.getAula().getId());
      ps.executeUpdate();
      try (ResultSet claves = ps.getGeneratedKeys()) {
        if (claves.next()) {
          setObjectId(s, claves.getInt(1));
        }
      }
    }
  }

  @Override
  public void actualizar(Seccion s) throws Exception {
    String sql = "UPDATE seccion SET codigo=?, nombre=?, docente_id=?, aula_id=? WHERE id=?";
    try (Connection con = abrir()) {
      con.setAutoCommit(false);
      try {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
          ps.setString(1, s.getCodigoSeccion());
          ps.setString(2, s.getCurso());
          ps.setInt(3, s.getProfesor().getId());
          ps.setInt(4, s.getAula().getId());
          ps.setInt(5, s.getId());
          ps.executeUpdate();
        }

        // Sync matricula table
        List<Integer> dbEstIds = new ArrayList<>();
        String selectMat = "SELECT estudiante_id FROM matricula WHERE seccion_id=?";
        try (PreparedStatement ps = con.prepareStatement(selectMat)) {
          ps.setInt(1, s.getId());
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              dbEstIds.add(rs.getInt("estudiante_id"));
            }
          }
        }

        List<Estudiante> currentEsts = s.getEstudiantesMatriculados();
        List<Integer> currentEstIds = new ArrayList<>();
        for (Estudiante e : currentEsts) {
          currentEstIds.add(e.getId());
        }

        // Delete matriculas
        String deleteMat = "DELETE FROM matricula WHERE seccion_id=? AND estudiante_id=?";
        try (PreparedStatement ps = con.prepareStatement(deleteMat)) {
          for (int dbId : dbEstIds) {
            if (!currentEstIds.contains(dbId)) {
              ps.setInt(1, s.getId());
              ps.setInt(2, dbId);
              ps.executeUpdate();
            }
          }
        }

        // Insert matriculas
        String insertMat = "INSERT INTO matricula (seccion_id, estudiante_id) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(insertMat)) {
          for (Estudiante e : currentEsts) {
            if (!dbEstIds.contains(e.getId())) {
              ps.setInt(1, s.getId());
              ps.setInt(2, e.getId());
              ps.executeUpdate();
            }
          }
        }

        con.commit();
      } catch (Exception ex) {
        con.rollback();
        throw ex;
      } finally {
        con.setAutoCommit(true);
      }
    }
  }

  @Override
  public void eliminar(int id) throws Exception {
    String sql = "DELETE FROM seccion WHERE id=?";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }

  @Override
  public Optional<Seccion> buscarPorId(int id) throws Exception {
    String sql =
        "SELECT s.id AS sec_id, s.codigo AS sec_codigo, s.nombre AS sec_nombre, "
            + "emp.id AS emp_id, emp.nombre AS emp_nombre, emp.apellidos AS emp_apellidos, emp.email AS emp_email, emp.salario AS emp_salario, emp.fecha_ingreso AS emp_fecha, emp.tipo AS emp_tipo, "
            + "au.id AS au_id, au.numero AS au_numero, au.capacidad AS au_capacidad, au.tipo AS au_tipo, "
            + "ed.id AS ed_id, ed.codigo AS ed_codigo, ed.nombre AS ed_nombre "
            + "FROM seccion s "
            + "JOIN empleado emp ON s.docente_id = emp.id "
            + "JOIN aula au ON s.aula_id = au.id "
            + "JOIN edificio ed ON au.edificio_id = ed.id "
            + "WHERE s.id = ?";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Seccion s = mapearSeccion(rs);
          for (Estudiante est : cargarEstudiantes(con, id)) {
            s.matricularEstudiante(est);
          }
          return Optional.of(s);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Seccion> buscarTodos() throws Exception {
    String sql =
        "SELECT s.id AS sec_id, s.codigo AS sec_codigo, s.nombre AS sec_nombre, "
            + "emp.id AS emp_id, emp.nombre AS emp_nombre, emp.apellidos AS emp_apellidos, emp.email AS emp_email, emp.salario AS emp_salario, emp.fecha_ingreso AS emp_fecha, emp.tipo AS emp_tipo, "
            + "au.id AS au_id, au.numero AS au_numero, au.capacidad AS au_capacidad, au.tipo AS au_tipo, "
            + "ed.id AS ed_id, ed.codigo AS ed_codigo, ed.nombre AS ed_nombre "
            + "FROM seccion s "
            + "JOIN empleado emp ON s.docente_id = emp.id "
            + "JOIN aula au ON s.aula_id = au.id "
            + "JOIN edificio ed ON au.edificio_id = ed.id";
    List<Seccion> lista = new ArrayList<>();
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Seccion s = mapearSeccion(rs);
        lista.add(s);
      }
      for (Seccion s : lista) {
        for (Estudiante est : cargarEstudiantes(con, s.getId())) {
          s.matricularEstudiante(est);
        }
      }
    }
    return lista;
  }

  private Seccion mapearSeccion(ResultSet rs) throws Exception {
    int secId = rs.getInt("sec_id");
    String secCodigo = rs.getString("sec_codigo");
    String secNombre = rs.getString("sec_nombre");

    // Docente
    int empId = rs.getInt("emp_id");
    String empNombre = rs.getString("emp_nombre");
    String empApellidos = rs.getString("emp_apellidos");
    String empEmail = rs.getString("emp_email");
    double empSalario = rs.getDouble("emp_salario");
    Date empFechaSql = rs.getDate("emp_fecha");
    java.time.LocalDate empFecha = empFechaSql != null ? empFechaSql.toLocalDate() : null;
    TipoEmpleado empTipo = TipoEmpleado.valueOf(rs.getString("emp_tipo"));
    Empleado docente = new Empleado(empId, empNombre, empApellidos, empEmail, empSalario, empFecha, empTipo);

    // Edificio
    int edId = rs.getInt("ed_id");
    String edCodigo = rs.getString("ed_codigo");
    String edNombre = rs.getString("ed_nombre");
    Edificio ed = new Edificio(edId, edCodigo, edNombre);

    // Aula
    int auId = rs.getInt("au_id");
    String auNumero = rs.getString("au_numero");
    int auCapacidad = rs.getInt("au_capacidad");
    TipoAula auTipo = TipoAula.valueOf(rs.getString("au_tipo"));
    Aula aula = new Aula(auId, auNumero, auCapacidad, auTipo, ed);
    ed.agregarAula(aula);

    return new Seccion(secId, secCodigo, secNombre, docente, aula);
  }

  private List<Estudiante> cargarEstudiantes(Connection con, int seccionId) throws Exception {
    List<Estudiante> estudiantes = new ArrayList<>();
    String sql =
        "SELECT e.id, e.tipo, e.nombre, e.apellidos, e.email, e.carnet, e.porcentaje_beca "
            + "FROM matricula m "
            + "JOIN estudiante e ON m.estudiante_id = e.id "
            + "WHERE m.seccion_id = ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, seccionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int id = rs.getInt("id");
          String nombre = rs.getString("nombre");
          String apellidos = rs.getString("apellidos");
          String email = rs.getString("email");
          String carnet = rs.getString("carnet");
          Estudiante est;
          if ("BECADO".equals(rs.getString("tipo"))) {
            est = new EstudianteBecado(id, nombre, apellidos, email, carnet, rs.getDouble("porcentaje_beca"));
          } else {
            est = new EstudianteRegular(id, nombre, apellidos, email, carnet);
          }
          estudiantes.add(est);
        }
      }
    }
    return estudiantes;
  }
}
