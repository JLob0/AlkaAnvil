package com.alkacode.anvil.stats;

import com.alkacode.anvil.config.AnvilConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/**
 * Rastreia os eventos configurados em item-stats.stats.*.track e incrementa via
 * {@link ItemStatsManager}. So incrementa stats que a categoria do item realmente
 * rastreia (ex: um machado sem BLOCKS_BROKEN na propria categoria nao acumula nada
 * ao quebrar bloco, mesmo que outro item rastreie isso).
 */
public final class ItemStatsListener implements Listener {

    private final AnvilConfig config;
    private final Supplier<ItemStatsManager> managerSupplier;

    public ItemStatsListener(AnvilConfig config, Supplier<ItemStatsManager> managerSupplier) {
        this.config = config;
        this.managerSupplier = managerSupplier;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.itemStatsEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemStatsManager manager = managerSupplier.get();
        if (!manager.isTrackable(tool)) {
            return;
        }
        AnvilConfig.ItemStatsCategory category = manager.categoryOf(tool.getType());
        boolean isOre = event.getBlock().getType().name().endsWith("_ORE");

        if (tracks(category, "BLOCKS_BROKEN")) {
            manager.incrementStat(tool, "BLOCKS_BROKEN", 1);
        }
        if (isOre && tracks(category, "ORES_BROKEN")) {
            manager.incrementStat(tool, "ORES_BROKEN", 1);
        }
        applyBack(player, tool);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!config.itemStatsEnabled()) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        ItemStatsManager manager = managerSupplier.get();
        if (!manager.isTrackable(weapon)) {
            return;
        }
        AnvilConfig.ItemStatsCategory category = manager.categoryOf(weapon.getType());
        String statType = event.getEntity() instanceof Player ? "PLAYER_KILLS" : "MOB_KILLS";
        if (tracks(category, statType)) {
            manager.incrementStat(weapon, statType, 1);
            applyBack(killer, weapon);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!config.itemStatsEnabled()) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        ItemStatsManager manager = managerSupplier.get();
        if (!manager.isTrackable(weapon)) {
            return;
        }
        AnvilConfig.ItemStatsCategory category = manager.categoryOf(weapon.getType());
        if (tracks(category, "DAMAGE_DEALT")) {
            int damage = Math.max(1, (int) Math.round(event.getFinalDamage()));
            manager.incrementStat(weapon, "DAMAGE_DEALT", damage);
            applyBack(attacker, weapon);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!config.itemStatsEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null) {
            return;
        }
        ItemStatsManager manager = managerSupplier.get();
        if (!manager.isTrackable(bow)) {
            return;
        }
        AnvilConfig.ItemStatsCategory category = manager.categoryOf(bow.getType());
        if (tracks(category, "ARROWS_SHOT")) {
            manager.incrementStat(bow, "ARROWS_SHOT", 1);
            applyBack(player, bow);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!config.itemStatsEnabled() || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        ItemStatsManager manager = managerSupplier.get();
        if (!manager.isTrackable(rod)) {
            return;
        }
        AnvilConfig.ItemStatsCategory category = manager.categoryOf(rod.getType());
        if (tracks(category, "FISH_CAUGHT")) {
            manager.incrementStat(rod, "FISH_CAUGHT", 1);
            applyBack(player, rod);
        }
    }

    private boolean tracks(AnvilConfig.ItemStatsCategory category, String statType) {
        return category != null && category.track().contains(statType);
    }

    /** ItemMeta#setItemMeta ja escreve no ItemStack passado, mas reescrever a mao garante persistencia mesmo em forks do Paper que copiam o retorno de getItemInMainHand(). */
    private void applyBack(Player player, ItemStack item) {
        player.getInventory().setItemInMainHand(item);
    }
}
