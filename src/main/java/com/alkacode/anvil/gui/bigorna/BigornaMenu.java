package com.alkacode.anvil.gui.bigorna;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.anvil.AnvilCost;
import com.alkacode.anvil.anvil.AnvilMergeLogic;
import com.alkacode.anvil.anvil.AnvilUseType;
import com.alkacode.anvil.anvil.AnvilXpUtil;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.disenchant.DisenchantManager;
import com.alkacode.anvil.economy.AlkaEconomyHook;
import com.alkacode.anvil.gui.layout.GuiLayoutLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GUI propria da bigorna - substitui a AnvilInventory vanilla por completo. De
 * proposito NAO estende BaseGui: os slots A/B precisam aceitar item
 * arrastado/retirado livremente (shift-click incluso), e o GuiListener do Core
 * cancela isso incondicionalmente pra qualquer holder BaseGui. Mesmo padrao de
 * holder+listener proprio ja usado no AlkaEnderChest (EnderChestHolder/
 * EnderChestListener) pelo mesmo motivo.
 *
 * <p>A matematica em si (merge/reparo/disenchant/shatter) e 100% reaproveitada de
 * {@link AnvilMergeLogic}/{@link DisenchantManager} - as duas ja eram desacopladas
 * de UI (recebem ItemStack puro, nunca dependeram de AnvilView). So a "casca" mudou.
 */
public final class BigornaMenu {

    private static final String LAYOUT_ID = "alkaanvil-bigorna";

    private final AlkaAnvilPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final int slotA;
    private final int slotB;
    private final int slotCost;
    private final int slotResult;
    private final int slotLeftover;

    private PendingResult pending;

    private record PendingResult(AnvilCost cost, AnvilUseType useType, ItemStack result, ItemStack leftover) {
    }

    public BigornaMenu(AlkaAnvilPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout(LAYOUT_ID);
        List<Integer> free = layout.findSlots('_');
        this.slotA = free.get(0);
        this.slotB = free.get(1);
        this.slotCost = layout.firstSlot('C');
        this.slotResult = layout.firstSlot('R');
        this.slotLeftover = layout.firstSlot('L');

        String title = plugin.getMenuConfig().title(LAYOUT_ID + ".title", null);
        this.inventory = Bukkit.createInventory(new BigornaHolder(player, this), layout.rows() * 9,
                MiniMessage.miniMessage().deserialize(title == null || title.isEmpty() ? "Bigorna" : title));

        ItemStack border = plugin.getMenuConfig().item("common.border", null);
        for (int slot : layout.findSlots('#')) {
            inventory.setItem(slot, border);
        }
        recompute();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getSlotA() {
        return slotA;
    }

    public int getSlotB() {
        return slotB;
    }

    public int getSlotCost() {
        return slotCost;
    }

    public int getSlotResult() {
        return slotResult;
    }

    /** -1 se o layout nao definir um slot 'L' - a sobra de disenchant/shatter so
     * deixa de ter preview visual, ainda e entregue normalmente ao confirmar. */
    public int getSlotLeftover() {
        return slotLeftover;
    }

    /**
     * Reavalia A/B (disenchant -> shatter -> merge, mesma ordem de prioridade do
     * fluxo vanilla antigo) e atualiza os slots de preview. Chamado apos qualquer
     * clique/drag que possa ter mudado A/B.
     */
    public void recompute() {
        ItemStack left = inventory.getItem(slotA);
        ItemStack right = inventory.getItem(slotB);
        AnvilConfig config = plugin.getAnvilConfig();
        DisenchantManager disenchantManager = plugin.getDisenchantManager();

        DisenchantManager.DisenchantResult disenchant = disenchantManager.tryDisenchant(player, left, right);
        if (disenchant != null) {
            int cappedXp = AnvilXpUtil.applyCap(config, player, Math.max(1, disenchant.cost()));
            pending = new PendingResult(buildCost(config, cappedXp, "repair"), AnvilUseType.DISENCHANT,
                    disenchant.resultBook(), disenchant.leftoverItem());
            render();
            return;
        }

        DisenchantManager.ShatterResult shatter = disenchantManager.tryShatter(player, left, right);
        if (shatter != null) {
            int cappedXp = AnvilXpUtil.applyCap(config, player, Math.max(1, shatter.cost()));
            pending = new PendingResult(buildCost(config, cappedXp, "repair"), AnvilUseType.SHATTER,
                    shatter.resultBook(), shatter.leftoverBook());
            render();
            return;
        }

        AnvilMergeLogic.MergeResult merge = plugin.getMergeLogic().compute(player, left, right);
        if (merge != null) {
            pending = new PendingResult(merge.cost(), merge.useType(), merge.item(), null);
            render();
            return;
        }

        pending = null;
        render();
    }

    private AnvilCost buildCost(AnvilConfig config, int cappedXp, String monetaryOperation) {
        if (!config.monetaryCostEnabled()) {
            return AnvilCost.xpOnly(cappedXp);
        }
        double amount = cappedXp * config.monetaryMultiplier(monetaryOperation);
        return AnvilCost.monetary(cappedXp, amount, config.monetaryCurrency());
    }

    private void render() {
        if (pending == null) {
            ItemStack placeholder = plugin.getMenuConfig().item(LAYOUT_ID + ".vazio", null);
            inventory.setItem(slotResult, placeholder);
            setLeftoverSlot(placeholder.clone());
            inventory.setItem(slotCost, plugin.getMenuConfig().item(LAYOUT_ID + ".custo-vazio", null));
            return;
        }

        ItemStack resultIcon = pending.result().clone();
        ItemMeta meta = resultIcon.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.addAll(plugin.getMenuConfig().lore(LAYOUT_ID + ".resultado-confirmar", null));
        meta.lore(lore);
        resultIcon.setItemMeta(meta);
        inventory.setItem(slotResult, resultIcon);

        setLeftoverSlot(pending.leftover() != null
                ? pending.leftover().clone()
                : plugin.getMenuConfig().item(LAYOUT_ID + ".vazio", null));

        inventory.setItem(slotCost, buildCostIcon(pending.cost()));
    }

    /** So renderiza se o layout definir um slot 'L' - sem ele, a sobra de
     * disenchant/shatter fica sem preview mas continua sendo entregue no confirm(). */
    private void setLeftoverSlot(ItemStack item) {
        if (slotLeftover >= 0) {
            inventory.setItem(slotLeftover, item);
        }
    }

    /** Card de status do custo (slot C) - sempre mostra o valor exato necessario, o
     * que o jogador tem agora, e (se der) o quanto vai sobrar depois de pagar. */
    private ItemStack buildCostIcon(AnvilCost cost) {
        boolean bypass = player.hasPermission("alkaanvil.bypass.cost");

        if (cost.isMonetary()) {
            String currency = cost.currencyId();
            double needed = cost.currencyAmount();

            if (bypass) {
                return plugin.getMenuConfig().item(LAYOUT_ID + ".custo-bypass",
                        Map.of("necessario", formatAmount(needed) + " " + currency));
            }

            double have = plugin.getEconomyHook().getBalance(player, currency);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("necessario", formatAmount(needed) + " " + currency);
            placeholders.put("tenho", formatAmount(have) + " " + currency);
            if (have >= needed) {
                placeholders.put("sobra", formatAmount(have - needed) + " " + currency);
                return plugin.getMenuConfig().item(LAYOUT_ID + ".custo-suficiente", placeholders);
            }
            placeholders.put("falta", formatAmount(needed - have) + " " + currency);
            return plugin.getMenuConfig().item(LAYOUT_ID + ".custo-insuficiente", placeholders);
        }

        int needed = cost.xpLevels();
        int have = player.getLevel();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("necessario", needed + " niveis de XP");
        placeholders.put("tenho", have + " niveis de XP");
        if (have >= needed) {
            placeholders.put("sobra", (have - needed) + " niveis de XP");
            return plugin.getMenuConfig().item(LAYOUT_ID + ".custo-suficiente", placeholders);
        }
        placeholders.put("falta", (needed - have) + " niveis de XP");
        return plugin.getMenuConfig().item(LAYOUT_ID + ".custo-insuficiente", placeholders);
    }

    private String formatAmount(double amount) {
        return amount == Math.floor(amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.2f", amount);
    }

    /**
     * Cobra o custo, consome 1 unidade de A e B, entrega resultado (+ sobra, se
     * houver) direto no inventario do jogador. Nao mexe em nada e devolve false se
     * faltar XP/saldo.
     */
    public boolean confirm() {
        if (pending == null) {
            return false;
        }
        AnvilConfig config = plugin.getAnvilConfig();
        AnvilCost cost = pending.cost();

        if (cost.isMonetary()) {
            if (!player.hasPermission("alkaanvil.bypass.cost")) {
                AlkaEconomyHook economyHook = plugin.getEconomyHook();
                if (!economyHook.has(player, cost.currencyId(), cost.currencyAmount())) {
                    sendMessage(config, "not-enough-currency");
                    return false;
                }
                economyHook.remove(player, cost.currencyId(), cost.currencyAmount());
            }
        } else if (player.getLevel() < cost.xpLevels()) {
            sendMessage(config, "not-enough-xp");
            return false;
        } else {
            player.setLevel(player.getLevel() - cost.xpLevels());
        }

        consumeOne(slotA);
        consumeOne(slotB);

        giveOrDrop(pending.result());
        if (pending.leftover() != null) {
            giveOrDrop(pending.leftover());
        }

        pending = null;
        recompute();
        return true;
    }

    private void consumeOne(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (item.getAmount() <= 1) {
            inventory.setItem(slot, null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void giveOrDrop(ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
        overflow.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
    }

    private void sendMessage(AnvilConfig config, String key) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.prefix() + config.message(key)));
    }
}
