package com.alkacode.anvil.gui;

import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GUI de admin so-leitura (`/alkaanvil config`) - mostra os valores atuais de cada
 * secao do config.yml pro admin conferir sem abrir o arquivo. Edicao continua
 * sendo via YAML + /alkaanvil reload - sub-GUIs de edicao de cada secao (como o
 * spec original desenhou) ficam pra uma proxima iteracao, marcadas como opcionais
 * no MVP desde o desenho original.
 */
public final class AdminConfigGui extends BaseGui {

    private final AnvilConfig config;

    public AdminConfigGui(JavaPlugin plugin, Player viewer, AnvilConfig config) {
        super(plugin, viewer, "<dark_gray>AlkaAnvil <gray>- Config", 3, "alkaanvil_admin");
        this.config = config;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        setItem(10, createItem(Material.EXPERIENCE_BOTTLE, "<green>Limites de Custo",
                "<gray>remove-too-expensive: <white>" + config.removeTooExpensive(),
                "<gray>max-cost: <white>" + config.maxCost(),
                "<gray>rename-cost: <white>" + config.renameCost(),
                "<gray>item-repair-cost: <white>" + config.itemRepairCost(),
                "<gray>unit-repair-cost: <white>" + config.unitRepairCost()));

        setItem(11, createItem(Material.ENCHANTED_BOOK, "<green>Limites de Encantamento",
                "<gray>max-enchants-per-item: <white>" + config.maxEnchantsPerItem(),
                "<gray>overrides configurados: <white>" + config.perEnchantLevelOverrides().size()));

        setItem(12, createItem(Material.OAK_FENCE, "<green>Conflitos de Encantamento",
                "<gray>grupos configurados: <white>" + config.enchantConflictGroups().size()));

        setItem(13, createItem(Material.DIAMOND, "<green>Reparo por Unidade",
                "<gray>materiais configurados: <white>" + config.unitRepairMaterials().size(),
                "<gray>repair-per-unit: <white>" + config.unitRepairFraction()));

        setItem(14, createItem(Material.BOOK, "<green>Disenchant / Shatter",
                "<gray>disenchant: <white>" + (config.disenchantEnabled() ? "ativo" : "desativado"),
                "<gray>shatter: <white>" + (config.shatterEnabled() ? "ativo" : "desativado")));

        setItem(15, createItem(Material.PAPER, "<green>Estatisticas de Itens",
                "<gray>item-stats: <white>" + (config.itemStatsEnabled() ? "ativo" : "desativado"),
                "<gray>categorias: <white>" + config.itemStatsCategories().size()));

        setItem(16, createItem(Material.GOLD_INGOT, "<green>Custo em Moeda",
                "<gray>monetary-cost: <white>" + (config.monetaryCostEnabled() ? "ativo" : "desativado"),
                "<gray>moeda: <white>" + config.monetaryCurrency()));

        setItem(22, createItem(Material.INK_SAC, "<green>Cores no Rename",
                "<gray>rename-colors: <white>" + (config.renameColorsEnabled() ? "ativo" : "desativado"),
                "<gray>require-permission: <white>" + config.renameRequirePermission()));

        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), event -> event.getWhoClicked().closeInventory());
    }
}
