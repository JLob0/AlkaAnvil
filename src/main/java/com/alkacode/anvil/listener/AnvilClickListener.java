package com.alkacode.anvil.listener;

import com.alkacode.anvil.anvil.PendingAnvilOperation;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.economy.AlkaEconomyHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cobra o custo em moeda (quando monetary-cost esta habilitado) e devolve o
 * "segundo resultado" de disenchant/shatter (item limpo / livro remanescente) ao
 * slot 0, no momento em que o jogador retira o item do slot de resultado. A bigorna
 * vanilla so tem 1 slot de saida - o segundo resultado precisa ser reescrito 1 tick
 * DEPOIS do clique, porque a propria logica vanilla da bigorna limpa os slots 0/1
 * como parte do mesmo clique que entrega o resultado (reescrever no mesmo tick seria
 * sobrescrito por ela logo em seguida).
 */
public final class AnvilClickListener implements Listener {

    private static final int RESULT_SLOT = 2;
    private static final int LEFT_SLOT = 0;

    private final JavaPlugin plugin;
    private final AnvilConfig config;
    private final Supplier<AlkaEconomyHook> economyHookSupplier;
    private final Map<UUID, PendingAnvilOperation> pendingOperations;

    public AnvilClickListener(JavaPlugin plugin, AnvilConfig config, Supplier<AlkaEconomyHook> economyHookSupplier,
                               Map<UUID, PendingAnvilOperation> pendingOperations) {
        this.plugin = plugin;
        this.config = config;
        this.economyHookSupplier = economyHookSupplier;
        this.pendingOperations = pendingOperations;
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) {
            return;
        }
        if (event.getSlot() != RESULT_SLOT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }

        PendingAnvilOperation operation = pendingOperations.get(player.getUniqueId());
        if (operation == null) {
            return;
        }

        if (operation.cost().isMonetary() && !player.hasPermission("alkaanvil.bypass.cost")) {
            AlkaEconomyHook economyHook = economyHookSupplier.get();
            if (!economyHook.has(player, operation.cost().currencyId(), operation.cost().currencyAmount())) {
                event.setCancelled(true);
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize(config.prefix() + config.message("not-enough-currency")));
                return;
            }
            economyHook.remove(player, operation.cost().currencyId(), operation.cost().currencyAmount());
        }

        pendingOperations.remove(player.getUniqueId());

        ItemStack leftover = operation.leftoverItem();
        if (leftover != null) {
            ItemStack toRestore = leftover.clone();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getOpenInventory().getTopInventory() instanceof AnvilInventory) {
                        anvil.setItem(LEFT_SLOT, toRestore);
                    } else {
                        var overflow = player.getInventory().addItem(toRestore);
                        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                    }
                }
            }.runTask(plugin);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory() instanceof AnvilInventory) {
            pendingOperations.remove(event.getPlayer().getUniqueId());
        }
    }
}
