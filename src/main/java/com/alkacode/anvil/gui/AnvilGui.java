package com.alkacode.anvil.gui;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.gui.layout.GuiLayoutLoader;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Base das GUIs de admin do AlkaAnvil. Icone/texto/layout de cada GUI concreta
 * vem de menus.yml/gui-layouts.yml (ver R8 no CLAUDE.md) - essa classe so
 * oferece os helpers de wiring (applyBorder/setAt/icon).
 */
public abstract class AnvilGui extends BaseGui {

    protected final AlkaAnvilPlugin anvilPlugin;
    protected final String layoutId;

    protected AnvilGui(AlkaAnvilPlugin anvilPlugin, Player player, String title, int rows, String id, String layoutId) {
        super(anvilPlugin, player, title, rows, id);
        this.anvilPlugin = anvilPlugin;
        this.layoutId = layoutId;
    }

    protected com.alkacode.anvil.config.MenuConfig menu() {
        return anvilPlugin.getMenuConfig();
    }

    /** Aplica o layout do YML (gui-layouts.yml, chave layoutId): preenche a borda (#)
     * com o icone de menus.yml.common.border e retorna o layout. */
    protected GuiLayoutLoader.GuiLayout applyBorder() {
        GuiLayoutLoader.GuiLayout layout = anvilPlugin.getGuiLayoutLoader().getLayout(layoutId);
        ItemStack border = menu().item("common.border", null);
        layout(layout.layout(), Map.of('#', border), null);
        return layout;
    }

    protected void setAt(GuiLayoutLoader.GuiLayout layout, char c, ItemStack item, Consumer<InventoryClickEvent> action) {
        int slot = layout.firstSlot(c);
        if (slot >= 0) setItem(slot, item, action);
    }

    protected void setAt(GuiLayoutLoader.GuiLayout layout, char c, ItemStack item) {
        setAt(layout, c, item, null);
    }

    /** Icone de menus.yml.<layoutId>.<path> com placeholders. */
    protected ItemStack icon(String path, Map<String, String> placeholders) {
        return menu().item(layoutId + "." + path, placeholders);
    }

    protected ItemStack icon(String path) {
        return icon(path, null);
    }

    /** Botão voltar no slot 'V' do layout. */
    protected void backButton(GuiLayoutLoader.GuiLayout layout, Runnable open) {
        setAt(layout, 'V', menu().item("common.voltar", null), event -> open.run());
    }
}
