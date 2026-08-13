package com.alkacode.anvil.anvil;

import com.alkacode.anvil.config.AnvilConfig;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtra as tags MiniMessage que um jogador pode usar no rename da bigorna, de
 * acordo com as 3 permissoes granulares do config (`rename-colors.permissions`):
 * `code` (so cores nomeadas + decoracao, ex: &lt;red&gt;), `hex` (tambem
 * &lt;#RRGGBB&gt;) e `minimessage` (qualquer tag, incluindo gradient/click/hover).
 * Tags nao permitidas pro tier do jogador sao escapadas (viram texto literal), nunca
 * removidas silenciosamente - assim o jogador ve exatamente o que digitou, so sem o
 * efeito.
 */
final class RenameSanitizer {

    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z_#][a-zA-Z0-9_:#]*)((?::[^>]*)?)>");

    private static final java.util.Set<String> NAMED_COLORS_AND_DECORATION = java.util.Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "grey",
            "dark_gray", "dark_grey", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
            "bold", "b", "italic", "i", "underlined", "u", "strikethrough", "st", "obfuscated", "obf", "reset", "!i");

    private RenameSanitizer() {
    }

    /** Usado pra cobrar rename-colors.color-cost - so quando o nome final realmente contem alguma tag reconhecida. */
    static boolean hasColorTag(String rawName) {
        return TAG.matcher(rawName).find();
    }

    static String sanitize(AnvilConfig config, Player player, String rawName) {
        if (!config.renameColorsEnabled()) {
            return escapeAll(rawName);
        }
        if (!config.renameRequirePermission()) {
            return rawName;
        }
        if (player.hasPermission("alkaanvil.rename.mm")) {
            return rawName;
        }
        if (player.hasPermission("alkaanvil.rename.hex")) {
            return filterTags(rawName, true);
        }
        if (player.hasPermission("alkaanvil.rename.color")) {
            return filterTags(rawName, false);
        }
        return escapeAll(rawName);
    }

    private static String filterTags(String raw, boolean allowHex) {
        Matcher matcher = TAG.matcher(raw);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase(java.util.Locale.ROOT);
            boolean allowed = NAMED_COLORS_AND_DECORATION.contains(tagName)
                    || (allowHex && tagName.startsWith("#") && tagName.matches("#[0-9a-f]{6}"));
            String replacement = allowed ? matcher.group() : "\\" + matcher.group();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String escapeAll(String raw) {
        return raw.replace("<", "\\<");
    }
}
