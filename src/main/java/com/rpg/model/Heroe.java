package com.rpg.model;

public class Heroe {
    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    public Heroe() {
    }

    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.nivel = nivel;
    }

    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.nivel = nivel;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getVidaMaxima() { return vidaMaxima; }
    public void setVidaMaxima(int vidaMaxima) { this.vidaMaxima = vidaMaxima; }
    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    @Override
    public String toString() {
        return "ID: " + id + " | Nombre: " + nombre + " | Tipo: " + tipo + " | Vida Máxima: " + vidaMaxima + " | Nivel: " + nivel;
    }
}