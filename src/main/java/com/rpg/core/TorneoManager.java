package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();
        Personaje[] finalistas = new Personaje[2];

        Thread semiA = new Thread(
                () -> finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1)), "semifinal-A");
        Thread semiB = new Thread(
                () -> finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3)), "semifinal-B");

        mostrarEstados("antes de start()", semiA, semiB);   // NEW
        semiA.start();
        semiB.start();
        mostrarEstados("durante la ejecucion", semiA, semiB); // RUNNABLE / TIMED_WAITING

        try {
            semiA.join();
            semiB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Torneo interrumpido", e);
        }
        mostrarEstados("despues de join()", semiA, semiB);    // TERMINATED

        // join() garantiza que los resultados ya son visibles para este hilo
        if (finalistas[0] == null || finalistas[1] == null) {
            throw new IllegalStateException("Una semifinal termino con error");
        }
        return simularFinal(finalistas[0], finalistas[1]);
    }

    private void mostrarEstados(String momento, Thread a, Thread b) {
        registro.agregar("[Hilos %s] %s=%s | %s=%s", momento,
                a.getName(), a.getState(), b.getName(), b.getState());
    }

    /** Pausa corta para poder observar TIMED_WAITING en la demostracion. */
    private void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
