package com.alkacode.anvil.anvil;

import com.alkacode.anvil.config.AnvilConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.view.AnvilView;

/**
 * Aplica o {@link AnvilCost} calculado na AnvilView e resolve o cap de "Too Expensive".
 *
 * <p>O cliente do Minecraft mostra "Too Expensive" sempre que o repair cost exibido
 * ultrapassa 39 (regra do PROTOCOLO, nao do plugin) - burlar isso de verdade (mostrar
 * um numero maior em texto verde, cobrar o valor real por baixo) exige reescrever o
 * pacote via ProtocolLib, que hoje e so softdepend no AlkaCore e "ninguem usa ainda em
 * producao" (ver ALKANETWORKING.md). Por isso `remove-too-expensive` aqui significa
 * "nunca trava o item no slot" via CAP silencioso em 39, nao "mostra o preco real
 * acima de 39" - essa segunda parte fica pra quando o hook de ProtocolLib for
 * realmente usado por algum plugin da rede.
 */
public final class AnvilXpUtil {

    private static final int CLIENT_TOO_EXPENSIVE_THRESHOLD = 39;

    private AnvilXpUtil() {
    }

    public static int applyCap(AnvilConfig config, Player player, int rawXpLevels) {
        if (player.hasPermission("alkaanvil.bypass.cost")) {
            return rawXpLevels;
        }
        int capped = rawXpLevels;
        int maxCost = config.maxCost();
        if (maxCost > 0) {
            capped = Math.min(capped, maxCost);
        }
        if (config.removeTooExpensive()) {
            capped = Math.min(capped, CLIENT_TOO_EXPENSIVE_THRESHOLD);
        }
        return capped;
    }

    public static void apply(AnvilView view, AnvilCost cost) {
        view.setRepairCost(Math.max(0, cost.xpLevels()));
    }
}
