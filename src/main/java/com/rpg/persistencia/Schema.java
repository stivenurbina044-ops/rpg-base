package com.rpg.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Schema {
    private static final String URL = "jdbc:mysql://localhost:3306/batalla-rpg";
    private static final String USUARIO = "campus2023"; // Tu usuario de MySQL
    private static final String CONTRASENA = "campus2023"; // Tu contraseña de MySQL

    public static Connection obtenerConexion() {
        try {
            return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos: " + e.getMessage());
            return null;
        }
    }
}