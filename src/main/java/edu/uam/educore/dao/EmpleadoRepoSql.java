package edu.uam.educore.dao;

import edu.uam.educore.db.Conexion;
import edu.uam.educore.db.ConfiguracionBD;
import edu.uam.educore.enums.TipoEmpleado;
import edu.uam.educore.model.personas.Empleado;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Empleado sobre base de datos relacional.
 * Cada operación abre una conexión nueva con try-with-resources.
 */
public class EmpleadoRepoSql extends Repositorio<Empleado> {

  private final ConfiguracionBD config;

  public EmpleadoRepoSql(ConfiguracionBD config) {
    this.config = config;
  }

  private Connection abrir() throws Exception {
    return Conexion.getConnection(config.url(), config.usuario(), config.contrasena());
  }

  @Override
  public void guardar(Empleado e) throws Exception {
    String sql =
        "INSERT INTO empleado (nombre, apellidos, email, salario, fecha_ingreso, tipo)"
            + " VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, e.getNombre());
      ps.setString(2, e.getApellidos());
      ps.setString(3, e.getEmail());
      ps.setDouble(4, e.getSalario());
      ps.setDate(5, Date.valueOf(e.getFechaIngreso()));
      ps.setString(6, e.getTipoEnum().name());
      ps.executeUpdate();
      try (ResultSet claves = ps.getGeneratedKeys()) {
        if (claves.next()) {
          e.setId(claves.getInt(1));
        }
      }
    }
  }

  @Override
  public void actualizar(Empleado e) throws Exception {
    String sql =
        "UPDATE empleado SET nombre=?, apellidos=?, email=?, salario=?, fecha_ingreso=?, tipo=?"
            + " WHERE id=?";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, e.getNombre());
      ps.setString(2, e.getApellidos());
      ps.setString(3, e.getEmail());
      ps.setDouble(4, e.getSalario());
      ps.setDate(5, Date.valueOf(e.getFechaIngreso()));
      ps.setString(6, e.getTipoEnum().name());
      ps.setInt(7, e.getId());
      ps.executeUpdate();
    }
  }

  @Override
  public void eliminar(int id) throws Exception {
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement("DELETE FROM empleado WHERE id=?")) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }

  @Override
  public Optional<Empleado> buscarPorId(int id) throws Exception {
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM empleado WHERE id=?")) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapear(rs));
        }
        return Optional.empty();
      }
    }
  }

  @Override
  public List<Empleado> buscarTodos() throws Exception {
    List<Empleado> lista = new ArrayList<>();
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM empleado");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        lista.add(mapear(rs));
      }
    }
    return lista;
  }

  private Empleado mapear(ResultSet rs) throws Exception {
    int id = rs.getInt("id");
    String nombre = rs.getString("nombre");
    String apellidos = rs.getString("apellidos");
    String email = rs.getString("email");
    double salario = rs.getDouble("salario");
    Date sqlDate = rs.getDate("fecha_ingreso");
    java.time.LocalDate fechaIngreso = sqlDate != null ? sqlDate.toLocalDate() : null;
    String tipoStr = rs.getString("tipo");
    TipoEmpleado tipo = tipoStr != null ? TipoEmpleado.valueOf(tipoStr) : null;
    return new Empleado(id, nombre, apellidos, email, salario, fechaIngreso, tipo);
  }
}
