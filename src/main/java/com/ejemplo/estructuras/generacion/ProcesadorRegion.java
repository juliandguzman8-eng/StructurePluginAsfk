package com.ejemplo.estructuras.generacion;

import com.ejemplo.estructuras.EstructurasPlugin;
import com.ejemplo.estructuras.modelo.EstructuraDef;
import com.ejemplo.estructuras.modelo.MarcadorMob;
import com.ejemplo.estructuras.modelo.MarcadorSpawner;
import com.ejemplo.estructuras.modelo.Rangos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Recorre la caja de la estructura recien pegada, por tandas para no causar lag,
 * y convierte marcadores en mobs/spawners y rellena los contenedores con botin.
 */
public class ProcesadorRegion extends BukkitRunnable {

    private final EstructurasPlugin plugin;
    private final EstructuraDef def;
    private final World mundo;
    private final int xMin, yMin, zMin, xMax, yMax, zMax;
    private final int porTick;
    private final Random random = new Random();

    private int x, y, z;
    private int cofres = 0;
    private int mobs = 0;
    private int spawners = 0;

    public ProcesadorRegion(EstructurasPlugin plugin, EstructuraDef def, World mundo,
                            int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
        this.plugin = plugin;
        this.def = def;
        this.mundo = mundo;
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
        this.yMin = Math.max(yMin, mundo.getMinHeight());
        this.yMax = Math.min(yMax, mundo.getMaxHeight() - 1);
        this.porTick = Math.max(256, plugin.getConfig().getInt("general.bloques-por-tick", 6000));
        this.x = this.xMin;
        this.y = this.yMin;
        this.z = this.zMin;
    }

    @Override
    public void run() {
        int procesados = 0;
        while (procesados < porTick) {
            if (y > yMax) {
                terminar();
                return;
            }
            if (mundo.isChunkLoaded(x >> 4, z >> 4)) {
                procesar(x, y, z);
            }
            avanzar();
            procesados++;
        }
    }

    private void avanzar() {
        x++;
        if (x > xMax) {
            x = xMin;
            z++;
            if (z > zMax) {
                z = zMin;
                y++;
            }
        }
    }

    private void terminar() {
        cancel();
        if (plugin.debug()) {
            plugin.getLogger().info("[" + def.id + "] procesado: " + cofres + " contenedores, "
                    + mobs + " mobs, " + spawners + " spawners.");
        }
    }

    private void procesar(int bx, int by, int bz) {
        Block bloque = mundo.getBlockAt(bx, by, bz);
        Material material = bloque.getType();
        if (material.isAir()) {
            return;
        }

        MarcadorMob marcadorMob = def.marcadoresMobs.get(material);
        if (marcadorMob != null) {
            bloque.setType(marcadorMob.reemplazo, false);
            if (random.nextDouble() <= marcadorMob.probabilidad) {
                int cantidad = Rangos.aleatorio(random, marcadorMob.cantidadMin, marcadorMob.cantidadMax);
                Location donde = new Location(mundo, bx + 0.5, by, bz + 0.5);
                for (int i = 0; i < cantidad; i++) {
                    if (plugin.mythic().spawnear(marcadorMob.mob, donde, marcadorMob.nivel)) {
                        mobs++;
                    }
                }
            }
            return;
        }

        MarcadorSpawner marcadorSpawner = def.marcadoresSpawners.get(material);
        if (marcadorSpawner != null) {
            if (random.nextDouble() <= marcadorSpawner.probabilidad) {
                colocarSpawner(bloque, marcadorSpawner);
                spawners++;
            } else {
                bloque.setType(Material.AIR, false);
            }
            return;
        }

        if (esContenedor(material)) {
            rellenarContenedor(bloque, bx, by, bz);
        }
    }

    private boolean esContenedor(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.BARREL
                || Tag.SHULKER_BOXES.isTagged(material);
    }

    private void rellenarContenedor(Block bloque, int bx, int by, int bz) {
        String tabla = def.tablaCofres;

        // Tabla distinta segun el bloque marcador que haya debajo
        if (by - 1 >= mundo.getMinHeight() && !def.tablasPorBloque.isEmpty()) {
            Block abajo = mundo.getBlockAt(bx, by - 1, bz);
            String especial = def.tablasPorBloque.get(abajo.getType());
            if (especial != null) {
                tabla = especial;
                Material reemplazo = def.reemplazoPorBloque.getOrDefault(abajo.getType(), Material.AIR);
                abajo.setType(reemplazo, false);
            }
        }

        if (tabla == null) {
            return;
        }

        BlockState estado = bloque.getState(false);
        if (!(estado instanceof Container contenedor)) {
            return;
        }
        // En cofres dobles usamos solo la mitad de este bloque para no rellenar dos veces.
        Inventory inventario = (contenedor instanceof Chest cofre)
                ? cofre.getBlockInventory()
                : contenedor.getInventory();

        plugin.botin().rellenar(inventario, tabla, def.vaciarCofres);
        cofres++;
    }

    private void colocarSpawner(Block bloque, MarcadorSpawner marcador) {
        bloque.setType(Material.SPAWNER, false);
        BlockState estado = bloque.getState(false);
        if (!(estado instanceof CreatureSpawner spawner)) {
            return;
        }
        // Tipo base: el spawn real lo sustituye EscuchaSpawners por el MythicMob.
        spawner.setSpawnedType(EntityType.ZOMBIE);
        spawner.setDelay(marcador.delayMinimo);
        spawner.setMinSpawnDelay(marcador.delayMinimo);
        spawner.setMaxSpawnDelay(Math.max(marcador.delayMinimo, marcador.delayMaximo));
        spawner.setSpawnCount(marcador.cantidadPorTanda);
        spawner.setRequiredPlayerRange(marcador.rangoJugador);
        spawner.setMaxNearbyEntities(marcador.maximoCercanos);
        spawner.setSpawnRange(marcador.rangoSpawn);

        spawner.getPersistentDataContainer().set(plugin.claveMob(), PersistentDataType.STRING, marcador.mob);
        spawner.getPersistentDataContainer().set(plugin.claveNivel(), PersistentDataType.INTEGER, marcador.nivel);
        spawner.update(true, false);
    }
}
