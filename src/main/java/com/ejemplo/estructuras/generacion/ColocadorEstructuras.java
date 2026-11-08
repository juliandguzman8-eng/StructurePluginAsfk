package com.ejemplo.estructuras.generacion;

import com.ejemplo.estructuras.EstructurasPlugin;
import com.ejemplo.estructuras.modelo.EstructuraDef;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;

public class ColocadorEstructuras {

    private final EstructurasPlugin plugin;
    private final Random random = new Random();

    public ColocadorEstructuras(EstructurasPlugin plugin) {
        this.plugin = plugin;
    }

    /** Intenta generar como mucho UNA estructura en un chunk recien creado. */
    public boolean intentarEnChunk(Chunk chunk) {
        World mundo = chunk.getWorld();
        List<String> mundosGlobales = plugin.getConfig().getStringList("general.mundos-permitidos");

        for (EstructuraDef def : plugin.estructuras().barajadas()) {
            if (!def.activa || def.probabilidad <= 0.0) {
                continue;
            }
            if (!def.permiteMundo(mundo.getName(), mundosGlobales)) {
                continue;
            }
            if (random.nextDouble() > def.probabilidad) {
                continue;
            }
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            if (colocar(def, mundo, x, z, false)) {
                return true;
            }
        }
        return false;
    }

    /** Coloca una estructura en unas coordenadas. Si forzado = true se saltan los filtros. */
    public boolean colocar(EstructuraDef def, World mundo, int x, int z, boolean forzado) {
        Clipboard clipboard = plugin.estructuras().clipboard(def);
        if (clipboard == null) {
            return false;
        }

        int ySuelo = alturaSolida(mundo, x, z);
        if (ySuelo == Integer.MIN_VALUE) {
            return false;
        }

        BlockVector3 dimensiones = clipboard.getDimensions();
        int radio = Math.max(dimensiones.x(), dimensiones.z()) / 2 + 1;

        if (!forzado) {
            if (ySuelo < def.yMinimo || ySuelo > def.yMaximo) {
                return false;
            }
            if (def.evitarLiquidos && mundo.getBlockAt(x, ySuelo, z).isLiquid()) {
                return false;
            }
            if (!def.permiteBioma(nombreBioma(mundo, x, ySuelo, z))) {
                return false;
            }
            if (plugin.registro().hayCerca(mundo, x, z, def.distanciaMinima, def.id)) {
                return false;
            }
            int global = plugin.getConfig().getInt("general.distancia-minima-global", 200);
            if (plugin.registro().hayCerca(mundo, x, z, global, null)) {
                return false;
            }
            if (!chunksGenerados(mundo, x, z, radio)) {
                return false;
            }
            if (!terrenoValido(mundo, x, z, radio, ySuelo, def)) {
                return false;
            }
        }

        int yBase = ySuelo + 1 + def.offsetY;

        AffineTransform transform = new AffineTransform();
        int grados = def.rotacionAleatoria ? random.nextInt(4) * 90 : 0;
        if (grados != 0) {
            transform = transform.rotateY(grados);
        }

        ClipboardHolder holder = new ClipboardHolder(clipboard);
        holder.setTransform(transform);

        BlockVector3 destino = BlockVector3.at(x, yBase, z);
        if (def.anclaCentro) {
            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();
            Vector3 centroBase = Vector3.at(
                    (min.x() + max.x()) / 2.0,
                    min.y(),
                    (min.z() + max.z()) / 2.0);
            Vector3 desplazamiento = centroBase.subtract(clipboard.getOrigin().toVector3());
            Vector3 rotado = transform.apply(desplazamiento);
            destino = destino.subtract(BlockVector3.at(
                    (int) Math.round(rotado.x()),
                    (int) Math.round(rotado.y()),
                    (int) Math.round(rotado.z())));
        }

        com.sk89q.worldedit.world.World mundoWE = BukkitAdapter.adapt(mundo);
        try (EditSession sesion = WorldEdit.getInstance().newEditSessionBuilder()
                .world(mundoWE)
                .maxBlocks(-1)
                .build()) {

            Operation operacion = holder.createPaste(sesion)
                    .to(destino)
                    .ignoreAirBlocks(def.ignorarAire)
                    .copyEntities(def.pegarEntidades)
                    .build();
            Operations.complete(operacion);

        } catch (WorldEditException e) {
            plugin.getLogger().log(Level.WARNING, "Error pegando la estructura " + def.id, e);
            return false;
        }

        // Caja que ocupa la estructura ya pegada, para buscar cofres y marcadores.
        Vector3 relMin = clipboard.getMinimumPoint().subtract(clipboard.getOrigin()).toVector3();
        Vector3 relMax = clipboard.getMaximumPoint().subtract(clipboard.getOrigin()).toVector3();
        Vector3 a = transform.apply(relMin);
        Vector3 b = transform.apply(relMax);

        int xMin = destino.x() + (int) Math.floor(Math.min(a.x(), b.x()));
        int xMax = destino.x() + (int) Math.ceil(Math.max(a.x(), b.x()));
        int yMin = destino.y() + (int) Math.floor(Math.min(a.y(), b.y()));
        int yMax = destino.y() + (int) Math.ceil(Math.max(a.y(), b.y()));
        int zMin = destino.z() + (int) Math.floor(Math.min(a.z(), b.z()));
        int zMax = destino.z() + (int) Math.ceil(Math.max(a.z(), b.z()));

        new ProcesadorRegion(plugin, def, mundo, xMin, yMin, zMin, xMax, yMax, zMax)
                .runTaskTimer(plugin, 1L, 1L);

        Location centro = new Location(mundo, x, yBase, z);
        plugin.registro().registrar(def.id, centro);

        if (plugin.debug()) {
            plugin.getLogger().info("Estructura '" + def.id + "' colocada en "
                    + mundo.getName() + " " + x + "," + yBase + "," + z + " (rotacion " + grados + ")");
        }
        return true;
    }

    /** Altura del bloque solido mas alto, o MIN_VALUE si no hay. */
    private int alturaSolida(World mundo, int x, int z) {
        int y = mundo.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int minimo = mundo.getMinHeight();
        while (y > minimo && mundo.getBlockAt(x, y, z).getType().isAir()) {
            y--;
        }
        if (y <= minimo) {
            return Integer.MIN_VALUE;
        }
        return y;
    }

    private boolean chunksGenerados(World mundo, int x, int z, int radio) {
        int cxMin = (x - radio) >> 4;
        int cxMax = (x + radio) >> 4;
        int czMin = (z - radio) >> 4;
        int czMax = (z + radio) >> 4;
        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                if (!mundo.isChunkGenerated(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Comprueba desnivel y liquidos en las esquinas de la huella. */
    private boolean terrenoValido(World mundo, int x, int z, int radio, int yCentro, EstructuraDef def) {
        int[][] puntos = {
                {-radio, -radio}, {radio, -radio}, {-radio, radio}, {radio, radio},
                {-radio, 0}, {radio, 0}, {0, -radio}, {0, radio}
        };
        int minimo = yCentro;
        int maximo = yCentro;
        for (int[] punto : puntos) {
            int px = x + punto[0];
            int pz = z + punto[1];
            int altura = alturaSolida(mundo, px, pz);
            if (altura == Integer.MIN_VALUE) {
                return false;
            }
            if (def.evitarLiquidos && mundo.getBlockAt(px, altura, pz).isLiquid()) {
                return false;
            }
            minimo = Math.min(minimo, altura);
            maximo = Math.max(maximo, altura);
        }
        return (maximo - minimo) <= def.desnivelMaximo;
    }

    private String nombreBioma(World mundo, int x, int y, int z) {
        try {
            return mundo.getBiome(x, y, z).getKey().getKey().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return String.valueOf(mundo.getBiome(x, y, z)).toLowerCase(Locale.ROOT);
        }
    }

    /** Devuelve el bloque de suelo bajo una ubicacion (lo usa el comando /estructuras aqui). */
    public Block sueloEn(Location donde) {
        int y = alturaSolida(donde.getWorld(), donde.getBlockX(), donde.getBlockZ());
        if (y == Integer.MIN_VALUE) {
            return null;
        }
        return donde.getWorld().getBlockAt(donde.getBlockX(), y, donde.getBlockZ());
    }
}
