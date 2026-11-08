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
import org.bukkit.Material;
import org.bukkit.Tag;
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
 
    /** Intenta generar como mucho UNA estructura en un chunk. */
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
 
            String motivo = intentarColocar(def, mundo, x, z, false);
            if (motivo == null) {
                return true;
            }
            if (plugin.debug()) {
                plugin.getLogger().info("[" + def.id + "] descartado en " + x + "," + z + ": " + motivo);
            }
        }
        return false;
    }
 
    /** Version publica: true si se coloco. */
    public boolean colocar(EstructuraDef def, World mundo, int x, int z, boolean forzado) {
        String motivo = intentarColocar(def, mundo, x, z, forzado);
        if (motivo != null && plugin.debug()) {
            plugin.getLogger().info("[" + def.id + "] no colocada: " + motivo);
        }
        return motivo == null;
    }
 
    /** @return null si se coloco, o el motivo del descarte. */
    private String intentarColocar(EstructuraDef def, World mundo, int x, int z, boolean forzado) {
        Clipboard clipboard = plugin.estructuras().clipboard(def);
        if (clipboard == null) {
            return "no hay schematic cargado";
        }
 
        int ySuelo = alturaSuelo(mundo, x, z);
        if (ySuelo == Integer.MIN_VALUE) {
            return "no se encontro suelo solido";
        }
 
        BlockVector3 dimensiones = clipboard.getDimensions();
        int radio = Math.max(dimensiones.x(), dimensiones.z()) / 2 + 1;
 
        if (!forzado) {
            if (ySuelo < def.yMinimo || ySuelo > def.yMaximo) {
                return "altura " + ySuelo + " fuera del rango " + def.yMinimo + "-" + def.yMaximo;
            }
            Material enSuelo = mundo.getBlockAt(x, ySuelo, z).getType();
            if (def.evitarLiquidos && (enSuelo == Material.WATER || enSuelo == Material.LAVA)) {
                return "hay liquido en la superficie";
            }
            String bioma = nombreBioma(mundo, x, ySuelo, z);
            if (!def.permiteBioma(bioma)) {
                return "bioma no permitido (" + bioma + ")";
            }
            if (plugin.registro().hayCerca(mundo, x, z, def.distanciaMinima, def.id)) {
                return "ya hay otra '" + def.id + "' a menos de " + def.distanciaMinima + " bloques";
            }
            int global = plugin.getConfig().getInt("general.distancia-minima-global", 200);
            if (plugin.registro().hayCerca(mundo, x, z, global, null)) {
                return "hay otra estructura a menos de " + global + " bloques";
            }
            if (plugin.getConfig().getBoolean("general.exigir-chunks-generados", false)
                    && !chunksGenerados(mundo, x, z, radio)) {
                return "hay chunks vecinos sin generar";
            }
 
            int[] alturas = alturasAlrededor(mundo, x, z, radio);
            int desnivel = alturas[1] - alturas[0];
            if (desnivel > def.desnivelMaximo) {
                return "terreno irregular (desnivel " + desnivel + " > " + def.desnivelMaximo + ")";
            }
            if (def.evitarLiquidos && hayLiquidoAlrededor(mundo, x, z, radio)) {
                return "hay liquido en la huella de la estructura";
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
            return "error de WorldEdit al pegar";
        }
 
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
 
        plugin.registro().registrar(def.id, new Location(mundo, x, yBase, z));
 
        plugin.getLogger().info("Estructura '" + def.id + "' colocada en "
                + mundo.getName() + " " + x + "," + yBase + "," + z + " (rotacion " + grados + ")");
        return null;
    }
 
    /**
     * Altura del suelo real: baja ignorando arboles, hojas, nieve y plantas,
     * para que un roble al lado no cuente como una montana de 6 bloques.
     */
    private int alturaSuelo(World mundo, int x, int z) {
        int y = mundo.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int minimo = mundo.getMinHeight();
        while (y > minimo && esIgnorable(mundo.getBlockAt(x, y, z).getType())) {
            y--;
        }
        return y <= minimo ? Integer.MIN_VALUE : y;
    }
 
    /** Bloques que no cuentan como "suelo" al medir el terreno. */
    private boolean esIgnorable(Material material) {
        // Los liquidos SI cuentan, para poder detectarlos y rechazar el sitio.
        if (material == Material.WATER || material == Material.LAVA) {
            return false;
        }
        if (material.isAir()) {
            return true;
        }
        if (Tag.LOGS.isTagged(material) || Tag.LEAVES.isTagged(material)
                || Tag.SAPLINGS.isTagged(material) || Tag.FLOWERS.isTagged(material)) {
            return true;
        }
        switch (material) {
            case SNOW, SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN, DEAD_BUSH, VINE,
                 CACTUS, BAMBOO, SUGAR_CANE, MUSHROOM_STEM, BROWN_MUSHROOM_BLOCK,
                 RED_MUSHROOM_BLOCK, PUMPKIN, MELON, SWEET_BERRY_BUSH, MOSS_CARPET -> {
                return true;
            }
            default -> {
                return !material.isSolid();
            }
        }
    }
 
    /** @return {alturaMinima, alturaMaxima} en la huella de la estructura. */
    private int[] alturasAlrededor(World mundo, int x, int z, int radio) {
        int[][] puntos = {
                {0, 0},
                {-radio, -radio}, {radio, -radio}, {-radio, radio}, {radio, radio},
                {-radio, 0}, {radio, 0}, {0, -radio}, {0, radio}
        };
        int minimo = Integer.MAX_VALUE;
        int maximo = Integer.MIN_VALUE;
        for (int[] punto : puntos) {
            int altura = alturaSuelo(mundo, x + punto[0], z + punto[1]);
            if (altura == Integer.MIN_VALUE) {
                continue;
            }
            minimo = Math.min(minimo, altura);
            maximo = Math.max(maximo, altura);
        }
        if (minimo == Integer.MAX_VALUE) {
            return new int[]{0, 0};
        }
        return new int[]{minimo, maximo};
    }
 
    private boolean hayLiquidoAlrededor(World mundo, int x, int z, int radio) {
        int[][] puntos = {
                {-radio, -radio}, {radio, -radio}, {-radio, radio}, {radio, radio},
                {-radio, 0}, {radio, 0}, {0, -radio}, {0, radio}
        };
        for (int[] punto : puntos) {
            int px = x + punto[0];
            int pz = z + punto[1];
            int altura = alturaSuelo(mundo, px, pz);
            if (altura == Integer.MIN_VALUE) {
                continue;
            }
            Material material = mundo.getBlockAt(px, altura, pz).getType();
            if (material == Material.WATER || material == Material.LAVA) {
                return true;
            }
        }
        return false;
    }
 
    private boolean chunksGenerados(World mundo, int x, int z, int radio) {
        for (int cx = (x - radio) >> 4; cx <= (x + radio) >> 4; cx++) {
            for (int cz = (z - radio) >> 4; cz <= (z + radio) >> 4; cz++) {
                if (!mundo.isChunkGenerated(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }
 
    private String nombreBioma(World mundo, int x, int y, int z) {
        try {
            return mundo.getBiome(x, y, z).getKey().getKey().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return String.valueOf(mundo.getBiome(x, y, z)).toLowerCase(Locale.ROOT);
        }
    }
 
    public Block sueloEn(Location donde) {
        int y = alturaSuelo(donde.getWorld(), donde.getBlockX(), donde.getBlockZ());
        return y == Integer.MIN_VALUE ? null : donde.getWorld().getBlockAt(donde.getBlockX(), y, donde.getBlockZ());
    }
}
 
