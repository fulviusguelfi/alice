package com.projetoalice.alice;

import baritone.api.pathing.goals.GoalBlock;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slash commands so Alice can be driven from RCON (tests, ops) without needing
 * a real player to send chat messages. Registered at server start.
 *
 * Usage:
 *   /alicecmd goto &lt;x&gt; &lt;y&gt; &lt;z&gt;   — set a GoalBlock at the given coords
 *   /alicecmd stay                       — cancel current path
 *   /alicecmd status                     — reply with HP/pos/pathing state
 *   /alicecmd pos                        — reply with Alice position (x y z)
 */
public final class AliceCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");
    private static AliceEntity aliceEntity;
    private static AliceChatHandler chatHandler;

    public static void bindEntity(AliceEntity entity) {
        aliceEntity = entity;
    }

    public static void bindChatHandler(AliceChatHandler handler) {
        chatHandler = handler;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("alicecmd")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("goto")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(AliceCommands::cmdGoto)))))
                .then(Commands.literal("stay").executes(AliceCommands::cmdStay))
                .then(Commands.literal("status").executes(AliceCommands::cmdStatus))
                .then(Commands.literal("pos").executes(AliceCommands::cmdPos))
                .then(Commands.literal("tp")
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(AliceCommands::cmdTp)))))
                .then(Commands.literal("chat")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(AliceCommands::cmdChat))))
                .then(Commands.literal("perfstats").executes(AliceCommands::cmdPerfStats))
        );
        LOGGER.info("[Alice] /alicecmd registered");
    }

    private static int cmdGoto(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (aliceEntity == null || !aliceEntity.isAttached()) {
            ctx.getSource().sendFailure(Component.literal("Alice not attached"));
            return 0;
        }
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        aliceEntity.getBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));
        AliceMod.JOURNAL.record(BehaviorJournal.Type.GOAL,
                "ir para (" + x + "," + y + "," + z + ")", "rcon/cmd");
        ctx.getSource().sendSuccess(() -> Component.literal("[Alice] goto " + x + " " + y + " " + z), false);
        LOGGER.info("[Alice] goto via command: {} {} {}", x, y, z);
        return Command.SINGLE_SUCCESS;
    }

    private static int cmdStay(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (aliceEntity == null || !aliceEntity.isAttached()) {
            ctx.getSource().sendFailure(Component.literal("Alice not attached"));
            return 0;
        }
        aliceEntity.getBaritone().getPathingBehavior().cancelEverything();
        aliceEntity.getBaritone().getFollowProcess().cancel();
        aliceEntity.getBaritone().getCustomGoalProcess().setGoal(null);
        var pos = aliceEntity.getFakePlayer().blockPosition();
        ctx.getSource().sendSuccess(() -> Component.literal("[Alice] staying at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int cmdStatus(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (aliceEntity == null || !aliceEntity.isAttached()) {
            ctx.getSource().sendFailure(Component.literal("Alice not attached"));
            return 0;
        }
        var fp = aliceEntity.getFakePlayer();
        var pos = fp.blockPosition();
        boolean pathing = aliceEntity.getBaritone().getPathingBehavior().isPathing();
        String msg = String.format("[Alice] hp=%.0f/%.0f pos=%d,%d,%d pathing=%s",
                fp.getHealth(), fp.getMaxHealth(), pos.getX(), pos.getY(), pos.getZ(), pathing);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int cmdTp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (aliceEntity == null || !aliceEntity.isAttached()) {
            ctx.getSource().sendFailure(Component.literal("Alice not attached"));
            return 0;
        }
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        // Cancel any pathing first to avoid Baritone immediately trying to return
        aliceEntity.getBaritone().getPathingBehavior().cancelEverything();
        aliceEntity.getBaritone().getFollowProcess().cancel();
        aliceEntity.getBaritone().getCustomGoalProcess().setGoal(null);
        var fp = aliceEntity.getFakePlayer();
        fp.moveTo(x, y, z, fp.getYRot(), fp.getXRot());
        ctx.getSource().sendSuccess(() -> Component.literal("[Alice] tp " + x + " " + y + " " + z), false);
        LOGGER.info("[Alice] tp via command: {} {} {}", x, y, z);
        return Command.SINGLE_SUCCESS;
    }

    private static int cmdPos(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (aliceEntity == null || !aliceEntity.isAttached()) {
            ctx.getSource().sendFailure(Component.literal("Alice not attached"));
            return 0;
        }
        var fp = aliceEntity.getFakePlayer();
        String msg = String.format("[Alice] pos %.2f %.2f %.2f", fp.getX(), fp.getY(), fp.getZ());
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int cmdChat(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (chatHandler == null) {
            ctx.getSource().sendFailure(Component.literal("ChatHandler not bound"));
            return 0;
        }
        String player = StringArgumentType.getString(ctx, "player");
        String message = StringArgumentType.getString(ctx, "message");
        chatHandler.handleMessage(player, message);
        ctx.getSource().sendSuccess(() ->
                Component.literal("[Alice] Chat simulated from " + player), false);
        LOGGER.info("[Alice] RCON chat simulation from {} message={}", player, message);
        return Command.SINGLE_SUCCESS;
    }

    private static int cmdPerfStats(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (aliceEntity == null || !aliceEntity.isAttached()) {
            ctx.getSource().sendFailure(Component.literal("Alice not attached"));
            return 0;
        }
        String stats = aliceEntity.getPerfStats();
        ctx.getSource().sendSuccess(() -> Component.literal(stats), false);
        return Command.SINGLE_SUCCESS;
    }
}
