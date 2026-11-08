package com.ejemplo.estructuras.modelo;

import org.bukkit.configuration.ConfigurationSection;

/** Un bloque del schematic que se convierte en un spawner de MythicMobs. */
public class MarcadorSpawner {

    public final String mob;
    public final int nivel;
    public final double probabilidad;
    public final int delayMinimo;
    public final int delayMaximo;
    public final int cantidadPorTanda;
    public final int rangoJugador;
    public final int maximoCercanos;
    public final int rangoSpawn;

    public MarcadorSpawner(ConfigurationSection s) {
        this.mob = s.getString("mob", "");
        this.nivel = s.getInt("nivel", 1);
        this.probabilidad = s.getDouble("probabilidad", 1.0);
        this.delayMinimo = s.getInt("delay-minimo", 300);
        this.delayMaximo = s.getInt("delay-maximo", 700);
        this.cantidadPorTanda = s.getInt("cantidad-por-tanda", 2);
        this.rangoJugador = s.getInt("rango-jugador", 16);
        this.maximoCercanos = s.getInt("maximo-cercanos", 6);
        this.rangoSpawn = s.getInt("rango-spawn", 4);
    }
}
