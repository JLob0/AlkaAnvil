package com.alkacode.anvil.command;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.enchant.AlkaEnchantment;
import com.alkacode.anvil.enchant.AlkaEnchantmentRegistry;
import com.alkacode.anvil.enchant.VanillaEnchantment;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /encantar [tipo] <encantamento> [nivel] - aplica encantamento vanilla ou AE no item
 * segurado. O argumento opcional "tipo" (espada, picareta, arco, ...) so filtra quais
 * encantamentos aparecem no tab. Vanilla pode passar do nivel natural (o AlkaAnvil ja
 * existe pra isso, ex: eficiencia 40); AE respeita o proprio limite; encantamento sem
 * nivel (ex: mending) nao precisa de valor e aplica 1.
 */
public final class EnchantCommand implements CommandExecutor, TabCompleter {

    private final AlkaAnvilPlugin plugin;
    private final AnvilConfig config;

    private static final Map<String, List<Material>> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("espada", variants("_SWORD"));
        TYPES.put("picareta", variants("_PICKAXE"));
        TYPES.put("machado", variants("_AXE"));
        TYPES.put("enxada", variants("_HOE"));
        TYPES.put("pa", variants("_SHOVEL"));
        TYPES.put("arco", List.of(Material.BOW, Material.CROSSBOW));
        TYPES.put("tridente", List.of(Material.TRIDENT));
        TYPES.put("moca", List.of(Material.MACE));
        TYPES.put("capacete", variants("_HELMET"));
        TYPES.put("peitoral", variants("_CHESTPLATE"));
        TYPES.put("calcas", variants("_LEGGINGS"));
        TYPES.put("botas", variants("_BOOTS"));
        TYPES.put("varinha", List.of(Material.FISHING_ROD));
        TYPES.put("todos", List.of());
    }

    private static List<Material> variants(String suffix) {
        List<Material> result = new ArrayList<>();
        for (String base : List.of("WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND", "NETHERITE")) {
            Material mat = Material.matchMaterial(base + suffix);
            if (mat != null) {
                result.add(mat);
            }
        }
        return result;
    }

    public EnchantCommand(AlkaAnvilPlugin plugin, AnvilConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, config.message("player-only"));
            return true;
        }
        if (!sender.hasPermission("alkaanvil.encant")) {
            send(sender, config.message("no-permission"));
            return true;
        }
        if (args.length < 1) {
            send(sender, config.message("encant-usage"));
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            send(sender, config.message("encant-no-item"));
            return true;
        }

        String enchantArg;
        String levelArg;
        if (TYPES.containsKey(args[0].toLowerCase(Locale.ROOT))) {
            if (args.length < 2) {
                send(sender, config.message("encant-usage"));
                return true;
            }
            enchantArg = args[1];
            levelArg = args.length >= 3 ? args[2] : null;
        } else {
            enchantArg = args[0];
            levelArg = args.length >= 2 ? args[1] : null;
        }

        AlkaEnchantment enchantment = resolve(enchantArg);
        if (enchantment == null) {
            send(sender, config.message("encant-unknown"));
            return true;
        }

        int max = enchantment.getMaxLevel();
        boolean vanilla = enchantment instanceof VanillaEnchantment;

        int level;
        if (levelArg == null) {
            // Sem nivel especificado: usa o maximo do AE, ou o natural/override do vanilla.
            level = max;
        } else {
            try {
                level = Integer.parseInt(levelArg);
            } catch (NumberFormatException e) {
                send(sender, config.message("encant-invalid-level"));
                return true;
            }
        }
        if (level < 1) {
            level = 1;
        }

        // Vanilla passa do nivel natural; AE respeita o proprio limite (clamp).
        if (!vanilla && level > max) {
            level = max;
        }

        ItemStack result = enchantment.setLevel(held.clone(), level);
        player.getInventory().setItemInMainHand(result);
        send(sender, config.message("encant-success")
                .replace("<enchant>", enchantment.getName())
                .replace("<level>", String.valueOf(level)));
        return true;
    }

    private AlkaEnchantment resolve(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        AlkaEnchantmentRegistry registry = plugin.getRegistry();

        AlkaEnchantment direct = registry.get(lower);
        if (direct != null) {
            return direct;
        }

        String bare = lower.contains(":") ? lower.substring(lower.indexOf(':') + 1) : lower;
        for (Map.Entry<String, AlkaEnchantment> entry : registry.all().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(bare)
                    || entry.getValue().getName().equalsIgnoreCase(bare)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String type : TYPES.keySet()) {
                if (type.startsWith(lower)) {
                    result.add(type);
                }
            }
            result.addAll(enchantNames(lower));
            return result;
        }

        boolean typeArg = TYPES.containsKey(args[0].toLowerCase(Locale.ROOT));
        if (typeArg) {
            if (args.length == 2) {
                return enchantNamesForType(args[0].toLowerCase(Locale.ROOT), args[1].toLowerCase(Locale.ROOT));
            }
            if (args.length == 3) {
                AlkaEnchantment enchantment = resolve(args[1]);
                if (enchantment != null) {
                    return levelCandidates(enchantment, args[2]);
                }
            }
            return List.of();
        }

        // Sem tipo: args[0] = encantamento, args[1] = nivel.
        if (args.length == 2) {
            AlkaEnchantment enchantment = resolve(args[0]);
            if (enchantment != null) {
                return levelCandidates(enchantment, args[1]);
            }
        }
        return List.of();
    }

    private List<String> enchantNames(String lower) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, AlkaEnchantment> entry : plugin.getRegistry().all().entrySet()) {
            String name = entry.getValue().getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                names.add(name);
            }
        }
        return names;
    }

    /** Filtra os encantamentos pelos materiais do tipo - "espada" so mostra enchants que encantam qualquer item da lista. */
    private List<String> enchantNamesForType(String type, String lower) {
        List<Material> materials = TYPES.get(type);
        if (materials == null || materials.isEmpty()) {
            return enchantNames(lower);
        }
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, AlkaEnchantment> entry : plugin.getRegistry().all().entrySet()) {
            AlkaEnchantment enchantment = entry.getValue();
            String name = enchantment.getName();
            if (!name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                continue;
            }
            for (Material material : materials) {
                if (enchantment.canEnchantItem(new ItemStack(material))) {
                    names.add(name);
                    break;
                }
            }
        }
        return names;
    }

    private List<String> levelCandidates(AlkaEnchantment enchantment, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        int max = enchantment.getMaxLevel();
        boolean vanilla = enchantment instanceof VanillaEnchantment;
        List<String> levels = new ArrayList<>();
        int cap = vanilla ? Math.max(max, 10) : max;
        for (int i = 1; i <= cap; i++) {
            if (String.valueOf(i).startsWith(lower)) {
                levels.add(String.valueOf(i));
            }
        }
        return levels;
    }

    private void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.prefix() + miniMessage));
    }
}
