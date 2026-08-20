package com.alkacode.anvil.gui;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * GUI de admin (`/alkaanvil config`) - visao geral das secoes do config.yml, cada
 * botao abre uma {@link SectionEditGui} com os campos escalares/toggles daquela
 * secao editaveis ao vivo. Secoes cuja estrutura e lista/mapa (conflitos de
 * encantamento, materiais de unit-repair, categorias de item-stats, valores por
 * encantamento) continuam so editaveis via YAML + /alkaanvil reload - decisao
 * tomada com o usuario (AskUserQuestion) pra nao replicar o framework de picker
 * GUIs de ~50 arquivos do CustomAnvil de referencia.
 */
public final class AdminConfigGui extends BaseGui {

    private final AlkaAnvilPlugin anvilPlugin;
    private final AnvilConfig config;

    public AdminConfigGui(AlkaAnvilPlugin anvilPlugin, Player viewer, AnvilConfig config) {
        super(anvilPlugin, viewer, "<dark_gray>AlkaAnvil <gray>- Config", 3, "alkaanvil_admin");
        this.anvilPlugin = anvilPlugin;
        this.config = config;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        setItem(10, createItem(Material.EXPERIENCE_BOTTLE, "<green>Limites de Custo",
                "<gray>remove-too-expensive, max-cost,",
                "<gray>rename-cost, item/unit-repair-cost"),
                e -> openSection("Limites de Custo", costLimitsFields()));

        setItem(11, createItem(Material.ENCHANTED_BOOK, "<green>Limites de Encantamento",
                "<gray>max-enchants-per-item",
                "<gray>(overrides por-encantamento: YAML)"),
                e -> openSection("Limites de Encantamento", enchantLimitFields()));

        setItem(12, createItem(Material.ANVIL, "<green>Penalidade de Uso",
                "<gray>merge-mode, additive-penalty"),
                e -> openSection("Penalidade de Uso", workPenaltyFields()));

        setItem(13, createItem(Material.DIAMOND, "<green>Reparo por Unidade",
                "<gray>repair-per-unit",
                "<gray>(materiais por item: YAML)"),
                e -> openSection("Reparo por Unidade", unitRepairFields()));

        setItem(14, createItem(Material.BOOK, "<green>Disenchant / Shatter",
                "<gray>disenchant: <white>" + (config.disenchantEnabled() ? "ativo" : "desativado"),
                "<gray>shatter: <white>" + (config.shatterEnabled() ? "ativo" : "desativado")),
                e -> openSection("Disenchant / Shatter", disenchantShatterFields()));

        setItem(15, createItem(Material.PAPER, "<green>Estatisticas de Itens",
                "<gray>enabled, lore.enabled, lore.position",
                "<gray>(categorias/formato de lore: YAML)"),
                e -> openSection("Estatisticas de Itens", itemStatsFields()));

        setItem(16, createItem(Material.GOLD_INGOT, "<green>Custo em Moeda",
                "<gray>enabled, moeda, multiplicadores"),
                e -> openSection("Custo em Moeda", monetaryCostFields()));

        setItem(19, createItem(Material.OAK_FENCE, "<green>Conflitos de Encantamento",
                "<gray>grupos configurados: <white>" + config.enchantConflictGroups().size(),
                "",
                "<gray>Lista de grupos - edite via config.yml",
                "<gray>+ /alkaanvil reload."));

        setItem(20, createItem(Material.INK_SAC, "<green>Cores no Rename",
                "<gray>enabled, require-permission, color-cost"),
                e -> openSection("Cores no Rename", renameColorsFields()));

        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), event -> event.getWhoClicked().closeInventory());
    }

    private void openSection(String title, List<SectionEditGui.Field> fields) {
        new SectionEditGui(anvilPlugin, player, title, fields, this::reopen).open();
    }

    private void reopen() {
        new AdminConfigGui(anvilPlugin, player, config).open();
    }

    private List<SectionEditGui.Field> costLimitsFields() {
        return List.of(
                ConfigFields.bool("Remover Too Expensive", Material.NAME_TAG, config,
                        "cost-limits.remove-too-expensive", config::removeTooExpensive),
                ConfigFields.intField("Custo Maximo (XP)", Material.EXPERIENCE_BOTTLE, config,
                        "cost-limits.max-cost", config::maxCost),
                ConfigFields.intField("Custo de Renomear", Material.NAME_TAG, config,
                        "cost-limits.rename-cost", config::renameCost),
                ConfigFields.intField("Custo Reparo (item+item)", Material.ANVIL, config,
                        "cost-limits.item-repair-cost", config::itemRepairCost),
                ConfigFields.intField("Custo Reparo (material)", Material.IRON_INGOT, config,
                        "cost-limits.unit-repair-cost", config::unitRepairCost)
        );
    }

    private List<SectionEditGui.Field> enchantLimitFields() {
        return List.of(
                ConfigFields.intField("Max. Encantamentos por Item", Material.ENCHANTED_BOOK, config,
                        "enchantment-limits.max-enchants-per-item", config::maxEnchantsPerItem)
        );
    }

    private List<SectionEditGui.Field> workPenaltyFields() {
        return List.of(
                ConfigFields.enumCycle("Modo de Combinacao", Material.ANVIL, config,
                        "work-penalty.merge-mode", List.of("SUM", "MAX", "AVERAGE", "NONE"),
                        () -> config.workPenaltyMergeMode().name()),
                ConfigFields.intField("Penalidade Extra Fixa", Material.REDSTONE, config,
                        "work-penalty.additive-penalty", config::workPenaltyAdditive)
        );
    }

    private List<SectionEditGui.Field> unitRepairFields() {
        return List.of(
                ConfigFields.doubleField("Fracao Reparada por Unidade", Material.DIAMOND, config,
                        "unit-repair.repair-per-unit", config::unitRepairFraction)
        );
    }

    private List<SectionEditGui.Field> disenchantShatterFields() {
        return List.of(
                ConfigFields.bool("Disenchant Ativo", Material.LIME_DYE, config,
                        "disenchant.enabled", config::disenchantEnabled),
                ConfigFields.intField("Disenchant: Custo Base", Material.BOOK, config,
                        "disenchant.base-cost", config::disenchantBaseCost),
                ConfigFields.intField("Disenchant: Custo p/ Encant.", Material.BOOK, config,
                        "disenchant.cost-per-enchant", config::disenchantCostPerEnchant),
                ConfigFields.intField("Disenchant: Custo p/ Nivel", Material.BOOK, config,
                        "disenchant.cost-per-level", config::disenchantCostPerLevel),
                ConfigFields.bool("Disenchant Requer Permissao", Material.TRIPWIRE_HOOK, config,
                        "disenchant.require-permission", config::disenchantRequirePermission),
                ConfigFields.bool("Shatter Ativo", Material.LIME_DYE, config,
                        "shatter.enabled", config::shatterEnabled),
                ConfigFields.intField("Shatter: Custo Base", Material.BOOK, config,
                        "shatter.base-cost", config::shatterBaseCost),
                ConfigFields.intField("Shatter: Custo p/ Nivel", Material.BOOK, config,
                        "shatter.cost-per-level", config::shatterCostPerLevel),
                ConfigFields.bool("Shatter Requer Permissao", Material.TRIPWIRE_HOOK, config,
                        "shatter.require-permission", config::shatterRequirePermission)
        );
    }

    private List<SectionEditGui.Field> itemStatsFields() {
        return List.of(
                ConfigFields.bool("Estatisticas Ativas", Material.PAPER, config,
                        "item-stats.enabled", config::itemStatsEnabled),
                ConfigFields.bool("Lore de Estatisticas Ativa", Material.WRITABLE_BOOK, config,
                        "item-stats.lore.enabled", config::itemStatsLoreEnabled),
                ConfigFields.enumCycle("Posicao da Lore", Material.COMPARATOR, config,
                        "item-stats.lore.position", List.of("TOP", "BOTTOM"), config::itemStatsLorePositionRaw)
        );
    }

    private List<SectionEditGui.Field> monetaryCostFields() {
        return List.of(
                ConfigFields.bool("Custo em Moeda Ativo", Material.GOLD_INGOT, config,
                        "monetary-cost.enabled", config::monetaryCostEnabled),
                ConfigFields.currencyCycle("Moeda", Material.SUNFLOWER, config,
                        "monetary-cost.currency", config::monetaryCurrency,
                        () -> anvilPlugin.getEconomyHook().currencyIds()),
                ConfigFields.doubleField("Multiplicador: Encantar", Material.ENCHANTED_BOOK, config,
                        "monetary-cost.multipliers.enchantment", () -> config.monetaryMultiplier("enchantment")),
                ConfigFields.doubleField("Multiplicador: Reparar", Material.ANVIL, config,
                        "monetary-cost.multipliers.repair", () -> config.monetaryMultiplier("repair")),
                ConfigFields.doubleField("Multiplicador: Renomear", Material.NAME_TAG, config,
                        "monetary-cost.multipliers.rename", () -> config.monetaryMultiplier("rename"))
        );
    }

    private List<SectionEditGui.Field> renameColorsFields() {
        return List.of(
                ConfigFields.bool("Cores no Rename Ativas", Material.INK_SAC, config,
                        "rename-colors.enabled", config::renameColorsEnabled),
                ConfigFields.bool("Requer Permissao", Material.TRIPWIRE_HOOK, config,
                        "rename-colors.require-permission", config::renameRequirePermission),
                ConfigFields.intField("Custo de Usar Cor", Material.EXPERIENCE_BOTTLE, config,
                        "rename-colors.color-cost", config::renameColorCost)
        );
    }
}
