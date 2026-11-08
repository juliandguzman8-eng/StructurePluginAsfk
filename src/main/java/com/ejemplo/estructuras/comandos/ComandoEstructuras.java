package com.ejemplo.estructuras.comandos;

import com.ejemplo.estructuras.EstructurasPlugin;
import com.ejemplo.estructuras.modelo.EstructuraDef;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComandoEstructuras implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMANDOS =
            List.of("recargar", "lista", "info", "aqui", "pegar", "limpiar");

    private final EstructurasPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ComandoEstructuras(EstructurasPlugin plugin) {
        this.plugin = plugin;
    }

    private void enviar(CommandSender quien, String texto) {
        quien.sendMessage(mm.deserialize("<dark_aqua>[Estructuras]</dark_aqua> " + texto));
    }

    @Override
    public boolean onCommand(CommandSender emisor, Command comando, String etiqueta, String[] args) {
        if (args.length == 0) {
            enviar(emisor, "<gray>Usa:</gray> <white>/estructuras recargar | lista | info <id> | aqui <id> | pegar <id> <x> <z> | limpiar</white>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "recargar" -> {
                plugin.recargar();
                enviar(emisor, "<green>Configuracion recargada.</green> <gray>Estructuras: "
                        + plugin.estructuras().ids().size() + ", tablas: " + plugin.botin().ids().size() + "</gray>");
            }

            case "lista" -> {
                if (plugin.estructuras().ids().isEmpty()) {
                    enviar(emisor, "<red>No hay estructuras cargadas.</red>");
                    return true;
                }
                enviar(emisor, "<gray>Estructuras cargadas:</gray>");
                for (EstructuraDef def : plugin.estructuras().barajadas()) {
                    emisor.sendMessage(mm.deserialize("<gray> - </gray><white>" + def.id + "</white> "
                            + (def.activa ? "<green>activa</green>" : "<red>inactiva</red>")
                            + " <gray>prob " + def.probabilidad + " | colocadas "
                            + plugin.registro().totalDe(def.id) + "</gray>"));
                }
            }

            case "info" -> {
                if (args.length < 2) {
                    enviar(emisor, "<red>Falta el id. /estructuras info <id></red>");
                    return true;
                }
                EstructuraDef def = plugin.estructuras().estructura(args[1]);
                if (def == null) {
                    enviar(emisor, "<red>No existe esa estructura.</red>");
                    return true;
                }
                emisor.sendMessage(mm.deserialize("<gray>Archivo:</gray> <white>" + def.archivo + "</white>"));
                emisor.sendMessage(mm.deserialize("<gray>Probabilidad por chunk:</gray> <white>" + def.probabilidad + "</white>"));
                emisor.sendMessage(mm.deserialize("<gray>Y permitido:</gray> <white>" + def.yMinimo + " - " + def.yMaximo + "</white>"));
                emisor.sendMessage(mm.deserialize("<gray>Distancia minima:</gray> <white>" + def.distanciaMinima + "</white>"));
                emisor.sendMessage(mm.deserialize("<gray>Marcadores de mobs:</gray> <white>" + def.marcadoresMobs.size()
                        + "</white> <gray>spawners:</gray> <white>" + def.marcadoresSpawners.size() + "</white>"));
                emisor.sendMessage(mm.deserialize("<gray>Tabla de cofres:</gray> <white>" + def.tablaCofres + "</white>"));
                emisor.sendMessage(mm.deserialize("<gray>Veces colocada:</gray> <white>" + plugin.registro().totalDe(def.id) + "</white>"));
            }

            case "aqui" -> {
                if (!(emisor instanceof Player jugador)) {
                    enviar(emisor, "<red>Este subcomando es solo para jugadores.</red>");
                    return true;
                }
                if (args.length < 2) {
                    enviar(emisor, "<red>Falta el id. /estructuras aqui <id></red>");
                    return true;
                }
                EstructuraDef def = plugin.estructuras().estructura(args[1]);
                if (def == null) {
                    enviar(emisor, "<red>No existe esa estructura.</red>");
                    return true;
                }
                boolean ok = plugin.colocador().colocar(def, jugador.getWorld(),
                        jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ(), true);
                enviar(emisor, ok ? "<green>Estructura colocada.</green>" : "<red>No se pudo colocar.</red>");
            }

            case "pegar" -> {
                if (args.length < 4) {
                    enviar(emisor, "<red>Uso: /estructuras pegar <id> <x> <z></red>");
                    return true;
                }
                EstructuraDef def = plugin.estructuras().estructura(args[1]);
                if (def == null) {
                    enviar(emisor, "<red>No existe esa estructura.</red>");
                    return true;
                }
                World mundo = (emisor instanceof Player jugador)
                        ? jugador.getWorld()
                        : plugin.getServer().getWorlds().get(0);
                try {
                    int x = Integer.parseInt(args[2]);
                    int z = Integer.parseInt(args[3]);
                    boolean ok = plugin.colocador().colocar(def, mundo, x, z, true);
                    enviar(emisor, ok
                            ? "<green>Estructura colocada en " + x + ", " + z + " (" + mundo.getName() + ").</green>"
                            : "<red>No se pudo colocar ahi.</red>");
                } catch (NumberFormatException e) {
                    enviar(emisor, "<red>Las coordenadas deben ser numeros.</red>");
                }
            }

            case "limpiar" -> {
                int total = plugin.registro().total();
                plugin.registro().limpiar();
                enviar(emisor, "<green>Registro borrado (" + total + " posiciones).</green> "
                        + "<gray>Esto no elimina las estructuras ya construidas, solo olvida donde estaban "
                        + "para el calculo de distancias.</gray>");
            }

            default -> enviar(emisor, "<red>Subcomando desconocido.</red>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender emisor, Command comando, String etiqueta, String[] args) {
        List<String> sugerencias = new ArrayList<>();
        if (args.length == 1) {
            String parcial = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMANDOS) {
                if (sub.startsWith(parcial)) {
                    sugerencias.add(sub);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("recargar")
                && !args[0].equalsIgnoreCase("lista") && !args[0].equalsIgnoreCase("limpiar")) {
            String parcial = args[1].toLowerCase(Locale.ROOT);
            for (String id : plugin.estructuras().ids()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(parcial)) {
                    sugerencias.add(id);
                }
            }
        }
        return sugerencias;
    }
}
