package com.rpg.core;

import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.LectorTeclado;

import java.util.OptionalInt;
import java.util.Scanner;

/** Entrada por teclado con validacion, reintento y limite de tiempo opcional. */
public class Consola {

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private final LectorTeclado lector;
    private final GestorTurno gestorTurno = new GestorTurno();

    public Consola(Scanner scanner) {
        this.lector = new LectorTeclado(scanner);
        this.lector.iniciar();
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        try {
            return lector.leerLinea().trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lectura interrumpida", e);
        }
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }

    /** Como leerEntero, pero con tiempo maximo total. Vacio si se agoto el tiempo. */
    public OptionalInt leerEnteroConLimite(String mensaje, int minimo, int maximo, int segundos) {
        lector.limpiarBuffer();
        gestorTurno.iniciar(segundos, lector::notificarTiempoAgotado);
        System.out.print(mensaje);
        try {
            while (true) {
                String texto = lector.leerLineaOTiempo();
                if (texto == null) {
                    return OptionalInt.empty();
                }
                texto = texto.trim();
                try {
                    int valor = Integer.parseInt(texto);
                    if (valor >= minimo && valor <= maximo) {
                        return OptionalInt.of(valor);
                    }
                    System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
                } catch (NumberFormatException e) {
                    System.out.println(texto.isEmpty() ? "No escribiste nada."
                            : "\"" + texto + "\" no es un numero.");
                }
                System.out.print(mensaje);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OptionalInt.empty();
        } finally {
            gestorTurno.cancelar();
        }
    }
}