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
 * No se puede confiar en ChunkLoadEvent#isNewChunk(), que en Paper moderno
 * practicamente nunca devuelve true, ni en ChunkPopulateEvent, que no llega
 * con el generador vanilla. Asi que el criterio es el tiempo habitado del
 * chunk: cuanto han estado los jugadores cerca de el. Un chunk recien generado
 * vale 0, y uno donde alguien ha construido vale miles, asi que sirve para
 * generar solo en terreno virgen sin pisar construcciones.
 *
 * Cada chunk evaluado queda marcado en su PersistentDataContainer para no
 * repetir la tirada, y ese marcador sobrevive a los reinicios.
 */
public class EscuchaChunks implements Listener {
 
    private final EstructurasPlugin plugin;
    private final NamespacedKey claveEvaluado;
 
    private long chunksEvaluados = 0L;
    private long chunksSaltados = 0L;
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
 
    private void evaluar(Chunk chunk, boolean marcadoComoNuevo) {
        if (!plugin.getConfig().getBoolean("general.generacion-automatica", true)) {
            return;
        }
 
        PersistentDataContainer datos = chunk.getPersistentDataContainer();
        if (datos.has(claveEvaluado, PersistentDataType.BYTE)) {
            return;
        }
 
        if (!plugin.getConfig().getBoolean("general.generar-en-chunks-existentes", false)) {
            // Tiempo que los jugadores han pasado cerca de este chunk, en ticks.
            // 0 = nadie ha estado nunca ahi, o sea terreno virgen.
            long habitado = chunk.getInhabitedTime();
            long umbral = plugin.getConfig().getLong("general.ticks-habitado-maximo", 0L);
            if (!marcadoComoNuevo && habitado > umbral) {
                chunksSaltados++;
                return;
            }
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
                        + " | saltados por habitados: " + chunksSaltados
                        + " | estructuras colocadas: " + estructurasColocadas);
            }
        });
    }
}
 
