package com.alkacode.anvil.economy;

import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Soft-dependency pro AlkaEconomy - so usado quando monetary-cost.enabled=true no
 * config.yml. Diferente do AdvancedEnchantments, o AlkaEconomy E outro plugin
 * Alka* (mesmo `com.alkacode.*`), entao um import direto e seguro CONTANTO que o
 * softdepend no plugin.yml garanta que, se presente, ele carregue antes - nunca
 * assumir presenca sem checar {@link #isAvailable()} primeiro.
 */
public final class AlkaEconomyHook {

    private final EconomyManager economyManager;

    public AlkaEconomyHook() {
        this.economyManager = resolve();
    }

    private static EconomyManager resolve() {
        if (Bukkit.getPluginManager().getPlugin("AlkaEconomy") instanceof AlkaEconomyPlugin plugin && plugin.isEnabled()) {
            return plugin.getEconomyManager();
        }
        return null;
    }

    public boolean isAvailable() {
        return economyManager != null;
    }

    public boolean has(Player player, String currencyId, double amount) {
        return isAvailable() && economyManager.isValidCurrency(currencyId)
                && economyManager.has(player.getUniqueId(), currencyId, amount);
    }

    public void remove(Player player, String currencyId, double amount) {
        if (isAvailable()) {
            economyManager.removeBalance(player.getUniqueId(), currencyId, amount);
        }
    }
}
