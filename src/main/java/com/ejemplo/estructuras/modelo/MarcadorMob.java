package com.ejemplo.estructuras.modelo;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Un bloque del schematic que se convierte en uno o varios mobs de MythicMobs. */
public class MarcadorMob {

    public final String mob;
    public final int nivel;
    public final int cantidadMin;
    public final int cantidadMax;
    public final double probabilidad;
    public final Material reemplazo;

    public MarcadorMob(ConfigurationSection s) {
        this.mob = s.getString("mob", "");
        this.nivel = s.getInt("nivel", 1);

        int[] rango = Rangos.leer(s.get("cantidad"), 1, 1);
        this.cantidadMin = rango[0];
        this.cantidadMax = rango[1];

        this.probabilidad = s.getDouble("probabilidad", 1.0);

        Material mat = Material.matchMaterial(s.getString("reemplazo", "AIR"));
        this.reemplazo = mat == null ? Material.AIR : mat;
    }
}
