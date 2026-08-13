package com.alkacode.anvil.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Wrapper tipado sobre config.yml - recarregado inteiro em /alkaanvil reload. */
public final class AnvilConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public AnvilConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ---------------------------------------------------------------- cost-limits

    public boolean removeTooExpensive() {
        return config.getBoolean("cost-limits.remove-too-expensive", true);
    }

    public int maxCost() {
        return config.getInt("cost-limits.max-cost", 0);
    }

    public int renameCost() {
        return config.getInt("cost-limits.rename-cost", 1);
    }

    public int itemRepairCost() {
        return config.getInt("cost-limits.item-repair-cost", 2);
    }

    public int unitRepairCost() {
        return config.getInt("cost-limits.unit-repair-cost", 1);
    }

    // ---------------------------------------------------------------- enchant-values

    public record EnchantValue(int item, int book) {
    }

    public EnchantValue enchantValue(String enchantKey) {
        ConfigurationSection section = config.getConfigurationSection("enchant-values." + enchantKey);
        if (section == null) {
            section = config.getConfigurationSection("enchant-values.default");
        }
        if (section == null) {
            return new EnchantValue(2, 1);
        }
        return new EnchantValue(section.getInt("item", 2), section.getInt("book", 1));
    }

    // ---------------------------------------------------------------- work-penalty

    public enum MergeMode { SUM, MAX, AVERAGE, NONE }

    public MergeMode workPenaltyMergeMode() {
        try {
            return MergeMode.valueOf(config.getString("work-penalty.merge-mode", "SUM").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MergeMode.SUM;
        }
    }

    public int workPenaltyAdditive() {
        return config.getInt("work-penalty.additive-penalty", 0);
    }

    // ---------------------------------------------------------------- enchantment-limits

    public int maxEnchantsPerItem() {
        return config.getInt("enchantment-limits.max-enchants-per-item", 0);
    }

    public Map<String, Integer> perEnchantLevelOverrides() {
        Map<String, Integer> result = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("enchantment-limits.per-enchant");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            result.put(key.toLowerCase(java.util.Locale.ROOT), section.getInt(key));
        }
        return result;
    }

    // ---------------------------------------------------------------- enchantment-conflicts

    public List<List<String>> enchantConflictGroups() {
        List<List<String>> groups = new ArrayList<>();
        for (Object raw : config.getList("enchantment-conflicts.groups", List.of())) {
            if (raw instanceof List<?> list) {
                List<String> group = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String s) {
                        group.add(s.toLowerCase(java.util.Locale.ROOT));
                    }
                }
                groups.add(group);
            }
        }
        return groups;
    }

    // ---------------------------------------------------------------- unit-repair

    public Map<Material, List<Material>> unitRepairMaterials() {
        Map<Material, List<Material>> result = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("unit-repair.materials");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            Material repairMat = Material.matchMaterial(key);
            if (repairMat == null) {
                continue;
            }
            List<Material> targets = new ArrayList<>();
            for (String name : section.getStringList(key)) {
                Material target = Material.matchMaterial(name);
                if (target != null) {
                    targets.add(target);
                }
            }
            result.put(repairMat, targets);
        }
        return result;
    }

    public double unitRepairFraction() {
        return config.getDouble("unit-repair.repair-per-unit", 0.25);
    }

    // ---------------------------------------------------------------- monetary-cost

    public boolean monetaryCostEnabled() {
        return config.getBoolean("monetary-cost.enabled", false);
    }

    public String monetaryCurrency() {
        return config.getString("monetary-cost.currency", "coins");
    }

    public double monetaryMultiplier(String operation) {
        return config.getDouble("monetary-cost.multipliers." + operation, 1.0);
    }

    // ---------------------------------------------------------------- rename-colors

    public boolean renameColorsEnabled() {
        return config.getBoolean("rename-colors.enabled", true);
    }

    public boolean renameRequirePermission() {
        return config.getBoolean("rename-colors.require-permission", true);
    }

    public int renameColorCost() {
        return config.getInt("rename-colors.color-cost", 0);
    }

    // ---------------------------------------------------------------- disenchant / shatter

    public boolean disenchantEnabled() {
        return config.getBoolean("disenchant.enabled", true);
    }

    public int disenchantBaseCost() {
        return config.getInt("disenchant.base-cost", 5);
    }

    public int disenchantCostPerEnchant() {
        return config.getInt("disenchant.cost-per-enchant", 3);
    }

    public int disenchantCostPerLevel() {
        return config.getInt("disenchant.cost-per-level", 1);
    }

    public boolean shatterEnabled() {
        return config.getBoolean("shatter.enabled", true);
    }

    public int shatterBaseCost() {
        return config.getInt("shatter.base-cost", 3);
    }

    public int shatterCostPerLevel() {
        return config.getInt("shatter.cost-per-level", 2);
    }

    public boolean disenchantRequirePermission() {
        return config.getBoolean("disenchant.require-permission", false);
    }

    public boolean shatterRequirePermission() {
        return config.getBoolean("shatter.require-permission", false);
    }

    public java.util.Set<Material> disenchantBlacklistMaterials() {
        java.util.Set<Material> result = new java.util.HashSet<>();
        for (String name : config.getStringList("disenchant.blacklist-materials")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                result.add(material);
            }
        }
        return result;
    }

    public java.util.Set<String> disenchantBlacklistEnchants() {
        java.util.Set<String> result = new java.util.HashSet<>();
        for (String key : config.getStringList("disenchant.blacklist-enchants")) {
            result.add(key.toLowerCase(java.util.Locale.ROOT));
        }
        return result;
    }

    // ---------------------------------------------------------------- messages

    public String prefix() {
        return config.getString("messages.prefix", "");
    }

    public String message(String path) {
        return config.getString("messages." + path, "<red>Mensagem ausente: " + path);
    }

    // ---------------------------------------------------------------- item-stats

    public boolean itemStatsEnabled() {
        return config.getBoolean("item-stats.enabled", true);
    }

    public String itemStatsRequiredTag() {
        return config.getString("item-stats.required-tag", "");
    }

    public boolean itemStatsLoreEnabled() {
        return config.getBoolean("item-stats.lore.enabled", true);
    }

    public boolean itemStatsLoreOnTop() {
        return "TOP".equalsIgnoreCase(config.getString("item-stats.lore.position", "BOTTOM"));
    }

    public String itemStatsLoreHeader() {
        return config.getString("item-stats.lore.header", "");
    }

    public String itemStatsLoreFooter() {
        return config.getString("item-stats.lore.footer", "");
    }

    public String itemStatsLoreFormat() {
        return config.getString("item-stats.lore.format", "<gray><stat_name>: <yellow><value>");
    }

    public record ItemStatsCategory(String name, List<Material> materials, List<String> track) {
    }

    public List<ItemStatsCategory> itemStatsCategories() {
        List<ItemStatsCategory> result = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("item-stats.stats");
        if (section == null) {
            return result;
        }
        for (String categoryName : section.getKeys(false)) {
            ConfigurationSection categorySection = section.getConfigurationSection(categoryName);
            if (categorySection == null) {
                continue;
            }
            List<Material> materials = new ArrayList<>();
            for (String name : categorySection.getStringList("materials")) {
                Material material = Material.matchMaterial(name);
                if (material != null) {
                    materials.add(material);
                }
            }
            List<String> track = categorySection.getStringList("track");
            result.add(new ItemStatsCategory(categoryName, materials, track));
        }
        return result;
    }

    public String itemStatsName(String statType) {
        return config.getString("item-stats.stat-names." + statType, statType);
    }
}
