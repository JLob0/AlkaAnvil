package com.alkacode.anvil.listener;

import com.alkacode.anvil.anvil.AnvilCost;
import com.alkacode.anvil.anvil.AnvilMergeLogic;
import com.alkacode.anvil.anvil.AnvilUseType;
import com.alkacode.anvil.anvil.AnvilXpUtil;
import com.alkacode.anvil.anvil.PendingAnvilOperation;
import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.disenchant.DisenchantManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Intercepta PrepareAnvilEvent. Testa disenchant/shatter primeiro (ambos tem
 * prioridade sobre o merge normal - um item+livro em branco NUNCA e uma combinacao
 * valida de merge/reparo, entao nao ha ambiguidade real), senao delega pra
 * {@link AnvilMergeLogic}. Recebe as dependencias via {@link Supplier} pelo mesmo
 * motivo documentado em {@link com.alkacode.anvil.AlkaAnvilPlugin} - sobreviver a
 * /alkaanvil reload sem re-registrar o listener.
 */
public final class PrepareAnvilListener implements Listener {

    private final Supplier<AnvilMergeLogic> mergeLogicSupplier;
    private final Supplier<DisenchantManager> disenchantManagerSupplier;
    private final AnvilConfig config;
    private final Map<UUID, PendingAnvilOperation> pendingOperations;

    public PrepareAnvilListener(Supplier<AnvilMergeLogic> mergeLogicSupplier,
                                 Supplier<DisenchantManager> disenchantManagerSupplier,
                                 AnvilConfig config,
                                 Map<UUID, PendingAnvilOperation> pendingOperations) {
        this.mergeLogicSupplier = mergeLogicSupplier;
        this.disenchantManagerSupplier = disenchantManagerSupplier;
        this.config = config;
        this.pendingOperations = pendingOperations;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        if (!(view.getPlayer() instanceof Player player)) {
            return;
        }

        ItemStack left = view.getItem(0);
        ItemStack right = view.getItem(1);
        DisenchantManager disenchantManager = disenchantManagerSupplier.get();

        DisenchantManager.DisenchantResult disenchant = disenchantManager.tryDisenchant(player, left, right);
        if (disenchant != null) {
            int cappedXp = AnvilXpUtil.applyCap(config, player, Math.max(1, disenchant.cost()));
            AnvilCost cost = buildCost(cappedXp, "repair");
            event.setResult(disenchant.resultBook());
            AnvilXpUtil.apply(view, cost);
            pendingOperations.put(player.getUniqueId(),
                    new PendingAnvilOperation(cost, AnvilUseType.DISENCHANT, disenchant.leftoverItem()));
            return;
        }

        DisenchantManager.ShatterResult shatter = disenchantManager.tryShatter(player, left, right);
        if (shatter != null) {
            int cappedXp = AnvilXpUtil.applyCap(config, player, Math.max(1, shatter.cost()));
            AnvilCost cost = buildCost(cappedXp, "repair");
            event.setResult(shatter.resultBook());
            AnvilXpUtil.apply(view, cost);
            pendingOperations.put(player.getUniqueId(),
                    new PendingAnvilOperation(cost, AnvilUseType.SHATTER, shatter.leftoverBook()));
            return;
        }

        String renameText = view.getRenameText();
        AnvilMergeLogic.MergeResult result = mergeLogicSupplier.get().compute(player, left, right, renameText);
        if (result == null) {
            pendingOperations.remove(player.getUniqueId());
            return;
        }

        event.setResult(result.item());
        AnvilXpUtil.apply(view, result.cost());
        pendingOperations.put(player.getUniqueId(),
                new PendingAnvilOperation(result.cost(), result.useType(), null));
    }

    private AnvilCost buildCost(int cappedXp, String monetaryOperation) {
        if (!config.monetaryCostEnabled()) {
            return AnvilCost.xpOnly(cappedXp);
        }
        double amount = cappedXp * config.monetaryMultiplier(monetaryOperation);
        return AnvilCost.monetary(cappedXp, amount, config.monetaryCurrency());
    }
}
