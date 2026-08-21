package com.alkacode.anvil.gui;

import com.alkacode.anvil.AlkaAnvilPlugin;
import com.alkacode.anvil.config.AnvilConfig;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Construtores prontos de {@link SectionEditGui.Field} por tipo escalar - cada um sabe
 * gravar (via {@link AnvilConfig#set}), disparar {@code anvilPlugin().reloadAll()} (pra
 * campos que alimentam um snapshot tirado na construcao, ex: AlkaEnchantmentRegistry) e
 * se re-renderizar ou reabrir sozinho, sem o chamador precisar repetir esse fiapo em
 * cada secao de {@link AdminConfigGui}.
 *
 * <p>{@code path} e ao mesmo tempo a chave de config.yml (via {@link AnvilConfig#set})
 * E a chave do icone em menus.yml.alkaanvil-section.fields.<path> (material+name) - R8:
 * icone/texto do campo vem do YML, so o binding com o config.yml e logica em Java.
 */
public final class ConfigFields {

    private static final String FIELDS_PREFIX = "alkaanvil-section.fields.";

    private ConfigFields() {
    }

    private static Material icon(AlkaAnvilPlugin anvilPlugin, String path) {
        Material material = anvilPlugin.getMenuConfig().item(FIELDS_PREFIX + path, null).getType();
        return material;
    }

    private static String label(AlkaAnvilPlugin anvilPlugin, String path) {
        return anvilPlugin.getMenuConfig().name(FIELDS_PREFIX + path, null);
    }

    public static SectionEditGui.Field bool(AlkaAnvilPlugin anvilPlugin, AnvilConfig config, String path,
                                             BooleanSupplier getter) {
        Material icon = icon(anvilPlugin, path);
        String label = label(anvilPlugin, path);
        return new SectionEditGui.Field() {
            public String label() { return label; }
            public Material icon() { return icon; }
            public String currentValueText() { return getter.getAsBoolean() ? "<green>ativo" : "<red>desativado"; }
            public void onClick(SectionEditGui gui) {
                config.set(path, !getter.getAsBoolean());
                gui.anvilPlugin().reloadAll();
                gui.refreshUi();
            }
        };
    }

    public static SectionEditGui.Field intField(AlkaAnvilPlugin anvilPlugin, AnvilConfig config, String path,
                                                 IntSupplier getter) {
        Material icon = icon(anvilPlugin, path);
        String label = label(anvilPlugin, path);
        return new SectionEditGui.Field() {
            public String label() { return label; }
            public Material icon() { return icon; }
            public String currentValueText() { return String.valueOf(getter.getAsInt()); }
            public void onClick(SectionEditGui gui) {
                promptNumber(gui, config.message("admin-prompt-int"), raw -> {
                    try {
                        config.set(path, Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException ignored) {
                        sendMessage(gui, config.message("admin-invalid-number"));
                    }
                    gui.anvilPlugin().reloadAll();
                });
            }
        };
    }

    public static SectionEditGui.Field doubleField(AlkaAnvilPlugin anvilPlugin, AnvilConfig config, String path,
                                                     DoubleSupplier getter) {
        Material icon = icon(anvilPlugin, path);
        String label = label(anvilPlugin, path);
        return new SectionEditGui.Field() {
            public String label() { return label; }
            public Material icon() { return icon; }
            public String currentValueText() { return String.valueOf(getter.getAsDouble()); }
            public void onClick(SectionEditGui gui) {
                promptNumber(gui, config.message("admin-prompt-double"), raw -> {
                    try {
                        config.set(path, Double.parseDouble(raw.trim().replace(",", ".")));
                    } catch (NumberFormatException ignored) {
                        sendMessage(gui, config.message("admin-invalid-number"));
                    }
                    gui.anvilPlugin().reloadAll();
                });
            }
        };
    }

    /** Cicla entre valores fixos (ex: work-penalty.merge-mode) - sem chat, so clique repetido. */
    public static SectionEditGui.Field enumCycle(AlkaAnvilPlugin anvilPlugin, AnvilConfig config, String path,
                                                  List<String> options, Supplier<String> getter) {
        Material icon = icon(anvilPlugin, path);
        String label = label(anvilPlugin, path);
        return new SectionEditGui.Field() {
            public String label() { return label; }
            public Material icon() { return icon; }
            public String currentValueText() { return getter.get(); }
            public void onClick(SectionEditGui gui) {
                String current = getter.get().toUpperCase(Locale.ROOT);
                int index = options.indexOf(current);
                String next = options.get((index + 1) % options.size());
                config.set(path, next);
                gui.anvilPlugin().reloadAll();
                gui.refreshUi();
            }
        };
    }

    /** Cicla entre as moedas reais do AlkaEconomy (monetary-cost.currency) - se o AlkaEconomy
     * nao estiver presente, cai pra um unico "sem moedas" e nao faz nada no clique. */
    public static SectionEditGui.Field currencyCycle(AlkaAnvilPlugin anvilPlugin, AnvilConfig config, String path,
                                                       Supplier<String> getter, Supplier<List<String>> currencyIds) {
        Material icon = icon(anvilPlugin, path);
        String label = label(anvilPlugin, path);
        return new SectionEditGui.Field() {
            public String label() { return label; }
            public Material icon() { return icon; }
            public String currentValueText() { return getter.get(); }
            public List<String> extraLore() {
                return currencyIds.get().isEmpty()
                        ? List.of("<red>AlkaEconomy ausente - moedas nao listadas")
                        : List.of();
            }
            public void onClick(SectionEditGui gui) {
                List<String> ids = currencyIds.get();
                if (ids.isEmpty()) {
                    return;
                }
                int index = ids.indexOf(getter.get());
                String next = ids.get((index + 1) % ids.size());
                config.set(path, next);
                gui.anvilPlugin().reloadAll();
                gui.refreshUi();
            }
        };
    }

    private static void promptNumber(SectionEditGui gui, String prompt, java.util.function.Consumer<String> onInput) {
        gui.viewer().closeInventory();
        sendMessage(gui, prompt);
        gui.anvilPlugin().getChatInputManager().await(gui.viewer().getUniqueId(), input -> {
            onInput.accept(input);
            gui.open();
        });
    }

    private static void sendMessage(SectionEditGui gui, String miniMessage) {
        gui.viewer().sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(gui.anvilPlugin().getAnvilConfig().prefix() + miniMessage));
    }
}
