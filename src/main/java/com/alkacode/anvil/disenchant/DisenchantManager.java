package com.alkacode.anvil.disenchant;

import com.alkacode.anvil.config.AnvilConfig;
import com.alkacode.anvil.enchant.AlkaEnchantment;
import com.alkacode.anvil.enchant.AlkaEnchantmentRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Extrai encantamentos VANILLA de um item pra um livro (disenchant) ou divide um
 * livro vanilla com 2+ encantamentos em dois (shatter). Escopo deliberadamente
 * vanilla-only: encantamentos do AdvancedEnchantments ficam de fora dessas duas
 * operacoes - o AE ja tem seu proprio formato de livro (ver
 * {@link com.alkacode.anvil.enchant.AdvancedEnchantmentsWrapper}, `createEnchantmentBook`)
 * e o proprio conceito de "shatter" nao se aplica a ele (livros AE ja sao
 * mono-encantamento por design, `getBookEnchantment` retorna so um).
 *
 * <p>Ambas as operacoes trocam o item da ESQUERDA por um item diferente do que o
 * jogador retira do slot de resultado (disenchant: livro extraido no slot 2, item
 * limpo "sobra" pro jogador; shatter: livro extraido no slot 2, livro remanescente
 * "sobra"). A bigorna vanilla so suporta 1 saida - ver
 * {@link com.alkacode.anvil.listener.AnvilClickListener} pra como o item que
 * "sobra" e devolvido ao slot 0 um tick depois do jogador retirar o resultado.
 */
public final class DisenchantManager {

    private final AnvilConfig config;
    private final AlkaEnchantmentRegistry registry;

    public DisenchantManager(AnvilConfig config, AlkaEnchantmentRegistry registry) {
        this.config = config;
        this.registry = registry;
    }

    public record DisenchantResult(ItemStack resultBook, ItemStack leftoverItem, int cost) {
    }

    public record ShatterResult(ItemStack resultBook, ItemStack leftoverBook, int cost) {
    }

    public DisenchantResult tryDisenchant(Player player, ItemStack source, ItemStack book) {
        if (!config.disenchantEnabled()) {
            return null;
        }
        if (config.disenchantRequirePermission() && !player.hasPermission("alkaanvil.disenchant")) {
            return null;
        }
        if (source == null || source.getType().isAir() || book == null || book.getType() != Material.BOOK) {
            return null;
        }
        if (source.getType() == Material.BOOK || source.getType() == Material.ENCHANTED_BOOK) {
            return null; // livros vao pro tryShatter, nao pro disenchant
        }
        if (config.disenchantBlacklistMaterials().contains(source.getType())) {
            return null;
        }

        Map<AlkaEnchantment, Integer> vanillaEnchants = vanillaEnchantsOn(source);
        java.util.Set<String> blacklist = config.disenchantBlacklistEnchants();
        vanillaEnchants.keySet().removeIf(e -> blacklist.contains(e.getKey().toString()));
        if (vanillaEnchants.isEmpty()) {
            return null;
        }

        ItemStack resultBook = new ItemStack(Material.ENCHANTED_BOOK);
        int totalLevels = 0;
        for (Map.Entry<AlkaEnchantment, Integer> entry : vanillaEnchants.entrySet()) {
            entry.getKey().setLevel(resultBook, entry.getValue());
            totalLevels += entry.getValue();
        }

        ItemStack cleanItem = source.clone();
        cleanItem.setAmount(1);
        for (AlkaEnchantment enchant : vanillaEnchants.keySet()) {
            enchant.remove(cleanItem);
        }

        int cost = config.disenchantBaseCost() + vanillaEnchants.size() * config.disenchantCostPerEnchant()
                + totalLevels * config.disenchantCostPerLevel();
        return new DisenchantResult(resultBook, cleanItem, cost);
    }

    public ShatterResult tryShatter(Player player, ItemStack sourceBook, ItemStack blankBook) {
        if (!config.shatterEnabled()) {
            return null;
        }
        if (config.shatterRequirePermission() && !player.hasPermission("alkaanvil.shatter")) {
            return null;
        }
        if (sourceBook == null || sourceBook.getType() != Material.ENCHANTED_BOOK) {
            return null;
        }
        if (blankBook == null || blankBook.getType() != Material.BOOK) {
            return null;
        }

        Map<AlkaEnchantment, Integer> enchants = vanillaEnchantsOn(sourceBook);
        if (enchants.size() < 2) {
            return null;
        }

        Map.Entry<AlkaEnchantment, Integer> extracted = enchants.entrySet().iterator().next();

        ItemStack resultBook = new ItemStack(Material.ENCHANTED_BOOK);
        extracted.getKey().setLevel(resultBook, extracted.getValue());

        ItemStack remainingBook = sourceBook.clone();
        remainingBook.setAmount(1);
        extracted.getKey().remove(remainingBook);

        int cost = config.shatterBaseCost() + extracted.getValue() * config.shatterCostPerLevel();
        return new ShatterResult(resultBook, remainingBook, cost);
    }

    private Map<AlkaEnchantment, Integer> vanillaEnchantsOn(ItemStack item) {
        Map<AlkaEnchantment, Integer> result = new HashMap<>();
        if (item == null || item.getType().isAir()) {
            return result;
        }
        for (AlkaEnchantment enchant : registry.all().values()) {
            if (registry.isFromAdvancedEnchantments(enchant)) {
                continue;
            }
            int level = enchant.getLevel(item);
            if (level > 0) {
                result.put(enchant, level);
            }
        }
        return result;
    }
}
