package com.alkacode.anvil.anvil;

/**
 * Custo de uma operacao de bigorna. {@code currencyAmount} so e != 0 quando
 * monetary-cost esta habilitado no config - nesse modo o XP exibido na bigorna e
 * apenas simbolico (1 nivel, sempre pagavel) e o custo real e cobrado em moeda na
 * retirada do item, ver {@link com.alkacode.anvil.listener.AnvilClickListener}.
 */
public record AnvilCost(int xpLevels, double currencyAmount, String currencyId) {

    public static AnvilCost xpOnly(int xpLevels) {
        return new AnvilCost(xpLevels, 0, null);
    }

    public static AnvilCost monetary(int realXpLevels, double currencyAmount, String currencyId) {
        // 1 nivel simbolico no slot da bigorna - o custo de verdade e a moeda.
        int symbolicLevels = realXpLevels > 0 ? 1 : 0;
        return new AnvilCost(symbolicLevels, currencyAmount, currencyId);
    }

    public boolean isMonetary() {
        return currencyId != null;
    }
}
