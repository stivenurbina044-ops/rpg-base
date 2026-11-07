package com.rpg.concurrency;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Hilo demonio que lee el teclado y publica cada linea en una cola bloqueante. */
public class LectorTeclado implements Runnable {

    private static final String FIN = new String("<EOF>");
    private static final String TIEMPO = new String("<TIEMPO>");

    private final BlockingQueue<String> lineas = new LinkedBlockingQueue<>();
    private final Scanner scanner;

    public LectorTeclado(Scanner scanner) {
        this.scanner = scanner;
    }

    public void iniciar() {
        Thread hilo = new Thread(this, "lector-teclado");
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public void run() {
        try {
            while (scanner.hasNextLine()) {
                lineas.add(scanner.nextLine());
            }
        } finally {
            lineas.add(FIN);
        }
    }

    public String leerLinea() throws InterruptedException {
        return validar(lineas.take());
    }

    public String leerLinea(long limite, TimeUnit unidad) throws InterruptedException {
        String linea = lineas.poll(limite, unidad);
        return linea == null ? null : validar(linea);
    }

    /** Lo llama el temporizador: despierta a quien este esperando una linea. */
    public void notificarTiempoAgotado() {
        lineas.add(TIEMPO);
    }

    /** Devuelve null si el temporizador aviso que se agoto el tiempo. */
    public String leerLineaOTiempo() throws InterruptedException {
        String linea = lineas.take();
        return linea == TIEMPO ? null : validar(linea);
    }

    public void limpiarBuffer() {
        lineas.removeIf(s -> s != FIN);
    }

    private String validar(String linea) {
        if (linea == FIN) {
            lineas.add(FIN);
            throw new NoSuchElementException("Fin de la entrada");
        }
        return linea;
    }
}