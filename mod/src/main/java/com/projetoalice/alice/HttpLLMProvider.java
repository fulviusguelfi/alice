package com.projetoalice.alice;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI-compatible HTTP LLM provider.
 * Works with Groq, Maritaca, Ollama, and any OpenAI-compatible API.
 */
public class HttpLLMProvider implements AliceLLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");
    private static final Gson GSON = new Gson();

    private static final String SYSTEM_PROMPT_BASE =
            "Voce e a Alice, uma jogadora IA companheira em um servidor Minecraft " +
            "com o modpack Cursed Walking. Voce e ruiva, sobrevivente, direta e " +
            "prestativa. Responde sempre em portugues do Brasil. " +
            "Seja breve (maximo 3 frases) a menos que o jogador peca explicacao detalhada. " +
            "Voce tem consciencia dos seus proprios comportamentos recentes — use a secao " +
            "[CONTEXTO COMPORTAMENTAL] abaixo para responder perguntas tipo 'o que voce fez', " +
            "'por que fez isso', 'o que esta fazendo'. Se a pergunta for sobre algo que nao " +
            "aparece no contexto, diga que nao lembra.";

    private final HttpClient httpClient;

    public HttpLLMProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public CompletableFuture<String> chatAsync(String playerName, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callLLM(playerName, message);
            } catch (Exception e) {
                LOGGER.error("[Alice] LLM call failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    private String callLLM(String playerName, String message) throws Exception {
        String url = Config.llmUrl;
        if (!url.endsWith("/")) url += "/";
        url += "chat/completions";

        JsonObject body = new JsonObject();
        body.addProperty("model", Config.llmModel);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", Config.llmMaxTokens);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        String journalCtx = AliceMod.JOURNAL.compactForLlm();
        String systemContent = SYSTEM_PROMPT_BASE
                + "\n\n[CONTEXTO COMPORTAMENTAL — ultimos eventos, mais recente primeiro]\n"
                + (journalCtx.isEmpty() ? "(vazio — acabei de acordar)" : journalCtx);
        systemMsg.addProperty("content", systemContent);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", "[" + playerName + "] " + message);
        messages.add(userMsg);

        body.add("messages", messages);

        String jsonBody = GSON.toJson(body);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Config.llmTimeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (Config.llmApiKey != null && !Config.llmApiKey.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + Config.llmApiKey);
        }

        long t0 = System.currentTimeMillis();
        HttpResponse<String> resp = httpClient.send(reqBuilder.build(),
                HttpResponse.BodyHandlers.ofString());
        long elapsed = System.currentTimeMillis() - t0;

        if (Config.logLlm) {
            LOGGER.info("[Alice][LLM] {} {}ms HTTP {}", Config.llmModel, elapsed, resp.statusCode());
        }

        if (resp.statusCode() != 200) {
            LOGGER.error("[Alice][LLM] HTTP {} body={}", resp.statusCode(),
                    resp.body().substring(0, Math.min(resp.body().length(), 300)));
            return "Desculpa, nao consegui pensar agora (HTTP " + resp.statusCode() + ").";
        }

        JsonObject respJson = GSON.fromJson(resp.body(), JsonObject.class);
        String content = respJson.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        if (Config.logLlm) {
            LOGGER.info("[Alice][LLM] response: {}", content.substring(0, Math.min(content.length(), 100)));
        }

        return content;
    }
}
