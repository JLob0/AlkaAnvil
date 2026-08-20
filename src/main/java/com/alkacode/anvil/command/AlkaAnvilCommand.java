package com.alkacode.anvil.command;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.config.AnvilConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AlkaAnvilCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "info", "config", "bigorna");

    private final AlkaAnvilPlugin plugin;
    private final AnvilConfig config;

    public AlkaAnvilCommand(AlkaAnvilPlugin plugin, AnvilConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("alkaanvil.reload")) {
                send(sender, config.message("no-permission"));
                return true;
            }
            plugin.reloadAll();
            send(sender, config.message("reload"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            send(sender, "<gray>AlkaAnvil v" + plugin.getPluginMeta().getVersion()
                    + " - AdvancedEnchantments: " + (com.alkacode.anvil.enchant.AdvancedEnchantmentsWrapper.isAvailable() ? "<green>ativo" : "<red>ausente")
                    + " <gray>| Custo em moeda: " + (config.monetaryCostEnabled() ? "<green>ativo" : "<gray>desativado"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("bigorna")) {
            if (!(sender instanceof Player player)) {
                send(sender, config.message("player-only"));
                return true;
            }
            openVirtualAnvil(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("config")) {
            if (!sender.hasPermission("alkaanvil.config")) {
                send(sender, config.message("no-permission"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                send(sender, config.message("player-only"));
                return true;
            }
            plugin.openAdminGui(player);
            return true;
        }

        send(sender, "<yellow>Uso: /alkaanvil <reload|info|config|bigorna>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String lower = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(lower)).collect(Collectors.toList());
    }

    private void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.prefix() + miniMessage));
    }

    /** openAnvil(Location, boolean) e deprecated mas nao tem substituto sem bloco real na API -
     * mesmo padrao ja usado em AlkaVips#PerkCommands para /vipbigorna. */
    @SuppressWarnings("deprecation")
    private void openVirtualAnvil(Player player) {
        player.openAnvil(null, true);
    }
}
