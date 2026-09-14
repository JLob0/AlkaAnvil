package com.alkacode.anvil.anvil;

import com.alkacode.anvil.config.AnvilConfig;
import org.bukkit.entity.Player;

/**
 * Resolve o cap de custo (max-cost) de uma operacao de bigorna. A GUI propria
 * (BigornaMenu) nao depende mais do limite de protocolo vanilla de 39 niveis
 * ("Too Expensive") - esse cap so existia pra contornar a UI da AnvilInventory
 * real, que nao existe mais.
 */
public final class AnvilXpUtil {

    private AnvilXpUtil() {
    }

    public static int applyCap(AnvilConfig config, Player player, int rawXpLevels) {
        if (player.hasPermission("alkaanvil.bypass.cost")) {
            return rawXpLevels;
        }
        int maxCost = config.maxCost();
        return maxCost > 0 ? Math.min(rawXpLevels, maxCost) : rawXpLevels;
    }
}
