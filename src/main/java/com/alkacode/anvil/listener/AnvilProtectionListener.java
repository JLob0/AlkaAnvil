package com.alkacode.anvil.listener;

import com.alkacode.anvil.anvil.AnvilBlocks;
import com.alkacode.anvil.config.AnvilConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Bigorna infinita: bloco nao pode ser quebrado (picareta) nem degradar/quebrar ao
 * cair. Trazido do AlkaEssentials (qol.anvil.infinite) - Essentials nao mexe mais em
 * nada de bigorna, esse dominio agora e 100% do AlkaAnvil.
 */
public final class AnvilProtectionListener implements Listener {

    private final AnvilConfig config;

    public AnvilProtectionListener(AnvilConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onAnvilBreak(BlockBreakEvent event) {
        if (!config.anvilBlockInfinite() || event.getPlayer().hasPermission("alkaanvil.bypass.break")) {
            return;
        }
        if (AnvilBlocks.isAnvil(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvilFallDegrade(EntityChangeBlockEvent event) {
        if (!config.anvilBlockInfinite()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof FallingBlock falling && AnvilBlocks.isAnvil(falling.getBlockData().getMaterial())) {
            event.setCancelled(true);
        }
    }
}
