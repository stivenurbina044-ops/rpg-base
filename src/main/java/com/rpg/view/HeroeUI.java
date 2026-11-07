package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.model.Heroe;
import java.util.List;

public class HeroeUI {
    private final HeroeController controller;
    private final Consola consola;

    // Recibimos la Consola centralizada del juego para respetar el hilo lector
    public HeroeUI(Consola consola) {
        this.controller = new HeroeController();
        this.consola = consola;
    }

    public void iniciar() {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println("\n--- MENU REGISTRO DE HEROES ---");
            System.out.println("1. Registrar heroe");
            System.out.println("2. Listar todos los heroes");
            System.out.println("3. Actualizar nivel de heroe");
            System.out.println("4. Eliminar heroe");
            System.out.println("0. Salir");

            opcion = consola.leerEntero("Seleccione una opcion: ", 0, 4);

            switch (opcion) {
                case 1 -> registrar();
                case 2 -> listar();
                case 3 -> actualizarNivel();
                case 4 -> eliminar();
                case 0 -> System.out.println("Saliendo del modulo de heroes...");
            }
        }
    }

    private void registrar() {
        String nombre = consola.leerNombre("Nombre del heroe: ");

        String tipo = consola.leerLinea("Tipo (GUERRERO / MAGO): ").toUpperCase();
        while (!tipo.equals("GUERRERO") && !tipo.equals("MAGO")) {
            System.out.println("Tipo invalido. Debe ser GUERRERO o MAGO.");
            tipo = consola.leerLinea("Tipo (GUERRERO / MAGO): ").toUpperCase();
        }

        int vida = consola.leerEntero("Vida maxima: ", 1, 999999);
        int nivel = consola.leerEntero("Nivel: ", 1, 999);

        String resultado = controller.registrarHeroe(nombre, tipo, vida, nivel);
        System.out.println(resultado);
    }

    private void listar() {
        List<Heroe> heroes = controller.obtenerTodosLosHeroes();
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes registrados.");
        } else {
            System.out.println("\n--- LISTA DE HEROES ---");
            for (Heroe h : heroes) {
                System.out.println(h);
            }
        }
    }

    private void actualizarNivel() {
        int id = consola.leerEntero("ID del heroe a actualizar: ", 1, 999999);
        int nivel = consola.leerEntero("Nuevo nivel: ", 1, 999);

        String resultado = controller.modificarNivel(id, nivel);
        System.out.println(resultado);
    }

    private void eliminar() {
        int id = consola.leerEntero("ID del heroe a eliminar: ", 1, 999999);
        String resultado = controller.borrarHeroe(id);
        System.out.println(resultado);
    }
}