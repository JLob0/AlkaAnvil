package com.alkacode.anvil.enchant;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponte com o AdvancedEnchantments via reflection - AE e um plugin pago sem artefato
 * Maven publico, entao nao da pra usar compileOnly. Assinaturas confirmadas em
 * 2026-08-13 via {@code javap} direto no jar real (AdvancedEnchantments-9.24.1.jar,
 * instalado no servidor de dev) - a classe {@code net.advancedplugins.ae.api.AEAPI} e
 * 100% estatica (sem singleton), e {@code applyEnchant}/{@code removeEnchantment}
 * retornam um ItemStack NOVO (nao mutam o parametro in-place).
 *
 * <p>Este wrapper NAO reimplementa limite de nivel/conflito pros encantamentos AE -
 * o AE ja valida isso sozinho internamente (classe {@code AdvancedEnchantment} expoe
 * {@code isBlacklisted}/{@code getHighestLevel}), reimplementar seria duplicar uma
 * regra que o proprio AE ja garante ao aplicar via {@code applyEnchant}.
 */
public final class AdvancedEnchantmentsWrapper implements AlkaEnchantment {

    private static final String API_CLASS = "net.advancedplugins.ae.api.AEAPI";

    private static Method applyEnchantMethod;
    private static Method removeEnchantmentMethod;
    private static Method getEnchantLevelMethod;
    private static Method getHighestEnchantmentLevelMethod;
    private static Method isApplicableMethod;
    private static Method getAllEnchantmentsMethod;
    private static Method isAnEnchantmentMethod;
    private static boolean loaded;

    private final String aeName;
    private final Logger logger;

    private AdvancedEnchantmentsWrapper(String aeName, Logger logger) {
        this.aeName = aeName;
        this.logger = logger;
    }

    /** Chama uma unica vez no onEnable - preenche os Method estaticos se o AE estiver presente. */
    public static synchronized boolean initReflection(Logger logger) {
        if (loaded) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("AdvancedEnchantments") == null) {
            return false;
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            applyEnchantMethod = api.getMethod("applyEnchant", String.class, int.class, ItemStack.class);
            removeEnchantmentMethod = api.getMethod("removeEnchantment", ItemStack.class, String.class);
            getEnchantLevelMethod = api.getMethod("getEnchantLevel", String.class, ItemStack.class);
            getHighestEnchantmentLevelMethod = api.getMethod("getHighestEnchantmentLevel", String.class);
            isApplicableMethod = api.getMethod("isApplicable", Material.class, String.class);
            getAllEnchantmentsMethod = api.getMethod("getAllEnchantments");
            isAnEnchantmentMethod = api.getMethod("isAnEnchantment", String.class);
            loaded = true;
            logger.info("[AlkaAnvil] AdvancedEnchantments detectado - encantamentos AE habilitados na bigorna.");
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[AlkaAnvil] AdvancedEnchantments encontrado mas a API nao carregou via reflexao.", t);
            return false;
        }
    }

    public static boolean isAvailable() {
        return loaded;
    }

    @SuppressWarnings("unchecked")
    public static List<String> registeredNames(Logger logger) {
        if (!loaded) {
            return List.of();
        }
        try {
            Object result = getAllEnchantmentsMethod.invoke(null);
            return result instanceof List<?> list ? (List<String>) list : List.of();
        } catch (Throwable t) {
            logger.log(Level.FINE, "Falha ao listar encantamentos do AE.", t);
            return List.of();
        }
    }

    public static AdvancedEnchantmentsWrapper of(String aeName, Logger logger) {
        return new AdvancedEnchantmentsWrapper(aeName, logger);
    }

    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey("advancedenchantments", aeName.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public String getName() {
        return aeName;
    }

    @Override
    public int getMaxLevel() {
        try {
            Object result = getHighestEnchantmentLevelMethod.invoke(null, aeName);
            return result instanceof Integer level ? level : 1;
        } catch (Throwable t) {
            return 1;
        }
    }

    @Override
    public int getLevel(ItemStack item) {
        try {
            Object result = getEnchantLevelMethod.invoke(null, aeName, item);
            return result instanceof Integer level ? level : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public ItemStack setLevel(ItemStack item, int level) {
        try {
            if (level <= 0) {
                Object result = removeEnchantmentMethod.invoke(null, item, aeName);
                return result instanceof ItemStack stack ? stack : item;
            }
            Object result = applyEnchantMethod.invoke(null, aeName, level, item);
            return result instanceof ItemStack stack ? stack : item;
        } catch (Throwable t) {
            logger.log(Level.FINE, "Falha ao aplicar/remover encantamento AE '" + aeName + "'.", t);
            return item;
        }
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        try {
            Object result = isApplicableMethod.invoke(null, item.getType(), aeName);
            return result instanceof Boolean applicable && applicable;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Integer> allEnchantmentsOnItem(ItemStack item, Logger logger) {
        if (!loaded || item == null) {
            return Map.of();
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            Method method = api.getMethod("getEnchantmentsOnItem", ItemStack.class);
            Object result = method.invoke(null, item);
            return result instanceof Map<?, ?> map ? (Map<String, Integer>) map : Map.of();
        } catch (Throwable t) {
            logger.log(Level.FINE, "Falha ao ler encantamentos AE do item.", t);
            return Map.of();
        }
    }

    public static boolean isAnEnchantment(String name, Logger logger) {
        if (!loaded) {
            return false;
        }
        try {
            Object result = isAnEnchantmentMethod.invoke(null, name);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }
}
