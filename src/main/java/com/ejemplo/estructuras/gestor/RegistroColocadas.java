package com.ejemplo.estructuras.gestor;

import com.ejemplo.estructuras.EstructurasPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Guarda donde se ha colocado cada estructura para respetar distancias minimas. */
public class RegistroColocadas {

    public record Colocada(String id, int x, int y, int z) {
    }

    private final EstructurasPlugin plugin;
    private final File archivo;
    private final Map<String, List<Colocada>> porMundo = new HashMap<>();
    private boolean sucio = false;

    public RegistroColocadas(EstructurasPlugin plugin) {
        this.plugin = plugin;
        this.archivo = new File(plugin.getDataFolder(), "colocadas.yml");
    }

    public void cargar() {
        porMundo.clear();
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(archivo);
        for (String mundo : yml.getKeys(false)) {
            List<Colocada> lista = new ArrayList<>();
            for (String linea : yml.getStringList(mundo)) {
                String[] partes = linea.split(";");
                if (partes.length != 4) {
                    continue;
                }
                try {
                    lista.add(new Colocada(partes[0],
                            Integer.parseInt(partes[1]),
                            Integer.parseInt(partes[2]),
                            Integer.parseInt(partes[3])));
                } catch (NumberFormatException ignored) {
                }
            }
            porMundo.put(mundo, lista);
        }
    }

    public void guardar() {
        if (!sucio) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, List<Colocada>> entrada : porMundo.entrySet()) {
            List<String> lineas = new ArrayList<>();
            for (Colocada c : entrada.getValue()) {
                lineas.add(c.id() + ";" + c.x() + ";" + c.y() + ";" + c.z());
            }
            yml.set(entrada.getKey(), lineas);
        }
        try {
            yml.save(archivo);
            sucio = false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar colocadas.yml", e);
        }
    }

    public void registrar(String idEstructura, Location donde) {
        porMundo.computeIfAbsent(donde.getWorld().getName(), k -> new ArrayList<>())
                .add(new Colocada(idEstructura, donde.getBlockX(), donde.getBlockY(), donde.getBlockZ()));
        sucio = true;
    }

    /**
     * @param idEstructura si es null se comprueba contra cualquier estructura
     * @return true si ya hay algo demasiado cerca
     */
    public boolean hayCerca(World mundo, int x, int z, int distancia, String idEstructura) {
        if (distancia <= 0) {
            return false;
        }
        List<Colocada> lista = porMundo.get(mundo.getName());
        if (lista == null) {
            return false;
        }
        long limite = (long) distancia * distancia;
        for (Colocada c : lista) {
            if (idEstructura != null && !idEstructura.equals(c.id())) {
                continue;
            }
            long dx = (long) c.x() - x;
            long dz = (long) c.z() - z;
            if (dx * dx + dz * dz < limite) {
                return true;
            }
        }
        return false;
    }

    public int total() {
        int suma = 0;
        for (List<Colocada> lista : porMundo.values()) {
            suma += lista.size();
        }
        return suma;
    }

    public int totalDe(String idEstructura) {
        int suma = 0;
        for (List<Colocada> lista : porMundo.values()) {
            for (Colocada c : lista) {
                if (c.id().equals(idEstructura)) {
                    suma++;
                }
            }
        }
        return suma;
    }

    public void limpiar() {
        porMundo.clear();
        sucio = true;
        guardar();
    }
}
