package com.ejemplo.estructuras.gestor;

import com.ejemplo.estructuras.EstructurasPlugin;
import com.ejemplo.estructuras.modelo.EstructuraDef;
import com.ejemplo.estructuras.modelo.MarcadorMob;
import com.ejemplo.estructuras.modelo.MarcadorSpawner;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class GestorEstructuras {

    private final EstructurasPlugin plugin;
    private final Map<String, EstructuraDef> estructuras = new LinkedHashMap<>();
    private final Map<String, Clipboard> clipboards = new LinkedHashMap<>();

    public GestorEstructuras(EstructurasPlugin plugin) {
        this.plugin = plugin;
    }

    public void cargar() {
        estructuras.clear();
        clipboards.clear();

        File archivo = new File(plugin.getDataFolder(), "estructuras.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection raiz = yml.getConfigurationSection("estructuras");
        if (raiz == null) {
            plugin.getLogger().warning("estructuras.yml no tiene la seccion 'estructuras'.");
            return;
        }

        for (String id : raiz.getKeys(false)) {
            ConfigurationSection seccion = raiz.getConfigurationSection(id);
            if (seccion == null) {
                continue;
            }
            EstructuraDef def = new EstructuraDef(id, seccion);

            Clipboard clipboard = cargarClipboard(def.archivo);
            if (clipboard == null) {
                plugin.getLogger().warning("Estructura '" + id + "' omitida: no se pudo leer "
                        + def.archivo + " en la carpeta schematics.");
                continue;
            }

            long volumen = (long) clipboard.getRegion().getVolume();
            long maximo = plugin.getConfig().getLong("general.volumen-maximo", 1500000L);
            if (volumen > maximo) {
                plugin.getLogger().warning("Estructura '" + id + "' omitida: volumen " + volumen
                        + " supera el maximo configurado (" + maximo + ").");
                continue;
            }

            avisarMobsInexistentes(def);
            estructuras.put(id, def);
        }

        plugin.getLogger().info("Estructuras cargadas: " + estructuras.size());
    }

    private void avisarMobsInexistentes(EstructuraDef def) {
        if (!plugin.mythic().estaActivo()) {
            return;
        }
        for (MarcadorMob marcador : def.marcadoresMobs.values()) {
            if (!plugin.mythic().existeMob(marcador.mob)) {
                plugin.getLogger().warning("[" + def.id + "] El mob mythic '" + marcador.mob + "' no existe.");
            }
        }
        for (MarcadorSpawner marcador : def.marcadoresSpawners.values()) {
            if (!plugin.mythic().existeMob(marcador.mob)) {
                plugin.getLogger().warning("[" + def.id + "] El mob mythic '" + marcador.mob + "' no existe.");
            }
        }
    }

    private Clipboard cargarClipboard(String nombreArchivo) {
        Clipboard enCache = clipboards.get(nombreArchivo);
        if (enCache != null) {
            return enCache;
        }
        File archivo = new File(plugin.carpetaSchematics(), nombreArchivo);
        if (!archivo.exists()) {
            return null;
        }
        ClipboardFormat formato = ClipboardFormats.findByFile(archivo);
        if (formato == null) {
            plugin.getLogger().warning("Formato de schematic desconocido: " + nombreArchivo);
            return null;
        }
        try (FileInputStream entrada = new FileInputStream(archivo);
             ClipboardReader lector = formato.getReader(entrada)) {
            Clipboard clipboard = lector.read();
            clipboards.put(nombreArchivo, clipboard);
            return clipboard;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error leyendo el schematic " + nombreArchivo, e);
            return null;
        }
    }

    public Clipboard clipboard(EstructuraDef def) {
        return clipboards.get(def.archivo);
    }

    public EstructuraDef estructura(String id) {
        return estructuras.get(id);
    }

    public Collection<EstructuraDef> todas() {
        return estructuras.values();
    }

    public List<String> ids() {
        return new ArrayList<>(estructuras.keySet());
    }

    /** Lista barajada, para que no gane siempre la primera estructura del archivo. */
    public List<EstructuraDef> barajadas() {
        List<EstructuraDef> lista = new ArrayList<>(estructuras.values());
        Collections.shuffle(lista);
        return lista;
    }
}
