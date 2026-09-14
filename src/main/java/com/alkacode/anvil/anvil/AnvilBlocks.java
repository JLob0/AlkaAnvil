package com.alkacode.anvil.anvil;

import org.bukkit.Material;

/** Checagem compartilhada de "isso e um bloco de bigorna" - usado tanto pela GUI
 * (BigornaListener, pra interceptar o clique) quanto pela protecao de bloco
 * (AnvilProtectionListener, pra impedir quebrar/degradar). */
public final class AnvilBlocks {

    private AnvilBlocks() {
    }

    public static boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
    }
}
