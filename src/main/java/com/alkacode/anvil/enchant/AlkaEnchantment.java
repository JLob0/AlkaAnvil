package com.alkacode.anvil.enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/**
 * Abstrai um encantamento vanilla ou de terceiro (AdvancedEnchantments) atras de uma
 * interface unica, pra {@link com.alkacode.anvil.anvil.AnvilMergeLogic} nao precisar
 * saber a origem do encantamento que esta combinando/aplicando.
 */
public interface AlkaEnchantment {
    NamespacedKey getKey();

    String getName();

    int getMaxLevel();

    int getLevel(ItemStack item);

    /** Retorna o ItemStack resultante - nem toda origem muta o item in-place (ver AE). */
    ItemStack setLevel(ItemStack item, int level);

    default ItemStack remove(ItemStack item) {
        return setLevel(item, 0);
    }

    boolean canEnchantItem(ItemStack item);
}
