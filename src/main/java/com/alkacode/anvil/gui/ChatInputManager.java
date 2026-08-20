package com.alkacode.anvil.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Fila simples de "proxima mensagem de chat vira input" pros prompts numericos da
 * GUI de admin ({@link SectionEditGui}) - mesmo padrao ja usado em
 * com.alkacode.vips.gui.ChatInputManager (AlkaVips), copiado aqui em vez de extraido
 * pro AlkaCore pra nao acoplar dois plugins por uma classe de ~20 linhas.
 */
public final class ChatInputManager {

    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public void await(UUID uuid, Consumer<String> callback) {
        pending.put(uuid, callback);
    }

    public boolean isAwaiting(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void complete(UUID uuid, String input) {
        Consumer<String> callback = pending.remove(uuid);
        if (callback != null) {
            callback.accept(input);
        }
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }
}
