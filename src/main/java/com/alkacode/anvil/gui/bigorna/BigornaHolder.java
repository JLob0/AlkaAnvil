package com.alkacode.anvil.gui.bigorna;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marca o inventario da bigorna custom - guarda a {@link BigornaMenu} dona pra o
 * {@link com.alkacode.anvil.listener.BigornaListener} chamar recompute()/confirm()
 * a partir de qualquer evento de inventario. Mesmo formato do EnderChestHolder
 * (AlkaEnderChest): record puro, nao BaseGui.
 */
public record BigornaHolder(Player player, BigornaMenu menu) implements InventoryHolder {

    @Override
    public @NotNull Inventory getInventory() {
        return menu.getInventory();
    }
}
