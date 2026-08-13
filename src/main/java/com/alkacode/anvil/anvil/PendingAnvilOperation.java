package com.alkacode.anvil.anvil;

import org.bukkit.inventory.ItemStack;

/**
 * Estado de uma operacao de bigorna calculada em PrepareAnvilEvent, consumido no
 * click do slot de resultado. {@code leftoverItem} so e != null pra disenchant
 * (item limpo) e shatter (livro remanescente) - a bigorna vanilla so tem 1 slot de
 * saida, entao esse "segundo resultado" precisa ser devolvido ao slot 0 manualmente
 * um tick depois do jogador retirar o resultado principal, ver AnvilClickListener.
 */
public record PendingAnvilOperation(AnvilCost cost, AnvilUseType useType, ItemStack leftoverItem) {
}
