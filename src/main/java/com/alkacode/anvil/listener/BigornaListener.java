package com.alkacode.anvil.listener;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.anvil.AnvilBlocks;
import com.alkacode.anvil.gui.bigorna.BigornaHolder;
import com.alkacode.anvil.gui.bigorna.BigornaMenu;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Intercepta o bloco de bigorna real (abre {@link BigornaMenu} no lugar da
 * AnvilInventory vanilla - ela nunca mais abre) e trata clique/drag/close do
 * inventario dela. So os slots A/B (item do jogador) passam livre; borda/resultado/
 * sobra sao sempre controlados aqui. Mesmo padrao do EnderChestListener
 * (holder proprio, nao BaseGui - ver javadoc de {@link BigornaMenu}).
 */
public final class BigornaListener implements Listener {

    private final AlkaAnvilPlugin plugin;

    public BigornaListener(AlkaAnvilPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAnvilInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !AnvilBlocks.isAnvil(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().isBlock()) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        new BigornaMenu(plugin, player).open();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BigornaHolder holder)) {
            return;
        }
        BigornaMenu menu = holder.menu();

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            // inventario do proprio jogador - livre (inclusive shift-click pra dentro de A/B)
            schedule(menu::recompute);
            return;
        }

        int slot = event.getSlot();
        if (slot == menu.getSlotA() || slot == menu.getSlotB()) {
            schedule(menu::recompute);
            return;
        }

        event.setCancelled(true);
        if (slot == menu.getSlotResult() || slot == menu.getSlotLeftover()) {
            menu.confirm();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BigornaHolder holder)) {
            return;
        }
        BigornaMenu menu = holder.menu();
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                continue;
            }
            if (rawSlot != menu.getSlotA() && rawSlot != menu.getSlotB()) {
                event.setCancelled(true);
                return;
            }
        }
        schedule(menu::recompute);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BigornaHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        BigornaMenu menu = holder.menu();
        Inventory inv = event.getInventory();
        returnItem(inv, menu.getSlotA(), player);
        returnItem(inv, menu.getSlotB(), player);
    }

    private void returnItem(Inventory inv, int slot, Player player) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        inv.setItem(slot, null);
        var overflow = player.getInventory().addItem(item);
        overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
    }

    private void schedule(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
