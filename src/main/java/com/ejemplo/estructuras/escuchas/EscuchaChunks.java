package com.ejemplo.estructuras.escuchas;
 
import com.ejemplo.estructuras.EstructurasPlugin;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
 
/**
 * Decide si un chunk se evalua para generar estructuras.
 *
 * Se escuchan los dos eventos porque ChunkPopulateEvent no llega de forma fiable
 * en Paper moderno con el generador vanilla. Para no hacer dos tiradas en el mismo
 * chunk, cada chunk ya evaluado queda marcado en su PersistentDataContainer, cosa
 * que ademas sobrevive a los reinicios.
 */
public class EscuchaChunks implements Listener {
 
    private final EstructurasPlugin plugin;
    private final NamespacedKey claveEvaluado;
 
    private long chunksEvaluados = 0L;
    private long estructurasColocadas = 0L;
 
    public EscuchaChunks(EstructurasPlugin plugin) {
        this.plugin = plugin;
        this.claveEvaluado = new NamespacedKey(plugin, "chunk_evaluado");
    }
 
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alPoblarChunk(ChunkPopulateEvent evento) {
        evaluar(evento.getChunk(), true);
    }
 
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alCargarChunk(ChunkLoadEvent evento) {
        evaluar(evento.getChunk(), evento.isNewChunk());
    }
 
    private void evaluar(Chunk chunk, boolean esNuevo) {
        if (!plugin.getConfig().getBoolean("general.generacion-automatica", true)) {
            return;
        }
 
        PersistentDataContainer datos = chunk.getPersistentDataContainer();
        if (datos.has(claveEvaluado, PersistentDataType.BYTE)) {
            return;
        }
 
        // Los chunks que ya existian solo se evaluan si se activa en la config.
        // Ojo: si lo activas, pueden aparecer estructuras en terreno ya explorado.
        if (!esNuevo && !plugin.getConfig().getBoolean("general.generar-en-chunks-existentes", false)) {
            return;
        }
 
        datos.set(claveEvaluado, PersistentDataType.BYTE, (byte) 1);
        chunksEvaluados++;
 
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!chunk.isLoaded()) {
                return;
            }
            if (plugin.colocador().intentarEnChunk(chunk)) {
                estructurasColocadas++;
            }
            if (plugin.debug() && chunksEvaluados % 100 == 0) {
                plugin.getLogger().info("Chunks evaluados: " + chunksEvaluados
                        + " | estructuras colocadas: " + estructurasColocadas);
            }
        });
    }
}
 

