package com.alkacode.anvil.api;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.gui.bigorna.BigornaMenu;
import org.bukkit.entity.Player;

public final class AlkaAnvilAPIProvider implements AlkaAnvilAPI {

    private final AlkaAnvilPlugin plugin;

    public AlkaAnvilAPIProvider(AlkaAnvilPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void openBigorna(Player player) {
        new BigornaMenu(plugin, player).open();
    }
}
