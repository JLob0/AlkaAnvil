package com.alkacode.anvil.api;

import org.bukkit.entity.Player;

/**
 * API publica do AlkaAnvil, registrada via ServicesManager (mesmo padrao do
 * AlkaNpcsAPI/AlkaShopAPI). Permite outros plugins abrirem a GUI propria da bigorna
 * (BigornaMenu) sem depender das classes internas - ex: AlkaSmith usa isso pro botao
 * de bigorna do hub do Ferreiro em vez de abrir a AnvilInventory vanilla.
 */
public interface AlkaAnvilAPI {

    /** Abre a GUI da bigorna pro jogador - mesma que o bloco de bigorna real abre. */
    void openBigorna(Player player);
}
