package com.ejemplo.estructuras.escuchas;

import com.ejemplo.estructuras.EstructurasPlugin;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Cuando un spawner marcado por el plugin intenta generar su mob vanilla,
 * cancelamos ese spawn y ponemos el MythicMob correspondiente.
 */
public class EscuchaSpawners implements Listener {

    private final EstructurasPlugin plugin;

    public EscuchaSpawners(EstructurasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alGenerarSpawner(SpawnerSpawnEvent evento) {
        CreatureSpawner spawner = evento.getSpawner();
        if (spawner == null) {
            return;
        }
        PersistentDataContainer datos = spawner.getPersistentDataContainer();
        String mob = datos.get(plugin.claveMob(), PersistentDataType.STRING);
        if (mob == null || mob.isBlank()) {
            return;
        }
        Integer nivel = datos.get(plugin.claveNivel(), PersistentDataType.INTEGER);
        Location donde = evento.getLocation();

        evento.setCancelled(true);

        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.mythic().spawnear(mob, donde, nivel == null ? 1 : nivel));
    }
}
