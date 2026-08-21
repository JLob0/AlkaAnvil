package com.alkacode.anvil.gui;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.gui.layout.GuiLayoutLoader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-GUI generica de uma secao do config.yml, listando campos individualmente
 * clicaveis - reusada por todas as secoes escalares abertas a partir de
 * {@link AdminConfigGui} em vez de uma classe por secao (o padrao do CustomAnvil,
 * ~50 arquivos, e overkill pro escopo escalares/toggles decidido com o usuario;
 * secoes com estrutura de lista/mapa - conflitos de encantamento, materiais de
 * unit-repair, categorias de item-stats - continuam so editaveis via YAML).
 */
public final class SectionEditGui extends AnvilGui {

    /** Cada campo sabe exibir seu valor atual e reagir ao proprio clique - ver {@link ConfigFields}
     * pros construtores prontos (bool/int/double/enum/currency); icone/label vem de menus.yml. */
    public interface Field {
        String label();

        Material icon();

        String currentValueText();

        default List<String> extraLore() {
            return List.of();
        }

        void onClick(SectionEditGui gui);
    }

    private final List<Field> fields;
    private final Runnable onBack;

    public SectionEditGui(AlkaAnvilPlugin anvilPlugin, Player viewer, String title, List<Field> fields, Runnable onBack) {
        super(anvilPlugin, viewer, "<dark_gray>AlkaAnvil <gray>- " + title, 4, "alkaanvil_section", "alkaanvil-section");
        this.fields = fields;
        this.onBack = onBack;
    }

    @Override
    public void render() {
        GuiLayoutLoader.GuiLayout layout = applyBorder();

        List<Integer> slots = layout.findSlots('0');
        for (int i = 0; i < fields.size() && i < slots.size(); i++) {
            Field field = fields.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Valor atual: <white>" + field.currentValueText());
            lore.addAll(field.extraLore());
            lore.add("");
            lore.add("<green>Clique para editar");
            ItemStack item = createItem(field.icon(), field.label(), lore.toArray(new String[0]));
            setItem(slots.get(i), item, e -> field.onClick(this));
        }

        backButton(layout, onBack);
    }

    public AlkaAnvilPlugin anvilPlugin() {
        return anvilPlugin;
    }

    public Player viewer() {
        return player;
    }

    /** {@link #refresh()} e protected em BaseGui (mesmo pacote/subclasse) - {@link ConfigFields}
     * vive em com.alkacode.anvil.gui mas nao estende BaseGui, entao precisa desse wrapper publico. */
    public void refreshUi() {
        refresh();
    }
}
