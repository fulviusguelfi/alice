package com.projetoalice.alice;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Alice mod configuration — alice-common.toml
 */
@Mod.EventBusSubscriber(modid = AliceMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- LLM ---
    private static final ForgeConfigSpec.ConfigValue<String> LLM_URL = BUILDER
            .comment("LLM API base URL (OpenAI-compatible endpoint)")
            .define("llm.url", "https://api.groq.com/openai/v1");

    private static final ForgeConfigSpec.ConfigValue<String> LLM_API_KEY = BUILDER
            .comment("API key for the LLM provider")
            .define("llm.apiKey", "");

    private static final ForgeConfigSpec.ConfigValue<String> LLM_MODEL = BUILDER
            .comment("Model to use for chat responses")
            .define("llm.model", "llama-3.1-8b-instant");

    private static final ForgeConfigSpec.IntValue LLM_TIMEOUT_MS = BUILDER
            .comment("HTTP request timeout in milliseconds")
            .defineInRange("llm.timeoutMs", 30000, 1000, 120000);

    private static final ForgeConfigSpec.IntValue LLM_MAX_TOKENS = BUILDER
            .comment("Maximum tokens in LLM response")
            .defineInRange("llm.maxTokens", 200, 50, 1000);

    // --- Behavior ---
    private static final ForgeConfigSpec.IntValue PERCEPTION_RADIUS = BUILDER
            .comment("Radius in blocks for perceiving entities")
            .defineInRange("behavior.perceptionRadius", 16, 4, 64);

    private static final ForgeConfigSpec.IntValue COMBAT_RADIUS = BUILDER
            .comment("Radius in blocks for engaging hostile mobs")
            .defineInRange("behavior.combatRadius", 8, 2, 32);

    private static final ForgeConfigSpec.DoubleValue FLEE_HEALTH_THRESHOLD = BUILDER
            .comment("HP fraction below which Alice flees (0.0 to 1.0)")
            .defineInRange("behavior.fleeHealthThreshold", 0.20, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue CRITICAL_HEALTH_THRESHOLD = BUILDER
            .comment("HP fraction below which Alice sprint-flees (0.0 to 1.0)")
            .defineInRange("behavior.criticalHealthThreshold", 0.10, 0.0, 1.0);

    private static final ForgeConfigSpec.IntValue FOLLOW_DISTANCE = BUILDER
            .comment("Blocks Alice tolerates being away from the player (wired into Baritone followRadius)")
            .defineInRange("behavior.followDistance", 2, 1, 16);

    private static final ForgeConfigSpec.BooleanValue AUTO_COMBAT = BUILDER
            .comment("Whether Alice automatically attacks nearby hostile mobs")
            .define("behavior.autoCombat", true);

    // --- Inventory ---
    private static final ForgeConfigSpec.IntValue PICKUP_RADIUS = BUILDER
            .comment("Radius in blocks for Alice to detect and pick up dropped items")
            .defineInRange("inventory.pickupRadius", 4, 1, 16);

    private static final ForgeConfigSpec.IntValue CONTAINER_SCAN_RADIUS = BUILDER
            .comment("Radius in blocks to scan for containers")
            .defineInRange("inventory.containerScanRadius", 8, 2, 32);

    private static final ForgeConfigSpec.BooleanValue AUTO_EQUIP = BUILDER
            .comment("Whether Alice automatically equips better armor and weapons")
            .define("inventory.autoEquip", true);

    // --- Debug ---
    private static final ForgeConfigSpec.BooleanValue LOG_RULES = BUILDER
            .comment("Log rule activations")
            .define("debug.logRules", false);

    private static final ForgeConfigSpec.BooleanValue LOG_LLM = BUILDER
            .comment("Log LLM requests and responses")
            .define("debug.logLlm", true);

    private static final ForgeConfigSpec.BooleanValue LOG_FOLLOW = BUILDER
            .comment("Log follow/path state every ~2s and stuck detection")
            .define("debug.logFollow", true);

    private static final ForgeConfigSpec.BooleanValue LOG_PERF = BUILDER
            .comment("Log JVM heap/threads/GC every 30s")
            .define("debug.logPerf", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // --- Resolved values ---
    public static String llmUrl;
    public static String llmApiKey;
    public static String llmModel;
    public static int llmTimeoutMs;
    public static int llmMaxTokens;

    public static int perceptionRadius;
    public static int combatRadius;
    public static double fleeHealthThreshold;
    public static double criticalHealthThreshold;
    public static int followDistance;
    public static boolean autoCombat;

    public static int pickupRadius;
    public static int containerScanRadius;
    public static boolean autoEquip;

    public static boolean logRules;
    public static boolean logLlm;
    public static boolean logFollow;
    public static boolean logPerf;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        llmUrl = LLM_URL.get();
        llmApiKey = LLM_API_KEY.get();
        llmModel = LLM_MODEL.get();
        llmTimeoutMs = LLM_TIMEOUT_MS.get();
        llmMaxTokens = LLM_MAX_TOKENS.get();

        perceptionRadius = PERCEPTION_RADIUS.get();
        combatRadius = COMBAT_RADIUS.get();
        fleeHealthThreshold = FLEE_HEALTH_THRESHOLD.get();
        criticalHealthThreshold = CRITICAL_HEALTH_THRESHOLD.get();
        followDistance = FOLLOW_DISTANCE.get();
        autoCombat = AUTO_COMBAT.get();

        pickupRadius = PICKUP_RADIUS.get();
        containerScanRadius = CONTAINER_SCAN_RADIUS.get();
        autoEquip = AUTO_EQUIP.get();

        logRules = LOG_RULES.get();
        logLlm = LOG_LLM.get();
        logFollow = LOG_FOLLOW.get();
        logPerf = LOG_PERF.get();
    }
}
