# Simulador de Batalla RPG — Unidad 5.2: JDBC y arquitectura MVC

Proyecto Java desarrollado sobre `rpg-base` para incorporar persistencia de héroes en **MySQL** mediante **JDBC**, aplicando los patrones **MVC (Modelo–Vista–Controlador)** y **DAO (Data Access Object)**.

La ampliación permite registrar y administrar héroes desde un menú de consola, conservarlos en una base de datos y recuperarlos para utilizarlos en el simulador de batalla. Los módulos anteriores del juego (`core`, `entities`, `items`, `exceptions` y `concurrency`) se mantienen como parte del proyecto.

---

## 1. Objetivos implementados

La solución aborda los requisitos de la Unidad 5.2:

- Conexión a MySQL mediante JDBC y `mysql-connector-j` administrado por Maven.
- Configuración de conexión externa al código fuente mediante `config.txt`.
- Separación de responsabilidades entre View, Controller, Model y DAO.
- Consultas parametrizadas con `PreparedStatement`.
- Uso de `executeUpdate()` para operaciones de escritura y `executeQuery()` para consultas.
- Cierre de conexiones, sentencias y resultados mediante `try-with-resources`.
- Validación de los datos del héroe y manejo de errores con mensajes comprensibles.
- Integración de los héroes persistidos con el simulador RPG.

## 2. Arquitectura y separación de responsabilidades

El módulo de persistencia sigue el flujo:

`HeroeUI (View) → HeroeController → HeroeDAO → MySQL`

Cada capa tiene una responsabilidad definida:

| Capa | Clase | Responsabilidad |
|---|---|---|
| Vista | `HeroeUI` | Presenta el menú, captura los datos por consola y muestra los resultados. No contiene instrucciones JDBC ni accede directamente a MySQL. |
| Controlador | `HeroeController` | Coordina las operaciones, valida las reglas del módulo y solicita las acciones correspondientes al DAO. |
| Modelo | `Heroe` | Representa al héroe mediante atributos privados, constructor y métodos de acceso. Incluye validaciones en sus setters. |
| Persistencia / DAO | `HeroeDAO` | Centraliza las operaciones JDBC sobre la tabla `heroes`. |
| Configuración de BD | `ConexionDB` | Lee los parámetros de conexión desde `config.txt` y obtiene la conexión con `DriverManager`. |
| Esquema | `Schema` | Contiene la definición o gestión del esquema de la base de datos. |[cite: 1]

**Principio aplicado:** la vista no conoce la base de datos; el controlador no ejecuta SQL; y las operaciones JDBC se concentran en la capa DAO.

### Estructura del proyecto

La estructura general conserva los paquetes del simulador original y añade las clases del módulo de persistencia:

```text
rpg-base/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── rpg/
│                   ├── Main.java
│                   ├── controller/
│                   │   └── HeroeController.java
│                   ├── model/
│                   │   └── Heroe.java
│                   ├── view/
│                   │   └── HeroeUI.java
│                   ├── persistencia/
│                   │   ├── HeroeDAO.java
│                   │   └── Schema.java
│                   ├── core/
│                   ├── entities/
│                   ├── items/
│                   ├── exceptions/
│                   └── concurrency/
├── config.example.txt
├── config.txt
├── pom.xml
├── .gitignore
└── README.md