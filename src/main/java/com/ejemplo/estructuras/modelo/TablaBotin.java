package com.ejemplo.estructuras.modelo;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/** Una tabla de botin de botin.yml. */
public class TablaBotin {

    public static class Entrada {
        public final Material material;   // null si es un item de MythicMobs
        public final String mythic;       // null si es un item vanilla
        public final int cantidadMin;
        public final int cantidadMax;
        public final int peso;
        public final double probabilidad;
        public final String nombre;       // MiniMessage, opcional
        public final List<String> lore;   // MiniMessage, opcional

        public Entrada(ConfigurationSection s) {
            String mat = s.getString("material", null);
            this.material = mat == null ? null : Material.matchMaterial(mat);
            this.mythic = s.getString("mythic", null);

            int[] rango = Rangos.leer(s.get("cantidad"), 1, 1);
            this.cantidadMin = rango[0];
            this.cantidadMax = rango[1];

            this.peso = Math.max(1, s.getInt("peso", 1));
            this.probabilidad = s.getDouble("probabilidad", 1.0);
            this.nombre = s.getString("nombre", null);
            this.lore = s.getStringList("lore");
        }

        public boolean esValida() {
            return material != null || (mythic != null && !mythic.isBlank());
        }
    }

    public final String id;
    public final int tiradasMin;
    public final int tiradasMax;
    public final List<Entrada> entradas = new ArrayList<>();
    public final int pesoTotal;

    public TablaBotin(String id, ConfigurationSection s) {
        this.id = id;

        int[] tiradas = Rangos.leer(s.get("tiradas"), 1, 3);
        this.tiradasMin = tiradas[0];
        this.tiradasMax = tiradas[1];

        int suma = 0;
        List<?> lista = s.getList("items");
        if (lista != null) {
            for (int i = 0; i < lista.size(); i++) {
                ConfigurationSection datos = s.getConfigurationSection("items." + i);
                if (datos == null) {
                    // Las listas de mapas no son secciones: las convertimos a mano.
                    Object bruto = lista.get(i);
                    if (bruto instanceof java.util.Map<?, ?> mapa) {
                        org.bukkit.configuration.MemoryConfiguration temporal =
                                new org.bukkit.configuration.MemoryConfiguration();
                        for (java.util.Map.Entry<?, ?> entrada : mapa.entrySet()) {
                            temporal.set(String.valueOf(entrada.getKey()), entrada.getValue());
                        }
                        datos = temporal;
                    }
                }
                if (datos == null) {
                    continue;
                }
                Entrada entrada = new Entrada(datos);
                if (entrada.esValida()) {
                    entradas.add(entrada);
                    suma += entrada.peso;
                }
            }
        }
        this.pesoTotal = Math.max(1, suma);
    }
}
