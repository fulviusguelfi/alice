package com.projetoalice.alice;

import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles chat messages directed at Alice.
 * Parses simple commands (follow, stay, goto, status) and forwards
 * natural language to the LLM provider when available.
 */
public class AliceChatHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");

    // Patterns: "alice, <command>" or "alice <command>" (case insensitive, PT-BR friendly)
    private static final Pattern ALICE_PREFIX = Pattern.compile(
            "^(?:alice|@alice)[,:]?\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern GOTO_PATTERN = Pattern.compile(
            "(?:goto|va(?:\\s+para)?|ir(?:\\s+para)?)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)",
            Pattern.CASE_INSENSITIVE);

    private final AliceEntity aliceEntity;
    private AliceLLMProvider llmProvider;

    public AliceChatHandler(AliceEntity aliceEntity) {
        this.aliceEntity = aliceEntity;
    }

    public void setLlmProvider(AliceLLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * Process a chat message. Returns true if the message was handled by Alice.
     */
    public boolean handleChat(ServerPlayer sender, String rawText) {
        Matcher prefixMatch = ALICE_PREFIX.matcher(rawText.trim());
        if (!prefixMatch.matches()) return false;

        String command = prefixMatch.group(1).trim();
        LOGGER.info("[Alice] chat from {}: {}", sender.getName().getString(), command);
        AliceMod.JOURNAL.record(BehaviorJournal.Type.CHAT,
                sender.getName().getString() + " disse: " + command, "");

        if (!aliceEntity.isAttached()) {
            reply(sender, "Ainda nao estou pronta. Aguarda um momento.");
            return true;
        }

        // --- Command dispatch ---
        String lower = command.toLowerCase();

        // Introspection: "o que voce fez" / "historia" / "recente" — list journal without LLM
        if (lower.equals("o que voce fez") || lower.equals("o que você fez")
                || lower.equals("o que fez") || lower.equals("historia")
                || lower.equals("história") || lower.equals("recente")
                || lower.equals("journal") || lower.equals("memoria")
                || lower.equals("memória")) {
            sendHistory(sender);
            return true;
        }

        // Follow
        if (lower.equals("follow") || lower.equals("me segue") || lower.equals("segue")
                || lower.equals("me siga") || lower.equals("siga") || lower.startsWith("vem comigo")
                || lower.startsWith("vem até mim") || lower.startsWith("vem ate mim")
                || lower.equals("vem") || lower.startsWith("vem cá") || lower.startsWith("vem ca")) {
            startFollow(sender);
            return true;
        }

        // Stay
        if (lower.equals("stay") || lower.equals("fica") || lower.equals("fica ai")
                || lower.equals("fica aí") || lower.equals("para") || lower.equals("pare")) {
            stopAndStay(sender);
            return true;
        }

        // Goto
        Matcher gotoMatch = GOTO_PATTERN.matcher(command);
        if (gotoMatch.find()) {
            int x = Integer.parseInt(gotoMatch.group(1));
            int y = Integer.parseInt(gotoMatch.group(2));
            int z = Integer.parseInt(gotoMatch.group(3));
            goTo(sender, x, y, z);
            return true;
        }

        // Status
        if (lower.equals("status") || lower.equals("como voce esta")
                || lower.equals("como você está") || lower.equals("como vai")) {
            sendStatus(sender);
            return true;
        }

        // Not a known command — forward to LLM if available
        if (llmProvider != null) {
            forwardToLLM(sender, command);
        } else {
            reply(sender, "Entendi \"" + command + "\" mas ainda nao tenho LLM conectado. " +
                    "Comandos: follow, stay, goto <x y z>, status.");
        }
        return true;
    }

    private void startFollow(ServerPlayer target) {
        var baritone = aliceEntity.getBaritone();
        baritone.getFollowProcess().follow(e -> e == target);
        reply(target, "Estou te seguindo, " + target.getName().getString() + ".");
        LOGGER.info("[Alice] Following player {}", target.getName().getString());
        AliceMod.JOURNAL.record(BehaviorJournal.Type.GOAL,
                "seguindo " + target.getName().getString(), "jogador pediu no chat");
    }

    private void stopAndStay(ServerPlayer sender) {
        var baritone = aliceEntity.getBaritone();
        baritone.getPathingBehavior().cancelEverything();
        var pos = aliceEntity.getFakePlayer().blockPosition();
        reply(sender, "Parei aqui em " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".");
        LOGGER.info("[Alice] Staying at {}", pos);
        AliceMod.JOURNAL.record(BehaviorJournal.Type.GOAL, "parada em " + pos,
                sender.getName().getString() + " pediu pra parar");
    }

    private void goTo(ServerPlayer sender, int x, int y, int z) {
        var baritone = aliceEntity.getBaritone();
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));
        reply(sender, "Indo para " + x + ", " + y + ", " + z + ".");
        LOGGER.info("[Alice] Going to {}, {}, {}", x, y, z);
        AliceMod.JOURNAL.record(BehaviorJournal.Type.GOAL,
                "ir para (" + x + "," + y + "," + z + ")",
                sender.getName().getString() + " pediu no chat");
    }

    private void sendHistory(ServerPlayer sender) {
        String summary = AliceMod.JOURNAL.summaryPtBr(15);
        // Send header
        reply(sender, "Meus ultimos comportamentos:");
        // Chat has a line limit; split by newline and send each
        for (String line : summary.split("\n")) {
            if (!line.isBlank()) reply(sender, line);
        }
    }

    private void sendStatus(ServerPlayer sender) {
        var fp = aliceEntity.getFakePlayer();
        float hp = fp.getHealth();
        float maxHp = fp.getMaxHealth();
        var pos = fp.blockPosition();
        boolean pathActive = aliceEntity.getBaritone().getPathingBehavior().isPathing();

        String status = String.format("HP: %.0f/%.0f | Pos: %d, %d, %d | %s",
                hp, maxHp, pos.getX(), pos.getY(), pos.getZ(),
                pathActive ? "Navegando" : "Parada");
        reply(sender, status);
    }

    private void forwardToLLM(ServerPlayer sender, String message) {
        reply(sender, "Pensando...");
        llmProvider.chatAsync(sender.getName().getString(), message)
                .thenAccept(response -> {
                    AliceMod.JOURNAL.record(BehaviorJournal.Type.LLM,
                            "respondi: " + truncate(response, 60),
                            "pergunta de " + sender.getName().getString());
                    // Schedule reply on server thread
                    sender.server.execute(() -> reply(sender, response));
                })
                .exceptionally(ex -> {
                    LOGGER.error("[Alice] LLM error", ex);
                    AliceMod.JOURNAL.record(BehaviorJournal.Type.LLM, "falha ao pensar",
                            ex.getCause() != null ? ex.getCause().getClass().getSimpleName()
                                    : ex.getClass().getSimpleName());
                    sender.server.execute(() -> reply(sender,
                            "Desculpa, tive um problema pra pensar. Tenta de novo."));
                    return null;
                });
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private void reply(ServerPlayer target, String text) {
        target.sendSystemMessage(Component.literal("[Alice] " + text));
    }
}
