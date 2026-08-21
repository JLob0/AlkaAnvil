package com.alkacode.anvil.gui;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.gui.layout.GuiLayoutLoader;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * GUI de admin (`/alkaanvil config`) - visao geral das secoes do config.yml, cada
 * botao abre uma {@link SectionEditGui} com os campos escalares/toggles daquela
 * secao editaveis ao vivo. Secoes cuja estrutura e lista/mapa (conflitos de
 * encantamento, materiais de unit-repair, categorias de item-stats, valores por
 * encantamento) continuam so editaveis via YAML + /alkaanvil reload - decisao
 * tomada com o usuario (AskUserQuestion) pra nao replicar o framework de picker
 * GUIs de ~50 arquivos do CustomAnvil de referencia.
 */
public final class AdminConfigGui extends AnvilGui {

    private final AnvilConfig config;

    public AdminConfigGui(AlkaAnvilPlugin anvilPlugin, Player viewer, AnvilConfig config) {
        super(anvilPlugin, viewer, menuTitle(anvilPlugin), 3, "alkaanvil_admin", "alkaanvil-admin");
        this.config = config;
    }

    private static String menuTitle(AlkaAnvilPlugin anvilPlugin) {
        return anvilPlugin.getMenuConfig().title("alkaanvil-admin.title", null);
    }

    @Override
    public void render() {
        GuiLayoutLoader.GuiLayout layout = applyBorder();

        setAt(layout, 'A', icon("custo-limites"), e -> openSection("custo-limites", costLimitsFields()));
        setAt(layout, 'B', icon("encant-limites"), e -> openSection("encant-limites", enchantLimitFields()));
        setAt(layout, 'C', icon("penalidade-uso"), e -> openSection("penalidade-uso", workPenaltyFields()));
        setAt(layout, 'D', icon("reparo-unidade"), e -> openSection("reparo-unidade", unitRepairFields()));

        setAt(layout, 'E', icon("disenchant-shatter", Map.of(
                        "disenchant-status", config.disenchantEnabled() ? "ativo" : "desativado",
                        "shatter-status", config.shatterEnabled() ? "ativo" : "desativado")),
                e -> openSection("disenchant-shatter", disenchantShatterFields()));

        setAt(layout, 'F', icon("item-stats"), e -> openSection("item-stats", itemStatsFields()));
        setAt(layout, 'G', icon("custo-moeda"), e -> openSection("custo-moeda", monetaryCostFields()));

        setAt(layout, 'H', icon("conflitos-encant",
                Map.of("grupos", String.valueOf(config.enchantConflictGroups().size()))));

        setAt(layout, 'I', icon("cores-rename"), e -> openSection("cores-rename", renameColorsFields()));

        setAt(layout, 'X', menu().item("common.fechar", null), event -> event.getWhoClicked().closeInventory());
    }

    private void openSection(String sectionKey, List<SectionEditGui.Field> fields) {
        String title = menu().text("alkaanvil-admin." + sectionKey + ".section-title", null);
        new SectionEditGui(anvilPlugin, player, title, fields, this::reopen).open();
    }

    private void reopen() {
        new AdminConfigGui(anvilPlugin, player, config).open();
    }

    private List<SectionEditGui.Field> costLimitsFields() {
        return List.of(
                ConfigFields.bool(anvilPlugin, config, "cost-limits.remove-too-expensive", config::removeTooExpensive),
                ConfigFields.intField(anvilPlugin, config, "cost-limits.max-cost", config::maxCost),
                ConfigFields.intField(anvilPlugin, config, "cost-limits.rename-cost", config::renameCost),
                ConfigFields.intField(anvilPlugin, config, "cost-limits.item-repair-cost", config::itemRepairCost),
                ConfigFields.intField(anvilPlugin, config, "cost-limits.unit-repair-cost", config::unitRepairCost)
        );
    }

    private List<SectionEditGui.Field> enchantLimitFields() {
        return List.of(
                ConfigFields.intField(anvilPlugin, config, "enchantment-limits.max-enchants-per-item", config::maxEnchantsPerItem)
        );
    }

    private List<SectionEditGui.Field> workPenaltyFields() {
        return List.of(
                ConfigFields.enumCycle(anvilPlugin, config, "work-penalty.merge-mode",
                        List.of("SUM", "MAX", "AVERAGE", "NONE"), () -> config.workPenaltyMergeMode().name()),
                ConfigFields.intField(anvilPlugin, config, "work-penalty.additive-penalty", config::workPenaltyAdditive)
        );
    }

    private List<SectionEditGui.Field> unitRepairFields() {
        return List.of(
                ConfigFields.doubleField(anvilPlugin, config, "unit-repair.repair-per-unit", config::unitRepairFraction)
        );
    }

    private List<SectionEditGui.Field> disenchantShatterFields() {
        return List.of(
                ConfigFields.bool(anvilPlugin, config, "disenchant.enabled", config::disenchantEnabled),
                ConfigFields.intField(anvilPlugin, config, "disenchant.base-cost", config::disenchantBaseCost),
                ConfigFields.intField(anvilPlugin, config, "disenchant.cost-per-enchant", config::disenchantCostPerEnchant),
                ConfigFields.intField(anvilPlugin, config, "disenchant.cost-per-level", config::disenchantCostPerLevel),
                ConfigFields.bool(anvilPlugin, config, "disenchant.require-permission", config::disenchantRequirePermission),
                ConfigFields.bool(anvilPlugin, config, "shatter.enabled", config::shatterEnabled),
                ConfigFields.intField(anvilPlugin, config, "shatter.base-cost", config::shatterBaseCost),
                ConfigFields.intField(anvilPlugin, config, "shatter.cost-per-level", config::shatterCostPerLevel),
                ConfigFields.bool(anvilPlugin, config, "shatter.require-permission", config::shatterRequirePermission)
        );
    }

    private List<SectionEditGui.Field> itemStatsFields() {
        return List.of(
                ConfigFields.bool(anvilPlugin, config, "item-stats.enabled", config::itemStatsEnabled),
                ConfigFields.bool(anvilPlugin, config, "item-stats.lore.enabled", config::itemStatsLoreEnabled),
                ConfigFields.enumCycle(anvilPlugin, config, "item-stats.lore.position",
                        List.of("TOP", "BOTTOM"), config::itemStatsLorePositionRaw)
        );
    }

    private List<SectionEditGui.Field> monetaryCostFields() {
        return List.of(
                ConfigFields.bool(anvilPlugin, config, "monetary-cost.enabled", config::monetaryCostEnabled),
                ConfigFields.currencyCycle(anvilPlugin, config, "monetary-cost.currency", config::monetaryCurrency,
                        () -> anvilPlugin.getEconomyHook().currencyIds()),
                ConfigFields.doubleField(anvilPlugin, config, "monetary-cost.multipliers.enchantment",
                        () -> config.monetaryMultiplier("enchantment")),
                ConfigFields.doubleField(anvilPlugin, config, "monetary-cost.multipliers.repair",
                        () -> config.monetaryMultiplier("repair")),
                ConfigFields.doubleField(anvilPlugin, config, "monetary-cost.multipliers.rename",
                        () -> config.monetaryMultiplier("rename"))
        );
    }

    private List<SectionEditGui.Field> renameColorsFields() {
        return List.of(
                ConfigFields.bool(anvilPlugin, config, "rename-colors.enabled", config::renameColorsEnabled),
                ConfigFields.bool(anvilPlugin, config, "rename-colors.require-permission", config::renameRequirePermission),
                ConfigFields.intField(anvilPlugin, config, "rename-colors.color-cost", config::renameColorCost)
        );
    }
}
