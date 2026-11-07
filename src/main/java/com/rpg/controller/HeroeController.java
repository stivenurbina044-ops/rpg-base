package com.rpg.controller;

import com.rpg.model.Heroe;
import com.rpg.persistencia.HeroeDAO;
import java.util.List;

public class HeroeController {
    private final HeroeDAO heroeDAO;

    public HeroeController() {
        this.heroeDAO = new HeroeDAO();
    }

    public String registrarHeroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "El nombre del heroe no puede estar vacio.";
        }

        String tipoUpper = tipo != null ? tipo.trim().toUpperCase() : "";
        if (!tipoUpper.equals("GUERRERO") && !tipoUpper.equals("MAGO")) {
            return "El tipo de heroe debe ser GUERRERO o MAGO.";
        }

        if (vidaMaxima <= 0) {
            return "La vida maxima debe ser mayor a 0.";
        }

        if (nivel < 1) {
            return "El nivel debe ser mayor o igual a 1.";
        }

        // Creamos la instancia del POJO Heroe requerida por el DAO
        Heroe heroe = new Heroe(nombre.trim(), tipoUpper, vidaMaxima, nivel);
        boolean exito = heroeDAO.insertar(heroe);

        if (exito) {
            return "Heroe registrado exitosamente.";
        } else {
            return "Error al registrar el heroe en la base de datos (verifique si el nombre ya existe).";
        }
    }

    public List<Heroe> obtenerTodosLosHeroes() {
        return heroeDAO.listar();
    }

    public String modificarNivel(int id, int nuevoNivel) {
        if (nuevoNivel < 1) {
            return "El nivel debe ser mayor o igual a 1.";
        }

        int filasAfectadas = heroeDAO.actualizarNivel(id, nuevoNivel);
        if (filasAfectadas > 0) {
            return "Nivel del heroe actualizado correctamente.";
        } else {
            return "no existe un heroe con ese id";
        }
    }

    public String borrarHeroe(int id) {
        int filasAfectadas = heroeDAO.eliminar(id);
        if (filasAfectadas > 0) {
            return "Heroe eliminado exitosamente.";
        } else {
            return "no existe un heroe con ese id";
        }
    }

    public Heroe buscarHeroePorId(int id) {
        return heroeDAO.buscarPorId(id);
    }
}