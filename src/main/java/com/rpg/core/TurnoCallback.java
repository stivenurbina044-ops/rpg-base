package com.rpg.core;

/** Se invoca cuando el temporizador de turno llega a cero. */
@FunctionalInterface
public interface TurnoCallback {
    void onTiempoAgotado();
}