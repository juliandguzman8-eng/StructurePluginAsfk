package com.ejemplo.estructuras;
 
import com.ejemplo.estructuras.comandos.ComandoEstructuras;
import com.ejemplo.estructuras.escuchas.EscuchaChunks;
import com.ejemplo.estructuras.escuchas.EscuchaSpawners;
import com.ejemplo.estructuras.generacion.ColocadorEstructuras;
import com.ejemplo.estructuras.gestor.GestorBotin;
import com.ejemplo.estructuras.gestor.GestorEstructuras;
import com.ejemplo.estructuras.gestor.RegistroColocadas;
import com.ejemplo.estructuras.hooks.MythicHook;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
 
import java.io.File;
 
public final class EstructurasPlugin extends JavaPlugin {
 
    private NamespacedKey claveMob;
    private NamespacedKey claveNivel;
 
    private File carpetaSchematics;
    private MythicHook mythic;
    private GestorBotin botin;
    private GestorEstructuras estructuras;
    private RegistroColocadas registro;
    private ColocadorEstructuras colocador;
    private EscuchaSpawners spawners;
 
    @Override
    public void onEnable() {
        if (!hayWorldEdit()) {
            getLogger().severe("Hace falta WorldEdit o FastAsyncWorldEdit para pegar los schematics. Desactivando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
 
        claveMob = new NamespacedKey(this, "mythic_mob");
        claveNivel = new NamespacedKey(this, "mythic_nivel");
 
        saveDefaultConfig();
        guardarSiFalta("estructuras.yml");
        guardarSiFalta("botin.yml");
 
        carpetaSchematics = new File(getDataFolder(), "schematics");
        if (!carpetaSchematics.exists() && !carpetaSchematics.mkdirs()) {
            getLogger().warning("No se pudo crear la carpeta schematics.");
        }
 
        mythic = new MythicHook(this);
        botin = new GestorBotin(this);
        estructuras = new GestorEstructuras(this);
        registro = new RegistroColocadas(this);
        colocador = new ColocadorEstructuras(this);
        spawners = new EscuchaSpawners(this);
 
        registro.cargar();
        spawners.cargar();
        recargar();
 
        getServer().getPluginManager().registerEvents(new EscuchaChunks(this), this);
        getServer().getPluginManager().registerEvents(spawners, this);
        spawners.iniciarTarea();
 
        PluginCommand comando = getCommand("estructuras");
        if (comando != null) {
            ComandoEstructuras ejecutor = new ComandoEstructuras(this);
            comando.setExecutor(ejecutor);
            comando.setTabCompleter(ejecutor);
        }
 
        long minutos = Math.max(1L, getConfig().getLong("general.guardado-automatico-minutos", 5L));
        long ticks = minutos * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            registro.guardar();
            spawners.guardar();
        }, ticks, ticks);
 
        getLogger().info("EstructurasAleatorias activado.");
    }
 
    @Override
    public void onDisable() {
        if (registro != null) {
            registro.guardar();
        }
        if (spawners != null) {
            spawners.guardar();
        }
    }
 
    /** Recarga config.yml, estructuras.yml y botin.yml. */
    public void recargar() {
        reloadConfig();
        botin.cargar();
        estructuras.cargar();
    }
 
    private boolean hayWorldEdit() {
        return getServer().getPluginManager().getPlugin("WorldEdit") != null
                || getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }
 
    private void guardarSiFalta(String nombre) {
        if (!new File(getDataFolder(), nombre).exists()) {
            saveResource(nombre, false);
        }
    }
 
    public boolean debug() {
        return getConfig().getBoolean("general.debug", false);
    }
 
    public File carpetaSchematics() {
        return carpetaSchematics;
    }
 
    public MythicHook mythic() {
        return mythic;
    }
 
    public GestorBotin botin() {
        return botin;
    }
 
    public GestorEstructuras estructuras() {
        return estructuras;
    }
 
    public RegistroColocadas registro() {
        return registro;
    }
 
    public ColocadorEstructuras colocador() {
        return colocador;
    }
 
    public EscuchaSpawners spawners() {
        return spawners;
    }
 
    public NamespacedKey claveMob() {
        return claveMob;
    }
 
    public NamespacedKey claveNivel() {
        return claveNivel;
    }
}
 
