package edu.uam.educore.dao;

import edu.uam.educore.db.Conexion;
import edu.uam.educore.db.ConfiguracionBD;
import edu.uam.educore.enums.TipoAula;
import edu.uam.educore.model.infraestructura.Aula;
import edu.uam.educore.model.infraestructura.Edificio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EdificioRepoSql extends Repositorio<Edificio> {

  private final ConfiguracionBD config;

  public EdificioRepoSql(ConfiguracionBD config) {
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
  public void guardar(Edificio e) throws Exception {
    String sql = "INSERT INTO edificio (codigo, nombre) VALUES (?, ?)";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, e.getCodigo());
      ps.setString(2, e.getNombre());
      ps.executeUpdate();
      try (ResultSet claves = ps.getGeneratedKeys()) {
        if (claves.next()) {
          setObjectId(e, claves.getInt(1));
        }
      }
    }
  }

  @Override
  public void actualizar(Edificio e) throws Exception {
    String updateEdificio = "UPDATE edificio SET codigo=?, nombre=? WHERE id=?";
    try (Connection con = abrir()) {
      con.setAutoCommit(false);
      try {
        try (PreparedStatement ps = con.prepareStatement(updateEdificio)) {
          ps.setString(1, e.getCodigo());
          ps.setString(2, e.getNombre());
          ps.setInt(3, e.getId());
          ps.executeUpdate();
        }

        // Get existing aula IDs in DB for this edificio
        List<Integer> dbAulaIds = new ArrayList<>();
        String selectAulas = "SELECT id FROM aula WHERE edificio_id=?";
        try (PreparedStatement ps = con.prepareStatement(selectAulas)) {
          ps.setInt(1, e.getId());
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              dbAulaIds.add(rs.getInt("id"));
            }
          }
        }

        List<Aula> currentAulas = e.getAulas();
        List<Integer> currentAulaIds = new ArrayList<>();
        for (Aula a : currentAulas) {
          currentAulaIds.add(a.getId());
        }

        // Deletes
        String deleteAula = "DELETE FROM aula WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(deleteAula)) {
          for (int dbId : dbAulaIds) {
            if (!currentAulaIds.contains(dbId)) {
              ps.setInt(1, dbId);
              ps.executeUpdate();
            }
          }
        }

        // Updates & Inserts
        String updateAula = "UPDATE aula SET numero=?, capacidad=?, tipo=? WHERE id=?";
        String insertAula = "INSERT INTO aula (numero, capacidad, tipo, edificio_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement psUpdate = con.prepareStatement(updateAula);
             PreparedStatement psInsert = con.prepareStatement(insertAula, Statement.RETURN_GENERATED_KEYS)) {
          for (Aula a : currentAulas) {
            if (dbAulaIds.contains(a.getId())) {
              psUpdate.setString(1, a.getNumero());
              psUpdate.setInt(2, a.getCapacidad());
              psUpdate.setString(3, a.getTipoEnum().name());
              psUpdate.setInt(4, a.getId());
              psUpdate.executeUpdate();
            } else {
              psInsert.setString(1, a.getNumero());
              psInsert.setInt(2, a.getCapacidad());
              psInsert.setString(3, a.getTipoEnum().name());
              psInsert.setInt(4, e.getId());
              psInsert.executeUpdate();
              try (ResultSet rs = psInsert.getGeneratedKeys()) {
                if (rs.next()) {
                  setObjectId(a, rs.getInt(1));
                }
              }
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
    String sql = "DELETE FROM edificio WHERE id=?";
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }

  @Override
  public Optional<Edificio> buscarPorId(int id) throws Exception {
    String sql =
        "SELECT e.id AS ed_id, e.codigo AS ed_codigo, e.nombre AS ed_nombre, "
            + "a.id AS au_id, a.numero AS au_numero, a.capacidad AS au_capacidad, a.tipo AS au_tipo "
            + "FROM edificio e "
            + "LEFT JOIN aula a ON e.id = a.edificio_id "
            + "WHERE e.id = ? "
            + "ORDER BY a.id";
    Edificio ed = null;
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          if (ed == null) {
            ed = new Edificio(id, rs.getString("ed_codigo"), rs.getString("ed_nombre"));
          }
          int auId = rs.getInt("au_id");
          if (!rs.wasNull()) {
            TipoAula auTipo = TipoAula.valueOf(rs.getString("au_tipo"));
            Aula au = new Aula(auId, rs.getString("au_numero"), rs.getInt("au_capacidad"), auTipo, ed);
            ed.agregarAula(au);
          }
        }
      }
    }
    return Optional.ofNullable(ed);
  }

  @Override
  public List<Edificio> buscarTodos() throws Exception {
    String sql =
        "SELECT e.id AS ed_id, e.codigo AS ed_codigo, e.nombre AS ed_nombre, "
            + "a.id AS au_id, a.numero AS au_numero, a.capacidad AS au_capacidad, a.tipo AS au_tipo "
            + "FROM edificio e "
            + "LEFT JOIN aula a ON e.id = a.edificio_id "
            + "ORDER BY e.id, a.id";
    List<Edificio> edificios = new ArrayList<>();
    java.util.Map<Integer, Edificio> map = new java.util.LinkedHashMap<>();
    try (Connection con = abrir();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        int edId = rs.getInt("ed_id");
        Edificio ed = map.get(edId);
        if (ed == null) {
          ed = new Edificio(edId, rs.getString("ed_codigo"), rs.getString("ed_nombre"));
          map.put(edId, ed);
          edificios.add(ed);
        }
        int auId = rs.getInt("au_id");
        if (!rs.wasNull()) {
          TipoAula auTipo = TipoAula.valueOf(rs.getString("au_tipo"));
          Aula au = new Aula(auId, rs.getString("au_numero"), rs.getInt("au_capacidad"), auTipo, ed);
          ed.agregarAula(au);
        }
      }
    }
    return edificios;
  }
}
