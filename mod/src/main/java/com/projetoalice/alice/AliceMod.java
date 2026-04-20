package com.projetoalice.alice;

import com.mojang.logging.LogUtils;
import com.projetoalice.alice.rules.AttackNearestHostileRule;
import com.projetoalice.alice.rules.FleeOnCriticalHealthRule;
import com.projetoalice.alice.rules.FleeOnLowHealthRule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AliceMod.MODID)
public class AliceMod {

    public static final String MODID = "alice";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BehaviorJournal JOURNAL = new BehaviorJournal();

    private final AliceEntity aliceEntity = new AliceEntity();
    private final RuleEngine ruleEngine = new RuleEngine();
    private final AliceChatHandler chatHandler = new AliceChatHandler(aliceEntity);
    private ServerLevel overworld;
    private boolean teleportedToPlayer;

    public AliceMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AliceCommands());
        AliceCommands.bindEntity(aliceEntity);
        AliceCommands.bindChatHandler(chatHandler);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[Alice] commonSetup — llm={} model={}", Config.llmUrl, Config.llmModel);

        // Register rules
        ruleEngine.addRule(new FleeOnCriticalHealthRule());
        ruleEngine.addRule(new FleeOnLowHealthRule());
        ruleEngine.addRule(new AttackNearestHostileRule());

        // Initialize LLM provider if API key is configured
        if (Config.llmApiKey != null && !Config.llmApiKey.isEmpty()) {
            chatHandler.setLlmProvider(new HttpLLMProvider());
            LOGGER.info("[Alice] LLM provider initialized: {} / {}", Config.llmUrl, Config.llmModel);
        } else {
            LOGGER.warn("[Alice] No LLM API key configured — chat will only support commands");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[Alice] server starting");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("[Alice] server started — spawning Alice FakePlayer");
        overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            aliceEntity.attach(overworld);
            teleportedToPlayer = false;
            JOURNAL.record(BehaviorJournal.Type.SYSTEM, "acordei",
                    "servidor iniciou em " + overworld.dimension().location());
        } else {
            LOGGER.error("[Alice] Overworld level is null! Cannot attach Alice.");
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!aliceEntity.isAttached()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Teleport Alice to the player who just joined
        var fp = aliceEntity.getFakePlayer();
        fp.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        LOGGER.info("[Alice] Teleported to player {} at ({}, {}, {})",
                player.getName().getString(),
                (int) player.getX(), (int) player.getY(), (int) player.getZ());
        teleportedToPlayer = true;

        // Re-broadcast spawn packets so the newly-connected player sees Alice
        // (he was not online when Alice was first broadcast on ServerStarted).
        aliceEntity.broadcastSpawn(event.getEntity().getServer());

        // Auto-follow the player
        aliceEntity.getBaritone().getFollowProcess().follow(e -> e == player);
        LOGGER.info("[Alice] Auto-following {}", player.getName().getString());
        JOURNAL.record(BehaviorJournal.Type.GOAL, "seguindo " + player.getName().getString(),
                "jogador entrou no servidor");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && aliceEntity.isAttached() && overworld != null) {
            JOURNAL.incrementTick();
            // Rules run first (safety > combat > utility)
            ruleEngine.tick(aliceEntity, overworld);
            // Then Baritone tick
            aliceEntity.tick();
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        chatHandler.handleChat(event.getPlayer(), event.getRawText());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[Alice] server stopping — detaching Alice");
        aliceEntity.detach();
        overworld = null;
    }
}
