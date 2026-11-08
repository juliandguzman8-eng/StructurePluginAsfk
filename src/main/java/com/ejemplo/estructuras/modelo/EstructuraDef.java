package com.ejemplo.estructuras.modelo;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Todo lo que define una estructura en estructuras.yml. */
public class EstructuraDef {

    public final String id;
    public final boolean activa;
    public final String archivo;
    public final double probabilidad;
    public final List<String> mundos;
    public final List<String> biomas;
    public final int yMinimo;
    public final int yMaximo;
    public final int offsetY;
    public final boolean anclaCentro;
    public final boolean rotacionAleatoria;
    public final boolean ignorarAire;
    public final boolean pegarEntidades;
    public final int distanciaMinima;
    public final int desnivelMaximo;
    public final boolean evitarLiquidos;

    public final String tablaCofres;
    public final boolean vaciarCofres;
    /** Bloque debajo del cofre -> tabla de botin */
    public final Map<Material, String> tablasPorBloque = new HashMap<>();
    /** Bloque debajo del cofre -> bloque por el que se reemplaza despues */
    public final Map<Material, Material> reemplazoPorBloque = new HashMap<>();

    public final Map<Material, MarcadorMob> marcadoresMobs = new HashMap<>();
    public final Map<Material, MarcadorSpawner> marcadoresSpawners = new HashMap<>();

    public EstructuraDef(String id, ConfigurationSection s) {
        this.id = id;
        this.activa = s.getBoolean("activa", true);
        this.archivo = s.getString("archivo", id + ".schem");
        this.probabilidad = s.getDouble("probabilidad", 0.0);
        this.mundos = new ArrayList<>(s.getStringList("mundos"));

        this.biomas = new ArrayList<>();
        for (String bioma : s.getStringList("biomas")) {
            this.biomas.add(bioma.toLowerCase(Locale.ROOT).replace("minecraft:", "").trim());
        }

        this.yMinimo = s.getInt("y-minimo", -64);
        this.yMaximo = s.getInt("y-maximo", 320);
        this.offsetY = s.getInt("offset-y", 0);
        this.anclaCentro = !"ORIGEN".equalsIgnoreCase(s.getString("ancla", "CENTRO"));
        this.rotacionAleatoria = s.getBoolean("rotacion-aleatoria", true);
        this.ignorarAire = s.getBoolean("ignorar-aire", true);
        this.pegarEntidades = s.getBoolean("pegar-entidades", true);
        this.distanciaMinima = s.getInt("distancia-minima", 300);
        this.desnivelMaximo = s.getInt("desnivel-maximo", 4);
        this.evitarLiquidos = s.getBoolean("evitar-liquidos", true);

        ConfigurationSection cofres = s.getConfigurationSection("cofres");
        this.tablaCofres = cofres == null ? null : cofres.getString("tabla", null);
        this.vaciarCofres = cofres == null || cofres.getBoolean("vaciar-antes", true);

        if (cofres != null) {
            ConfigurationSection porBloque = cofres.getConfigurationSection("tablas-por-bloque");
            if (porBloque != null) {
                for (String clave : porBloque.getKeys(false)) {
                    Material material = Material.matchMaterial(clave);
                    ConfigurationSection datos = porBloque.getConfigurationSection(clave);
                    if (material == null || datos == null) {
                        continue;
                    }
                    tablasPorBloque.put(material, datos.getString("tabla", "comun"));
                    Material reemplazo = Material.matchMaterial(datos.getString("reemplazo", "AIR"));
                    reemplazoPorBloque.put(material, reemplazo == null ? Material.AIR : reemplazo);
                }
            }
        }

        ConfigurationSection mobs = s.getConfigurationSection("marcadores-mobs");
        if (mobs != null) {
            for (String clave : mobs.getKeys(false)) {
                Material material = Material.matchMaterial(clave);
                ConfigurationSection datos = mobs.getConfigurationSection(clave);
                if (material != null && datos != null) {
                    marcadoresMobs.put(material, new MarcadorMob(datos));
                }
            }
        }

        ConfigurationSection spawners = s.getConfigurationSection("marcadores-spawners");
        if (spawners != null) {
            for (String clave : spawners.getKeys(false)) {
                Material material = Material.matchMaterial(clave);
                ConfigurationSection datos = spawners.getConfigurationSection(clave);
                if (material != null && datos != null) {
                    marcadoresSpawners.put(material, new MarcadorSpawner(datos));
                }
            }
        }
    }

    public boolean permiteMundo(String nombreMundo, List<String> mundosGlobales) {
        if (!mundos.isEmpty()) {
            return mundos.contains(nombreMundo);
        }
        return mundosGlobales.isEmpty() || mundosGlobales.contains(nombreMundo);
    }

    public boolean permiteBioma(String bioma) {
        return biomas.isEmpty() || biomas.contains(bioma);
    }
}
