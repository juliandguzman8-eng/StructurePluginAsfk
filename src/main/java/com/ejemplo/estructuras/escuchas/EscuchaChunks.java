package com.ejemplo.estructuras.escuchas;

import com.ejemplo.estructuras.EstructurasPlugin;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

public class EscuchaChunks implements Listener {

    private final EstructurasPlugin plugin;

    public EscuchaChunks(EstructurasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alPoblarChunk(ChunkPopulateEvent evento) {
        if (!plugin.getConfig().getBoolean("general.generacion-automatica", true)) {
            return;
        }
        Chunk chunk = evento.getChunk();
        // No se puede modificar el mundo durante la generacion: lo hacemos al tick siguiente.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (chunk.isLoaded()) {
                plugin.colocador().intentarEnChunk(chunk);
            }
        });
    }
}
