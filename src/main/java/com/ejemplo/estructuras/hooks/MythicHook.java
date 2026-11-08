package com.ejemplo.estructuras.hooks;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Puente con MythicMobs. Si el plugin no esta instalado, todo devuelve null/false
 * sin romper nada (las clases de Mythic solo se tocan dentro de los metodos).
 */
public class MythicHook {

    private final Plugin plugin;
    private final boolean activo;

    public MythicHook(Plugin plugin) {
        this.plugin = plugin;
        this.activo = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        if (activo) {
            plugin.getLogger().info("MythicMobs detectado: items y mobs personalizados habilitados.");
        } else {
            plugin.getLogger().info("MythicMobs no esta instalado: se ignoraran los items y mobs mythic.");
        }
    }

    public boolean estaActivo() {
        return activo;
    }

    /** Devuelve el ItemStack de un item de MythicMobs, o null si no existe. */
    public ItemStack item(String nombre, int cantidad) {
        if (!activo || nombre == null || nombre.isBlank()) {
            return null;
        }
        try {
            ItemStack item = MythicBukkit.inst().getItemManager().getItemStack(nombre);
            if (item == null) {
                plugin.getLogger().warning("El item de MythicMobs '" + nombre + "' no existe.");
                return null;
            }
            item.setAmount(Math.max(1, cantidad));
            return item;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "No se pudo obtener el item mythic '" + nombre + "'", t);
            return null;
        }
    }

    /** Spawnea un MythicMob. Devuelve true si se pudo. */
    public boolean spawnear(String nombre, Location donde, int nivel) {
        if (!activo || nombre == null || nombre.isBlank()) {
            return false;
        }
        try {
            Entity entidad = MythicBukkit.inst().getAPIHelper().spawnMythicMob(nombre, donde, nivel);
            return entidad != null;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "No se pudo spawnear el mob mythic '" + nombre + "'", t);
            return false;
        }
    }

    /** Comprueba que un mob exista, para avisar al cargar la configuracion. */
    public boolean existeMob(String nombre) {
        if (!activo || nombre == null || nombre.isBlank()) {
            return false;
        }
        try {
            return MythicBukkit.inst().getMobManager().getMythicMob(nombre).isPresent();
        } catch (Throwable t) {
            return false;
        }
    }
}
