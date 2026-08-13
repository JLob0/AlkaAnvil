package com.alkacode.anvil.anvil;

import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.economy.AlkaEconomyHook;
import com.alkacode.anvil.enchant.AlkaEnchantment;
import com.alkacode.anvil.enchant.AlkaEnchantmentRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logica central de merge/rename/reparo da bigorna. Simplificacao deliberada em
 * relacao ao vanilla real: penalidades de encantamento ilegal nao geram um custo
 * adicional separado (o encantamento conflitante e simplesmente descartado do
 * resultado) - o CustomAnvil real tem um `sacrifice_illegal_enchant_cost` pra isso,
 * mas isso fica pra uma proxima iteracao, nao e essencial pro MVP funcionar.
 */
public final class AnvilMergeLogic {

    private final JavaPlugin plugin;
    private final AnvilConfig config;
    private final AlkaEnchantmentRegistry registry;
    private final AlkaEconomyHook economyHook;

    public AnvilMergeLogic(JavaPlugin plugin, AnvilConfig config, AlkaEnchantmentRegistry registry, AlkaEconomyHook economyHook) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.economyHook = economyHook;
    }

    public record MergeResult(ItemStack item, AnvilCost cost, AnvilUseType useType) {
    }

    public MergeResult compute(Player player, ItemStack left, ItemStack right, String renameText) {
        if (left == null || left.getType().isAir()) {
            return null;
        }

        ItemStack result = left.clone();
        int xpCost = 0;
        AnvilUseType useType = null;

        // ---------------------------------------------------------------- rename
        String currentName = plainName(result);
        boolean renaming = renameText != null && !renameText.isBlank() && !renameText.equals(currentName);
        if (renaming) {
            boolean usedColorTag = RenameSanitizer.hasColorTag(renameText);
            applyRename(player, result, renameText);
            xpCost += config.renameCost();
            if (usedColorTag) {
                xpCost += config.renameColorCost();
            }
            useType = AnvilUseType.RENAME;
        }

        // ---------------------------------------------------------------- right-hand item
        if (right != null && !right.getType().isAir()) {
            if (right.getType() == left.getType() && left.getType().getMaxDurability() > 0) {
                // ITEM_REPAIR: mesmo tipo de item -> combina durabilidade + encantamentos
                xpCost += repairDurability(result, left, right);
                xpCost += mergeEnchants(player, result, left, right, right.getType() == Material.BOOK
                        || right.getType() == Material.ENCHANTED_BOOK);
                xpCost += config.itemRepairCost();
                useType = AnvilUseType.ITEM_REPAIR;
            } else if (canReceiveEnchantsFrom(left, right)) {
                // ENCHANT_MERGE: livro encantado (ou item compativel) -> aplica no item
                xpCost += mergeEnchants(player, result, left, right, true);
                useType = AnvilUseType.ENCHANT_MERGE;
            } else {
                Material unitTarget = unitRepairTarget(left, right);
                if (unitTarget != null) {
                    // UNIT_REPAIR: material bruto (diamante, lingote...) repara fracao da durabilidade
                    xpCost += unitRepair(result);
                    xpCost += config.unitRepairCost();
                    useType = AnvilUseType.UNIT_REPAIR;
                } else {
                    return null; // combinacao nao reconhecida
                }
            }
        }

        if (useType == null) {
            return null;
        }

        // ---------------------------------------------------------------- work penalty (repair cost NBT)
        int leftPenalty = repairCostOf(left);
        int rightPenalty = right != null ? repairCostOf(right) : 0;
        int penaltyContribution = switch (config.workPenaltyMergeMode()) {
            case SUM -> leftPenalty + rightPenalty;
            case MAX -> Math.max(leftPenalty, rightPenalty);
            case AVERAGE -> (leftPenalty + rightPenalty) / 2;
            case NONE -> 0;
        };
        xpCost += penaltyContribution + config.workPenaltyAdditive();

        setRepairCostOf(result, Math.max(leftPenalty, rightPenalty) + 1);

        int cappedXp = AnvilXpUtil.applyCap(config, player, Math.max(1, xpCost));

        if (config.monetaryCostEnabled()) {
            String operation = switch (useType) {
                case RENAME -> "rename";
                case ITEM_REPAIR, UNIT_REPAIR, DISENCHANT, SHATTER -> "repair";
                case ENCHANT_MERGE -> "enchantment";
            };
            double multiplier = config.monetaryMultiplier(operation);
            String currency = config.monetaryCurrency();
            double amount = cappedXp * multiplier;
            return new MergeResult(result, AnvilCost.monetary(cappedXp, amount, currency), useType);
        }

        return new MergeResult(result, AnvilCost.xpOnly(cappedXp), useType);
    }

    // ---------------------------------------------------------------- helpers

    private boolean canReceiveEnchantsFrom(ItemStack left, ItemStack right) {
        if (right.getType() != Material.ENCHANTED_BOOK && right.getType() != Material.BOOK) {
            return false;
        }
        return !enchantsOn(right).isEmpty();
    }

    private Material unitRepairTarget(ItemStack left, ItemStack right) {
        List<Material> targets = config.unitRepairMaterials().get(right.getType());
        if (targets != null && targets.contains(left.getType())) {
            return right.getType();
        }
        return null;
    }

    private int unitRepair(ItemStack result) {
        ItemMeta meta = result.getItemMeta();
        if (!(meta instanceof org.bukkit.inventory.meta.Damageable damageable)) {
            return 0;
        }
        int maxDurability = result.getType().getMaxDurability();
        int recover = (int) Math.round(maxDurability * config.unitRepairFraction());
        int newDamage = Math.max(0, damageable.getDamage() - recover);
        damageable.setDamage(newDamage);
        result.setItemMeta(meta);
        return 1;
    }

    private int repairDurability(ItemStack result, ItemStack left, ItemStack right) {
        ItemMeta resultMeta = result.getItemMeta();
        if (!(resultMeta instanceof org.bukkit.inventory.meta.Damageable resultDamageable)) {
            return 0;
        }
        ItemMeta rightMeta = right.getItemMeta();
        if (!(rightMeta instanceof org.bukkit.inventory.meta.Damageable rightDamageable)) {
            return 0;
        }
        int maxDurability = result.getType().getMaxDurability();
        int leftDamage = resultDamageable.getDamage();
        int rightRemaining = maxDurability - rightDamageable.getDamage();
        // regra vanilla: soma a durabilidade restante do segundo item + 12% de bonus, capado no maximo
        int bonus = (int) Math.round(maxDurability * 0.12);
        int newDamage = Math.max(0, leftDamage - rightRemaining - bonus);
        resultDamageable.setDamage(newDamage);
        result.setItemMeta(resultMeta);
        return leftDamage != newDamage ? 1 : 0;
    }

    /** Combina os encantamentos de left+right no result, respeitando limites/conflitos (so vanilla). */
    private int mergeEnchants(Player player, ItemStack result, ItemStack left, ItemStack right, boolean rightIsBook) {
        Map<AlkaEnchantment, Integer> leftEnchants = enchantsOn(left);
        Map<AlkaEnchantment, Integer> rightEnchants = enchantsOn(right);
        Map<String, Integer> levelOverrides = config.perEnchantLevelOverrides();
        List<List<String>> conflictGroups = config.enchantConflictGroups();
        boolean bypassLevel = player.hasPermission("alkaanvil.bypass.level");
        boolean bypassConflict = player.hasPermission("alkaanvil.bypass.conflict");

        Map<AlkaEnchantment, Integer> finalLevels = new HashMap<>(leftEnchants);
        int cost = 0;

        for (Map.Entry<AlkaEnchantment, Integer> entry : rightEnchants.entrySet()) {
            AlkaEnchantment enchant = entry.getKey();
            int incomingLevel = entry.getValue();

            if (!bypassLevel && !enchant.canEnchantItem(result) && result.getType() != Material.BOOK
                    && result.getType() != Material.ENCHANTED_BOOK) {
                continue;
            }

            if (!bypassConflict && !registry.isFromAdvancedEnchantments(enchant)
                    && conflicts(enchant.getKey().toString(), finalLevels.keySet(), conflictGroups)) {
                continue;
            }

            Integer existingLevel = finalLevels.get(enchant);
            int resultLevel;
            if (existingLevel == null) {
                resultLevel = incomingLevel;
            } else if (existingLevel.equals(incomingLevel)) {
                resultLevel = existingLevel + 1;
            } else {
                resultLevel = Math.max(existingLevel, incomingLevel);
            }

            int maxLevel = registry.isFromAdvancedEnchantments(enchant)
                    ? enchant.getMaxLevel()
                    : levelOverrides.getOrDefault(enchant.getKey().toString(), enchant.getMaxLevel());
            if (!bypassLevel) {
                resultLevel = Math.min(resultLevel, maxLevel);
            }

            if (existingLevel == null || resultLevel > existingLevel) {
                finalLevels.put(enchant, resultLevel);
                AnvilConfig.EnchantValue value = config.enchantValue(enchant.getKey().toString());
                cost += resultLevel * (rightIsBook ? value.book() : value.item());
            }
        }

        for (Map.Entry<AlkaEnchantment, Integer> entry : finalLevels.entrySet()) {
            entry.getKey().setLevel(result, entry.getValue());
        }

        int maxEnchants = config.maxEnchantsPerItem();
        if (maxEnchants > 0 && finalLevels.size() > maxEnchants && !bypassLevel) {
            // MVP: nao poda o excedente automaticamente, so acusa - refinamento futuro.
            plugin.getLogger().fine("Item excedeu max-enchants-per-item (" + finalLevels.size() + "/" + maxEnchants + ").");
        }

        return cost;
    }

    private boolean conflicts(String candidateKey, java.util.Set<AlkaEnchantment> existing, List<List<String>> groups) {
        for (List<String> group : groups) {
            if (!group.contains(candidateKey)) {
                continue;
            }
            for (AlkaEnchantment enchant : existing) {
                String key = enchant.getKey().toString();
                if (!key.equals(candidateKey) && group.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<AlkaEnchantment, Integer> enchantsOn(ItemStack item) {
        Map<AlkaEnchantment, Integer> result = new HashMap<>();
        if (item == null || item.getType().isAir()) {
            return result;
        }
        for (AlkaEnchantment enchant : registry.all().values()) {
            int level = enchant.getLevel(item);
            if (level > 0) {
                result.put(enchant, level);
            }
        }
        return result;
    }

    private void applyRename(Player player, ItemStack item, String rawName) {
        Component name = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(RenameSanitizer.sanitize(config, player, rawName));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
    }

    private String plainName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        Component name = meta.displayName();
        return name != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name) : null;
    }

    private int repairCostOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta instanceof org.bukkit.inventory.meta.Repairable repairable ? repairable.getRepairCost() : 0;
    }

    private void setRepairCostOf(ItemStack item, int cost) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Repairable repairable) {
            repairable.setRepairCost(cost);
            item.setItemMeta(meta);
        }
    }
}
