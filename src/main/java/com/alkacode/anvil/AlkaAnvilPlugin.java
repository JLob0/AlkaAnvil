package com.alkacode.anvil;

import com.alkacode.anvil.anvil.AnvilMergeLogic;
import com.alkacode.anvil.anvil.PendingAnvilOperation;
import com.alkacode.anvil.command.AlkaAnvilCommand;
import com.alkacode.anvil.command.EnchantCommand;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.config.MenuConfig;
import com.alkacode.anvil.disenchant.DisenchantManager;
import com.alkacode.anvil.economy.AlkaEconomyHook;
import com.alkacode.anvil.enchant.AlkaEnchantmentRegistry;
import com.alkacode.anvil.gui.AdminConfigGui;
import com.alkacode.anvil.gui.ChatInputManager;
import com.alkacode.anvil.gui.layout.GuiLayoutLoader;
import com.alkacode.anvil.listener.AnvilClickListener;
import com.alkacode.anvil.listener.ChatInputListener;
import com.alkacode.anvil.listener.PrepareAnvilListener;
import com.alkacode.anvil.stats.ItemStatsListener;
import com.alkacode.anvil.stats.ItemStatsManager;
import com.alkacode.core.plugin.AlkaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla mecanicas da bigorna (custo, limites, conflitos, reparo, disenchant,
 * estatisticas de item) - substituto proprietario do CustomAnvil de terceiro. Ver
 * ALKANETWORKING.md/memoria project-alkaanvil pro racional das decisoes de design.
 *
 * <p>Registry/mergeLogic/economyHook/disenchantManager/itemStatsManager sao campos
 * MUTAVEIS reconstruidos em {@link #reloadAll()} - os listeners sao registrados uma
 * UNICA vez em {@link #onPluginEnable()} e leem esses campos via
 * {@link java.util.function.Supplier}, nunca guardando uma copia fixa. Chamar
 * {@code registerEvents} de novo no reload duplicaria todo handler.
 */
public final class AlkaAnvilPlugin extends AlkaPlugin {

    private final Map<UUID, PendingAnvilOperation> pendingOperations = new ConcurrentHashMap<>();
    private AnvilConfig config;
    private AlkaEnchantmentRegistry registry;
    private AlkaEconomyHook economyHook;
    private AnvilMergeLogic mergeLogic;
    private DisenchantManager disenchantManager;
    private ItemStatsManager itemStatsManager;
    private final ChatInputManager chatInputManager = new ChatInputManager();
    private MenuConfig menuConfig;
    private GuiLayoutLoader guiLayoutLoader;

    @Override
    protected void onPluginEnable() {
        config = new AnvilConfig(this);
        menuConfig = new MenuConfig(this);
        guiLayoutLoader = new GuiLayoutLoader(this);
        rebuild();

        getServer().getPluginManager().registerEvents(
                new PrepareAnvilListener(this::getMergeLogic, this::getDisenchantManager, config, pendingOperations), this);
        getServer().getPluginManager().registerEvents(
                new AnvilClickListener(this, config, this::getEconomyHook, pendingOperations), this);
        getServer().getPluginManager().registerEvents(new ItemStatsListener(config, this::getItemStatsManager), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this, chatInputManager), this);

        AlkaAnvilCommand command = new AlkaAnvilCommand(this, config);
        getCommand("alkaanvil").setExecutor(command);
        getCommand("alkaanvil").setTabCompleter(command);

        EnchantCommand enchantCommand = new EnchantCommand(this, config);
        getCommand("encantar").setExecutor(enchantCommand);
        getCommand("encantar").setTabCompleter(enchantCommand);

        getLogger().info("AlkaAnvil habilitado (" + registry.all().size() + " encantamentos registrados).");
    }

    @Override
    protected void onPluginDisable() {
        // Sem estado persistente proprio - custos pendentes sao so cache em memoria
        // (limpo naturalmente quando a bigorna fecha), nada a descarregar. Stats de
        // item vivem no PDC do proprio item, nao num banco - nada a fechar aqui.
    }

    private void rebuild() {
        registry = new AlkaEnchantmentRegistry(this, config.perEnchantLevelOverrides());
        economyHook = new AlkaEconomyHook();
        mergeLogic = new AnvilMergeLogic(this, config, registry, economyHook);
        disenchantManager = new DisenchantManager(config, registry);
        itemStatsManager = new ItemStatsManager(this, config);
    }

    public AnvilMergeLogic getMergeLogic() {
        return mergeLogic;
    }

    public AlkaEconomyHook getEconomyHook() {
        return economyHook;
    }

    public DisenchantManager getDisenchantManager() {
        return disenchantManager;
    }

    public ItemStatsManager getItemStatsManager() {
        return itemStatsManager;
    }

    public AlkaEnchantmentRegistry getRegistry() {
        return registry;
    }

    public AnvilConfig getAnvilConfig() {
        return config;
    }

    public void openAdminGui(org.bukkit.entity.Player player) {
        new AdminConfigGui(this, player, config).open();
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public GuiLayoutLoader getGuiLayoutLoader() {
        return guiLayoutLoader;
    }

    /** AE pode ter mudado limites/enchants desde o ultimo load - reconstroi tudo, so nao re-registra listeners/comando. */
    public void reloadAll() {
        config.reload();
        menuConfig.reload();
        pendingOperations.clear();
        rebuild();
    }
}
