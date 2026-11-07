package com.rpg.persistencia;

import com.rpg.model.Heroe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HeroeDAO {

    public boolean insertar(Heroe heroe) {
        String sql = "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
        try (Connection conn = Schema.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return false;

            stmt.setString(1, heroe.getNombre());
            stmt.setString(2, heroe.getTipo());
            stmt.setInt(3, heroe.getVidaMaxima());
            stmt.setInt(4, heroe.getNivel());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en DAO al insertar heroe: " + e.getMessage());
            return false;
        }
    }

    public List<Heroe> listar() {
        List<Heroe> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes";
        try (Connection conn = Schema.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (conn == null) return lista;

            while (rs.next()) {
                Heroe h = new Heroe(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getInt("vida_maxima"),
                        rs.getInt("nivel")
                );
                lista.add(h);
            }
        } catch (SQLException e) {
            System.err.println("Error en DAO al listar heroes: " + e.getMessage());
        }
        return lista;
    }

    public Heroe buscarPorId(int id) {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE id = ?";
        try (Connection conn = Schema.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (conn == null) return null;
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Heroe(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getInt("vida_maxima"),
                            rs.getInt("nivel")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en DAO: " + e.getMessage());
        }
        return null;
    }

    public List<Heroe> buscarPorTipo(String tipo) {
        List<Heroe> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ?";
        try (Connection conn = Schema.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return lista;

            stmt.setString(1, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Heroe(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getInt("vida_maxima"),
                            rs.getInt("nivel")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en DAO al buscar heroes por tipo: " + e.getMessage());
        }
        return lista;
    }

    public int actualizarNivel(int id, int nivel) {
        String sql = "UPDATE heroes SET nivel = ? WHERE id = ?";
        try (Connection conn = Schema.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return 0;
            stmt.setInt(1, nivel);
            stmt.setInt(2, id);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error en DAO al actualizar nivel: " + e.getMessage());
            return 0;
        }
    }

    public int eliminar(int id) {
        String sql = "DELETE FROM heroes WHERE id = ?";
        try (Connection conn = Schema.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (conn == null) return 0;
            stmt.setInt(1, id);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error en DAO al eliminar heroe: " + e.getMessage());
            return 0;
        }
    }
}