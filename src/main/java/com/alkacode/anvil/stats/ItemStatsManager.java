package com.alkacode.anvil.stats;

import com.alkacode.anvil.config.AnvilConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Estatisticas de item via PersistentDataContainer - sem tabela no banco, o item
 * carrega o proprio progresso (mesmo modelo do
 * {@code ItemStatsTracker-RPG/StatManager.java} do usuario, so a fatia de
 * "filtro de rastreamento + stat granular por categoria + lore", sem reincarnacao/
 * gemas/acessorios/bonus de atributo - ver memoria project-alkaanvil pro porque.
 */
public final class ItemStatsManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final AnvilConfig config;

    public ItemStatsManager(JavaPlugin plugin, AnvilConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    private NamespacedKey statKey(String statType) {
        return new NamespacedKey(plugin, "stat_" + statType.toLowerCase(Locale.ROOT));
    }

    /** Filtro de rastreamento: sem required-tag configurada, rastreia todo item aplicavel (categoria != null). */
    public boolean isTrackable(ItemStack item) {
        if (!config.itemStatsEnabled() || item == null || item.getType().isAir()) {
            return false;
        }
        if (categoryOf(item.getType()) == null) {
            return false;
        }
        String requiredTag = config.itemStatsRequiredTag();
        if (requiredTag.isBlank()) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String[] parts = requiredTag.split(":", 2);
        if (parts.length != 2) {
            return true;
        }
        try {
            NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            return pdc.has(key, PersistentDataType.STRING) || pdc.has(key, PersistentDataType.BYTE)
                    || pdc.has(key, PersistentDataType.INTEGER) || pdc.has(key, PersistentDataType.BOOLEAN);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public AnvilConfig.ItemStatsCategory categoryOf(Material type) {
        for (AnvilConfig.ItemStatsCategory category : config.itemStatsCategories()) {
            if (category.materials().contains(type)) {
                return category;
            }
        }
        return null;
    }

    public int getStat(ItemStack item, String statType) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return meta.getPersistentDataContainer().getOrDefault(statKey(statType), PersistentDataType.INTEGER, 0);
    }

    /** Incrementa o stat e atualiza a lore - unico ponto de entrada usado pelo ItemStatsListener. */
    public void incrementStat(ItemStack item, String statType, int amount) {
        if (item == null || item.getType().isAir() || amount == 0) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int current = pdc.getOrDefault(statKey(statType), PersistentDataType.INTEGER, 0);
        pdc.set(statKey(statType), PersistentDataType.INTEGER, current + amount);
        item.setItemMeta(meta);
        updateLore(item);
    }

    /** Reconstroi o bloco de lore de estatisticas (header + uma linha por stat rastreado + footer). */
    public void updateLore(ItemStack item) {
        if (!config.itemStatsLoreEnabled()) {
            return;
        }
        AnvilConfig.ItemStatsCategory category = categoryOf(item.getType());
        if (category == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        List<Component> existingLore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        removeStatsBlock(existingLore);

        List<Component> statsBlock = new ArrayList<>();
        if (!config.itemStatsLoreHeader().isBlank()) {
            statsBlock.add(MM.deserialize(config.itemStatsLoreHeader()));
        }
        for (String statType : category.track()) {
            int value = getStat(item, statType);
            String line = config.itemStatsLoreFormat()
                    .replace("<stat_name>", config.itemStatsName(statType))
                    .replace("<value>", formatNumber(value));
            statsBlock.add(MM.deserialize(line));
        }
        if (!config.itemStatsLoreFooter().isBlank()) {
            statsBlock.add(MM.deserialize(config.itemStatsLoreFooter()));
        }

        List<Component> newLore = new ArrayList<>();
        if (config.itemStatsLoreOnTop()) {
            newLore.addAll(statsBlock);
            newLore.addAll(existingLore);
        } else {
            newLore.addAll(existingLore);
            newLore.addAll(statsBlock);
        }
        meta.lore(newLore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "stats_lore_marker"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    /**
     * Remove um bloco de lore de estatisticas anterior (identificado pelo header/footer
     * configurados) antes de reconstruir - evita duplicar o bloco a cada incremento.
     */
    private void removeStatsBlock(List<Component> lore) {
        if (lore.isEmpty()) {
            return;
        }
        String header = config.itemStatsLoreHeader();
        String footer = config.itemStatsLoreFooter();
        if (header.isBlank() && footer.isBlank()) {
            return;
        }
        String headerPlain = plain(header);
        String footerPlain = plain(footer);
        int start = -1;
        int end = -1;
        for (int i = 0; i < lore.size(); i++) {
            String linePlain = plain(MM.serialize(lore.get(i)));
            if (start == -1 && !headerPlain.isEmpty() && linePlain.equals(headerPlain)) {
                start = i;
            } else if (start != -1 && !footerPlain.isEmpty() && linePlain.equals(footerPlain)) {
                end = i;
                break;
            }
        }
        if (start != -1 && end != -1) {
            lore.subList(start, end + 1).clear();
        }
    }

    private String plain(String miniMessage) {
        if (miniMessage == null || miniMessage.isBlank()) {
            return "";
        }
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(miniMessage));
    }

    private String formatNumber(long value) {
        if (value < 10_000) {
            return String.valueOf(value);
        }
        String raw = String.valueOf(value);
        StringBuilder formatted = new StringBuilder();
        int length = raw.length();
        for (int i = 0; i < length; i++) {
            if (i > 0 && (length - i) % 3 == 0) {
                formatted.append('.');
            }
            formatted.append(raw.charAt(i));
        }
        return formatted.toString();
    }
}
