package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorArchivos;
import com.rpg.persistencia.LogCombateArchivo;
import com.rpg.persistencia.PartidaRepositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario, guardado y archivos. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int SEGUNDOS_TURNO = 10;
    private static final Accion ACCION_POR_DEFECTO = Accion.ATACAR;
    private static final long INTERVALO_AUTOGUARDADO_MS = 30_000;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final PartidaRepositorio repositorio = new PartidaRepositorio();
    private final LogCombateArchivo log = new LogCombateArchivo();

    // perfil y gremio se reemplazan al cargar una partida, por eso no son final
    private volatile PerfilJugador perfil = new PerfilJugador();
    private volatile Gremio gremio = new Gremio(NOMBRE_GREMIO);

    private volatile String ultimoGuardado = "";
    /** El daemon no escribe a disco hasta que haya algo que guardar: asi no pisa tu partida con una vacia. */
    private volatile boolean hayProgreso;

    public MotorJuego(Consola consola) {
        this.consola = consola;
        new AutoGuardadoDaemon(INTERVALO_AUTOGUARDADO_MS, this::generarGuardado).iniciar();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        enemigo.setSalidaEfectos(registro::agregar);
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo;
        try {
            huyo = combatir(jugador, enemigo);
        } finally {
            jugador.cancelarEfectos();
            enemigo.cancelarEfectos();
        }

        cerrarCombate(jugador, enemigo, huyo);
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        synchronized (this) {
            perfil.desbloquearLogro("Campeon del Torneo");
        }
        guardarProgreso();
    }

    public synchronized void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        jugador.setSalidaEfectos(registro::agregar);
        entregarKitInicial(jugador);
        Gremio gremioActual;
        synchronized (this) {
            gremioActual = gremio;
            jugador.unirseAGremio(gremioActual);
        }
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremioActual.getNombre(), gremioActual.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (!jugador.estaVivo()) {   // un DoT pudo matarlo mientras decidia
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo() && jugador.estaVivo());
        return false;
    }

    /** Pide la accion con limite de tiempo; si expira, ejecuta la accion por defecto. */
    private Accion pedirAccion() {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");
        OptionalInt opcion = consola.leerEnteroConLimite(
                "Accion (" + SEGUNDOS_TURNO + "s): ", 0, 4, SEGUNDOS_TURNO);

        if (opcion.isEmpty()) {
            System.out.println();
            registro.agregar("Tiempo agotado: se ejecuta %s automaticamente.", ACCION_POR_DEFECTO);
            return ACCION_POR_DEFECTO;
        }
        return switch (opcion.getAsInt()) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d ticks)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate (synchronized: el daemon lee perfil y bestiario desde otro hilo)

    private synchronized void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    /** Guardado manual (al terminar una pelea o un torneo): memoria, partida.dat y log de combate. */
    public void guardarProgreso() {
        hayProgreso = true;
        generarGuardado();
        log.guardar(registro.getTexto(), true);   // true: agrega, no borra combates anteriores
        System.out.println("[Progreso guardado en memoria y en disco]");
    }

    /**
     * Lo usan el guardado manual y el daemon. Sincronizado: no se solapan entre si ni con los cambios de
     * perfil y gremio. Orden de candados: this y luego GestorArchivos.LOCK (siempre el mismo, sin deadlock).
     */
    private synchronized void generarGuardado() {
        if (!hayProgreso) {
            return;
        }
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
        repositorio.guardar(new EstadoPartida(perfil, gremio, bestiario.getDerrotados()));
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }

    // ---- archivos: historial, cargar y borrar

    public void verHistorial() {
        System.out.println(GestorArchivos.describir(GestorArchivos.LOG));
        List<String> lineas = log.leer();
        if (lineas.isEmpty()) {
            return;
        }
        int desde = Math.max(0, lineas.size() - 60);
        System.out.println("--- Historial (ultimas " + (lineas.size() - desde) + " lineas) ---");
        for (int i = desde; i < lineas.size(); i++) {
            System.out.println(lineas.get(i));
        }
    }

    public synchronized void cargarPartida() {
        Optional<EstadoPartida> cargada = repositorio.cargar();
        if (cargada.isEmpty()) {
            return;
        }
        EstadoPartida estado = cargada.get();
        perfil = estado.getPerfil();
        gremio = estado.getGremio();
        bestiario.cargarDerrotados(estado.getDerrotados());
        hayProgreso = true;
        System.out.printf("Partida cargada: %d victorias, %d derrotas, gremio %s (%d miembros).%n",
                perfil.getVictorias(), perfil.getDerrotas(), gremio.getNombre(), gremio.getMiembros().size());
    }

    /** Las preguntas se hacen SIN el lock tomado, para no bloquear al daemon mientras el jugador escribe. */
    public void borrarArchivos() {
        System.out.println(GestorArchivos.describir(GestorArchivos.PARTIDA));
        System.out.println(GestorArchivos.describir(GestorArchivos.LOG));
        int opcion = consola.leerEntero("Borrar: 1. Partida guardada  2. Historial  0. Cancelar: ", 0, 2);
        if (opcion == 0) {
            return;
        }
        String respuesta = consola.leerLinea("Seguro que quieres borrarlo? (s/n): ");
        if (!respuesta.equalsIgnoreCase("s")) {
            System.out.println("Cancelado.");
            return;
        }
        if (opcion == 1) {
            borrarPartida();
        } else if (GestorArchivos.borrarConFile(GestorArchivos.LOG)) {
            System.out.println("Historial borrado.");
        }
    }

    private synchronized void borrarPartida() {
        // se pausa el daemon (hayProgreso=false) y se reinicia la memoria: si no, recrearia el archivo
        hayProgreso = false;
        if (GestorArchivos.borrarConNio(GestorArchivos.PARTIDA)) {
            perfil = new PerfilJugador();
            gremio = new Gremio(NOMBRE_GREMIO);
            bestiario.cargarDerrotados(Map.of());
            System.out.println("Partida borrada.");
        } else {
            hayProgreso = perfil.getVictorias() + perfil.getDerrotas() > 0;
        }
    }
}