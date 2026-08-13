package com.alkacode.anvil.enchant;

import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Registro central vanilla + AE, indexado por chave "namespace:key" em minusculo. */
public final class AlkaEnchantmentRegistry {

    private final Map<String, AlkaEnchantment> byKey = new HashMap<>();
    private final Logger logger;

    public AlkaEnchantmentRegistry(JavaPlugin plugin, Map<String, Integer> vanillaLevelOverrides) {
        this.logger = plugin.getLogger();
        registerVanilla(vanillaLevelOverrides);
        registerAdvancedEnchantments();
    }

    private void registerVanilla(Map<String, Integer> levelOverrides) {
        for (Enchantment ench : Registry.ENCHANTMENT) {
            String key = ench.getKey().toString();
            int override = levelOverrides.getOrDefault(key, 0);
            byKey.put(key, new VanillaEnchantment(ench, override));
        }
    }

    private void registerAdvancedEnchantments() {
        if (!AdvancedEnchantmentsWrapper.initReflection(logger)) {
            return;
        }
        int count = 0;
        for (String name : AdvancedEnchantmentsWrapper.registeredNames(logger)) {
            AdvancedEnchantmentsWrapper wrapper = AdvancedEnchantmentsWrapper.of(name, logger);
            byKey.put(wrapper.getKey().toString(), wrapper);
            count++;
        }
        logger.info("[AlkaAnvil] " + count + " encantamentos do AdvancedEnchantments registrados.");
    }

    public AlkaEnchantment get(String key) {
        return byKey.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isFromAdvancedEnchantments(AlkaEnchantment enchantment) {
        return enchantment instanceof AdvancedEnchantmentsWrapper;
    }

    public Map<String, AlkaEnchantment> all() {
        return byKey;
    }
}
