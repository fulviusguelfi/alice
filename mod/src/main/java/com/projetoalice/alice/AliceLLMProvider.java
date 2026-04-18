package com.projetoalice.alice;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for LLM providers (Groq, Maritaca, Ollama).
 * All implementations must be async to avoid blocking the server tick.
 */
public interface AliceLLMProvider {

    /**
     * Send a chat message to the LLM and get a response asynchronously.
     *
     * @param playerName the name of the player talking to Alice
     * @param message    the player's message
     * @return a future that completes with Alice's response text
     */
    CompletableFuture<String> chatAsync(String playerName, String message);
}
