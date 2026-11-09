package com.rpg;

import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;
import com.rpg.view.HeroeUI;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // no se cierra: lo usa el hilo lector (daemon)
        try {
            Consola consola = new Consola(scanner);
            MotorJuego motor = new MotorJuego(consola);
            ejecutarMenu(scanner, consola, motor); // <-- Agregamos scanner aquí
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
        System.exit(0);
    }

    // Recibimos el scanner como parámetro
    private static void ejecutarMenu(Scanner scanner, Consola consola, MotorJuego motor) {
        HeroeController heroeController = new HeroeController();
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", 0, 8);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.verHistorial();
                case 5 -> motor.cargarPartida();
                case 6 -> motor.borrarArchivos();
                case 7 -> {
                    HeroeUI heroeUI = new HeroeUI(consola);
                    heroeUI.iniciar();
                }
                case 8 -> cargarHeroeYCombatir(consola, motor, heroeController);
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Ver historial de combates");
        System.out.println("5. Cargar partida");
        System.out.println("6. Borrar partida o historial");
        System.out.println("7. Gestionar heroes en BD (Modulo MVC)");
        System.out.println("8. Cargar heroe desde BD a combate rapido");
        System.out.println("0. Salir");
    }

    private static void cargarHeroeYCombatir(Consola consola, MotorJuego motor, HeroeController controller) {
        List<Heroe> heroes = controller.obtenerTodosLosHeroes();
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes registrados en la base de datos.");
            return;
        }

        System.out.println("\n--- SELECCIONE UN HEROE DE LA BD ---");
        for (Heroe h : heroes) {
            System.out.println("ID: " + h.getId() + " | Nombre: " + h.getNombre() + " | Tipo: " + h.getTipo());
        }

        int idSeleccionado = consola.leerEntero("Ingrese el ID del heroe: ", 1, 999999);
        Heroe datosHeroe = controller.buscarHeroePorId(idSeleccionado);

        if (datosHeroe == null) {
            System.out.println("No se encontro un heroe con ese ID.");
            return;
        }

        // Crear la entidad correspondiente (Guerrero o Mago) para el juego
        Personaje personajeJugador;
        if (datosHeroe.getTipo().equalsIgnoreCase("GUERRERO")) {
            personajeJugador = new Guerrero(datosHeroe.getNombre());
        } else {
            personajeJugador = new Mago(datosHeroe.getNombre());
        }

        System.out.println("¡Heroe " + datosHeroe.getNombre() + " cargado exitosamente desde la BD!");

        // Ejecutamos el combate rápido pasándole el personaje cargado (ajusta según los métodos de tu MotorJuego)
        // motor.combateRapidoConPersonaje(personajeJugador);
        motor.combateRapido(); // O invocas el flujo de combate que use tu motor
    }
}