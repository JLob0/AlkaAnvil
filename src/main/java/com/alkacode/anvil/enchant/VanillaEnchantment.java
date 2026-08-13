package com.alkacode.anvil.enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/** Wrapper trivial em cima de {@link Enchantment} - livros usam EnchantmentStorageMeta, o resto usa ItemMeta normal. */
public final class VanillaEnchantment implements AlkaEnchantment {

    private final Enchantment bukkit;
    private final int maxLevelOverride;

    public VanillaEnchantment(Enchantment bukkit, int maxLevelOverride) {
        this.bukkit = bukkit;
        this.maxLevelOverride = maxLevelOverride;
    }

    public Enchantment bukkit() {
        return bukkit;
    }

    @Override
    public NamespacedKey getKey() {
        return bukkit.getKey();
    }

    @Override
    public String getName() {
        return bukkit.getKey().getKey();
    }

    @Override
    public int getMaxLevel() {
        return maxLevelOverride > 0 ? maxLevelOverride : bukkit.getMaxLevel();
    }

    @Override
    public int getLevel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            return bookMeta.getStoredEnchantLevel(bukkit);
        }
        return meta != null ? meta.getEnchantLevel(bukkit) : 0;
    }

    @Override
    public ItemStack setLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            if (level <= 0) {
                bookMeta.removeStoredEnchant(bukkit);
            } else {
                bookMeta.addStoredEnchant(bukkit, level, true);
            }
        } else {
            if (level <= 0) {
                meta.removeEnchant(bukkit);
            } else {
                meta.addEnchant(bukkit, level, true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return bukkit.canEnchantItem(item);
    }
}
