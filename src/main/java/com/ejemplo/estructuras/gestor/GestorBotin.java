package com.ejemplo.estructuras.gestor;

import com.ejemplo.estructuras.EstructurasPlugin;
import com.ejemplo.estructuras.modelo.Rangos;
import com.ejemplo.estructuras.modelo.TablaBotin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GestorBotin {

    private final EstructurasPlugin plugin;
    private final Map<String, TablaBotin> tablas = new HashMap<>();
    private final Random random = new Random();

    public GestorBotin(EstructurasPlugin plugin) {
        this.plugin = plugin;
    }

    public void cargar() {
        tablas.clear();
        File archivo = new File(plugin.getDataFolder(), "botin.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection raiz = yml.getConfigurationSection("tablas");
        if (raiz == null) {
            plugin.getLogger().warning("botin.yml no tiene la seccion 'tablas'.");
            return;
        }
        for (String id : raiz.getKeys(false)) {
            ConfigurationSection seccion = raiz.getConfigurationSection(id);
            if (seccion == null) {
                continue;
            }
            TablaBotin tabla = new TablaBotin(id, seccion);
            if (tabla.entradas.isEmpty()) {
                plugin.getLogger().warning("La tabla de botin '" + id + "' no tiene items validos.");
            }
            tablas.put(id.toLowerCase(), tabla);
        }
        plugin.getLogger().info("Tablas de botin cargadas: " + tablas.size());
    }

    public TablaBotin tabla(String id) {
        return id == null ? null : tablas.get(id.toLowerCase());
    }

    public List<String> ids() {
        return new ArrayList<>(tablas.keySet());
    }

    /** Rellena un inventario con una tabla de botin. */
    public void rellenar(Inventory inventario, String idTabla, boolean vaciarAntes) {
        TablaBotin tabla = tabla(idTabla);
        if (tabla == null) {
            plugin.getLogger().warning("Tabla de botin desconocida: " + idTabla);
            return;
        }
        if (vaciarAntes) {
            inventario.clear();
        }

        List<Integer> huecos = new ArrayList<>();
        for (int i = 0; i < inventario.getSize(); i++) {
            if (inventario.getItem(i) == null) {
                huecos.add(i);
            }
        }
        if (huecos.isEmpty()) {
            return;
        }
        Collections.shuffle(huecos, random);

        int tiradas = Rangos.aleatorio(random, tabla.tiradasMin, tabla.tiradasMax);
        int puestos = 0;

        for (int t = 0; t < tiradas && puestos < huecos.size(); t++) {
            TablaBotin.Entrada entrada = elegir(tabla);
            if (entrada == null) {
                continue;
            }
            if (entrada.probabilidad < 1.0 && random.nextDouble() > entrada.probabilidad) {
                continue;
            }
            ItemStack item = construir(entrada);
            if (item == null) {
                continue;
            }
            inventario.setItem(huecos.get(puestos), item);
            puestos++;
        }
    }

    private TablaBotin.Entrada elegir(TablaBotin tabla) {
        int tirada = random.nextInt(tabla.pesoTotal);
        int acumulado = 0;
        for (TablaBotin.Entrada entrada : tabla.entradas) {
            acumulado += entrada.peso;
            if (tirada < acumulado) {
                return entrada;
            }
        }
        return null;
    }

    private ItemStack construir(TablaBotin.Entrada entrada) {
        int cantidad = Rangos.aleatorio(random, entrada.cantidadMin, entrada.cantidadMax);
        if (cantidad <= 0) {
            return null;
        }

        ItemStack item;
        if (entrada.mythic != null) {
            item = plugin.mythic().item(entrada.mythic, cantidad);
            if (item == null) {
                return null;
            }
        } else {
            item = new ItemStack(entrada.material, Math.min(cantidad, entrada.material.getMaxStackSize()));
        }

        if (entrada.nombre != null || (entrada.lore != null && !entrada.lore.isEmpty())) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                MiniMessage mm = MiniMessage.miniMessage();
                if (entrada.nombre != null) {
                    meta.displayName(mm.deserialize(entrada.nombre));
                }
                if (entrada.lore != null && !entrada.lore.isEmpty()) {
                    List<Component> lore = new ArrayList<>();
                    for (String linea : entrada.lore) {
                        lore.add(mm.deserialize(linea));
                    }
                    meta.lore(lore);
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
