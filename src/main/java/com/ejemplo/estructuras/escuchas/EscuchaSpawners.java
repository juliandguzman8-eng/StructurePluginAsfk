package com.ejemplo.estructuras.escuchas;
 
import com.ejemplo.estructuras.EstructurasPlugin;
import com.ejemplo.estructuras.modelo.MarcadorSpawner;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
 
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
 
/**
 * Gestiona los spawners de las estructuras.
 *
 * El bloque de spawner es solo decorativo: el spawn vanilla se cancela siempre y
 * es este gestor quien decide cuando generar el MythicMob, con su propio cooldown,
 * su distancia al jugador y su limite de mobs vivos (contando los que el mismo ha
 * generado, que es lo unico fiable con mobs personalizados).
 */
public class EscuchaSpawners implements Listener {
 
    public static class Datos {
        public String mob = "";
        public int nivel = 1;
        public int delayMinimo = 300;
        public int delayMaximo = 700;
        public int cantidad = 1;
        public int rangoJugador = 16;
        public int maximoCercanos = 4;
        public int rangoSpawn = 4;
        public long proximoMs = 0L;
        public final List<UUID> vivos = new ArrayList<>();
    }
 
    private final EstructurasPlugin plugin;
    private final Map<String, Datos> spawners = new ConcurrentHashMap<>();
    private final File archivo;
    private final Random random = new Random();
    private boolean sucio = false;
 
    public EscuchaSpawners(EstructurasPlugin plugin) {
        this.plugin = plugin;
        this.archivo = new File(plugin.getDataFolder(), "spawners.yml");
    }
 
    // ---------- clave ----------
 
    private String clave(World mundo, int x, int y, int z) {
        return mundo.getName() + ";" + x + ";" + y + ";" + z;
    }
 
    private String clave(Block bloque) {
        return clave(bloque.getWorld(), bloque.getX(), bloque.getY(), bloque.getZ());
    }
 
    // ---------- persistencia ----------
 
    public void cargar() {
        spawners.clear();
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection raiz = yml.getConfigurationSection("spawners");
        if (raiz == null) {
            return;
        }
        for (String clave : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(clave);
            if (s == null) {
                continue;
            }
            Datos d = new Datos();
            d.mob = s.getString("mob", "");
            d.nivel = s.getInt("nivel", 1);
            d.delayMinimo = s.getInt("delay-minimo", 300);
            d.delayMaximo = s.getInt("delay-maximo", 700);
            d.cantidad = s.getInt("cantidad", 1);
            d.rangoJugador = s.getInt("rango-jugador", 16);
            d.maximoCercanos = s.getInt("maximo-cercanos", 4);
            d.rangoSpawn = s.getInt("rango-spawn", 4);
            spawners.put(clave, d);
        }
        plugin.getLogger().info("Spawners de estructuras cargados: " + spawners.size());
    }
 
    public void guardar() {
        if (!sucio) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Datos> entrada : spawners.entrySet()) {
            String base = "spawners." + entrada.getKey() + ".";
            Datos d = entrada.getValue();
            yml.set(base + "mob", d.mob);
            yml.set(base + "nivel", d.nivel);
            yml.set(base + "delay-minimo", d.delayMinimo);
            yml.set(base + "delay-maximo", d.delayMaximo);
            yml.set(base + "cantidad", d.cantidad);
            yml.set(base + "rango-jugador", d.rangoJugador);
            yml.set(base + "maximo-cercanos", d.maximoCercanos);
            yml.set(base + "rango-spawn", d.rangoSpawn);
        }
        try {
            yml.save(archivo);
            sucio = false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar spawners.yml", e);
        }
    }
 
    public int total() {
        return spawners.size();
    }
 
    // ---------- registro ----------
 
    public void registrar(Block bloque, MarcadorSpawner marcador) {
        Datos d = new Datos();
        d.mob = marcador.mob;
        d.nivel = marcador.nivel;
        d.delayMinimo = marcador.delayMinimo;
        d.delayMaximo = Math.max(marcador.delayMinimo, marcador.delayMaximo);
        d.cantidad = Math.max(1, marcador.cantidadPorTanda);
        d.rangoJugador = marcador.rangoJugador;
        d.maximoCercanos = Math.max(1, marcador.maximoCercanos);
        d.rangoSpawn = Math.max(1, marcador.rangoSpawn);
        d.proximoMs = System.currentTimeMillis() + 50L * d.delayMinimo;
        spawners.put(clave(bloque), d);
        sucio = true;
    }
 
    // ---------- tarea ----------
 
    public void iniciarTarea() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 100L, 20L);
    }
 
    private void tick() {
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<String, Datos>> it = spawners.entrySet().iterator();
 
        while (it.hasNext()) {
            Map.Entry<String, Datos> entrada = it.next();
            String[] partes = entrada.getKey().split(";");
            if (partes.length != 4) {
                it.remove();
                sucio = true;
                continue;
            }
 
            World mundo = Bukkit.getWorld(partes[0]);
            if (mundo == null) {
                continue;
            }
 
            int x, y, z;
            try {
                x = Integer.parseInt(partes[1]);
                y = Integer.parseInt(partes[2]);
                z = Integer.parseInt(partes[3]);
            } catch (NumberFormatException e) {
                it.remove();
                sucio = true;
                continue;
            }
 
            if (!mundo.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
 
            Block bloque = mundo.getBlockAt(x, y, z);
            if (bloque.getType() != Material.SPAWNER) {
                // Alguien lo rompio: se olvida.
                it.remove();
                sucio = true;
                continue;
            }
 
            Datos d = entrada.getValue();
            if (ahora < d.proximoMs) {
                continue;
            }
 
            Location centro = bloque.getLocation().add(0.5, 0.5, 0.5);
            if (!hayJugadorCerca(mundo, centro, d.rangoJugador)) {
                continue;
            }
 
            d.vivos.removeIf(uuid -> {
                Entity e = Bukkit.getEntity(uuid);
                return e == null || e.isDead();
            });
 
            if (d.vivos.size() >= d.maximoCercanos) {
                d.proximoMs = ahora + 5000L;
                continue;
            }
 
            int aGenerar = Math.min(d.cantidad, d.maximoCercanos - d.vivos.size());
            int generados = 0;
            for (int i = 0; i < aGenerar; i++) {
                Location donde = buscarHueco(mundo, x, y, z, d.rangoSpawn);
                if (donde == null) {
                    continue;
                }
                Entity entidad = plugin.mythic().spawnearEntidad(d.mob, donde, d.nivel);
                if (entidad != null) {
                    d.vivos.add(entidad.getUniqueId());
                    generados++;
                }
            }
 
            if (plugin.debug()) {
                plugin.getLogger().info("Spawner " + entrada.getKey() + " -> " + generados
                        + " " + d.mob + " (vivos: " + d.vivos.size() + "/" + d.maximoCercanos + ")");
            }
 
            int margen = Math.max(1, d.delayMaximo - d.delayMinimo + 1);
            d.proximoMs = ahora + 50L * (d.delayMinimo + random.nextInt(margen));
        }
    }
 
    private boolean hayJugadorCerca(World mundo, Location centro, int rango) {
        double limite = (double) rango * rango;
        for (Player jugador : mundo.getPlayers()) {
            if (jugador.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (jugador.getLocation().distanceSquared(centro) <= limite) {
                return true;
            }
        }
        return false;
    }
 
    /** Busca un hueco de 2 bloques de alto sobre suelo solido cerca del spawner. */
    private Location buscarHueco(World mundo, int x, int y, int z, int radio) {
        for (int intento = 0; intento < 16; intento++) {
            int px = x + random.nextInt(radio * 2 + 1) - radio;
            int pz = z + random.nextInt(radio * 2 + 1) - radio;
            int py = y + random.nextInt(3) - 1;
 
            if (py - 1 < mundo.getMinHeight() || py + 1 >= mundo.getMaxHeight()) {
                continue;
            }
            Block pies = mundo.getBlockAt(px, py, pz);
            Block cabeza = mundo.getBlockAt(px, py + 1, pz);
            Block suelo = mundo.getBlockAt(px, py - 1, pz);
 
            if (pies.getType().isAir() && cabeza.getType().isAir() && suelo.getType().isSolid()) {
                return new Location(mundo, px + 0.5, py, pz + 0.5);
            }
        }
        return null;
    }
 
    // ---------- evento ----------
 
    /** El spawner vanilla nunca genera nada: de eso se encarga la tarea. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alGenerarSpawner(SpawnerSpawnEvent evento) {
        if (evento.getSpawner() == null) {
            return;
        }
        if (spawners.containsKey(clave(evento.getSpawner().getBlock()))) {
            evento.setCancelled(true);
        }
    }
}
 
