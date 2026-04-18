package com.projetoalice.alice;

import baritone.Baritone;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spike D: builds a test arena with obstacles and commands Baritone to navigate through them.
 *
 * Arena layout (top view, y=100 is floor, y=101 is standing level):
 *
 *   Start (0,101,0)                                    Goal (18,101,0)
 *     S . . . . X X . . . W . . . . . . . . G
 *               gap       wall
 *
 * - Gap at x=5,6: 2-block gap in floor (requires jump)
 * - Wall at x=10: 1-block wall (z=-5 to z=5, y=101) that must be contoured via z
 * - Platform spans x=-2..20, z=-7..7 at y=100 (stone)
 */
public class SpikeDTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");

    private static final BlockPos START = new BlockPos(0, 101, 0);
    private static final BlockPos GOAL = new BlockPos(18, 101, 0);

    private static final int PLATFORM_X_MIN = -2;
    private static final int PLATFORM_X_MAX = 20;
    private static final int PLATFORM_Z_MIN = -7;
    private static final int PLATFORM_Z_MAX = 7;
    private static final int PLATFORM_Y = 100;

    private boolean arenaBuilt;
    private boolean goalSet;
    private int ticksSinceGoal;
    private boolean completed;
    private long goalSetTime;

    /**
     * Build the arena and start pathfinding.
     */
    public void setup(ServerLevel level, AliceEntity alice) {
        buildArena(level);
        arenaBuilt = true;

        // Teleport Alice to start
        alice.getFakePlayer().moveTo(START.getX() + 0.5, START.getY(), START.getZ() + 0.5, 0, 0);
        LOGGER.info("[Alice][SpikeD] FakePlayer placed at START: {}", START);

        // Enable Baritone debug logging for diagnostics
        baritone.api.BaritoneAPI.getSettings().chatDebug.value = true;
        // Disable GoalBlock -> GoalXZ simplification: all arena chunks are loaded
        baritone.api.BaritoneAPI.getSettings().simplifyUnloadedYCoord.value = false;

        // Force-load chunks covering the arena so they don't unload
        int minChunkX = PLATFORM_X_MIN >> 4;
        int maxChunkX = PLATFORM_X_MAX >> 4;
        int minChunkZ = PLATFORM_Z_MIN >> 4;
        int maxChunkZ = PLATFORM_Z_MAX >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }
        LOGGER.info("[Alice][SpikeD] Force-loaded chunks x=[{},{}] z=[{},{}]",
                minChunkX, maxChunkX, minChunkZ, maxChunkZ);

        // Set goal
        Baritone baritone = alice.getBaritone();
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(GOAL));
        goalSet = true;
        goalSetTime = System.currentTimeMillis();
        LOGGER.info("[Alice][SpikeD] Goal set to: {} — pathfinding started", GOAL);
    }

    /**
     * Called each tick to monitor progress.
     */
    public void tick(AliceEntity alice) {
        if (!goalSet || completed) return;
        ticksSinceGoal++;

        BlockPos playerPos = alice.getFakePlayer().blockPosition();

        // Log position every 20 ticks (1 second)
        if (ticksSinceGoal % 20 == 0) {
            double dist = Math.sqrt(playerPos.distSqr(GOAL));
            LOGGER.info("[Alice][SpikeD] tick={} pos=({},{},{}) dist={} goal=({},{},{})",
                    ticksSinceGoal,
                    playerPos.getX(), playerPos.getY(), playerPos.getZ(),
                    String.format("%.1f", dist),
                    GOAL.getX(), GOAL.getY(), GOAL.getZ());
        }

        // Check if arrived (within 2 blocks)
        if (playerPos.distSqr(GOAL) <= 4) {
            long elapsed = System.currentTimeMillis() - goalSetTime;
            LOGGER.info("[Alice][SpikeD] SUCCESS! FakePlayer reached goal in {}ms ({} ticks). Final pos: ({},{},{})",
                    elapsed, ticksSinceGoal,
                    playerPos.getX(), playerPos.getY(), playerPos.getZ());
            completed = true;
            return;
        }

        // Timeout after 60 seconds (1200 ticks)
        if (ticksSinceGoal >= 1200) {
            double dist = Math.sqrt(playerPos.distSqr(GOAL));
            LOGGER.warn("[Alice][SpikeD] TIMEOUT after 60s! pos=({},{},{}) dist={} — pathfinding did not complete",
                    playerPos.getX(), playerPos.getY(), playerPos.getZ(),
                    String.format("%.1f", dist));
            completed = true;
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isArenaBuilt() {
        return arenaBuilt;
    }

    private void buildArena(ServerLevel level) {
        LOGGER.info("[Alice][SpikeD] Building test arena...");
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState wall = Blocks.COBBLESTONE.defaultBlockState();

        // Clear area above platform (y=101..105)
        for (int x = PLATFORM_X_MIN; x <= PLATFORM_X_MAX; x++) {
            for (int z = PLATFORM_Z_MIN; z <= PLATFORM_Z_MAX; z++) {
                for (int y = PLATFORM_Y + 1; y <= PLATFORM_Y + 5; y++) {
                    level.setBlock(new BlockPos(x, y, z), air, 2);
                }
            }
        }

        // Build stone floor at y=100
        for (int x = PLATFORM_X_MIN; x <= PLATFORM_X_MAX; x++) {
            for (int z = PLATFORM_Z_MIN; z <= PLATFORM_Z_MAX; z++) {
                // Gap at x=5,6 (but leave edges at z<-5 and z>5 for safety)
                if ((x == 5 || x == 6) && z >= -5 && z <= 5) {
                    level.setBlock(new BlockPos(x, PLATFORM_Y, z), air, 2);
                } else {
                    level.setBlock(new BlockPos(x, PLATFORM_Y, z), stone, 2);
                }
            }
        }

        // Build wall at x=10, y=101 (1 block high), z=-5 to z=5
        // Leave opening at z=6 so there IS a path around
        for (int z = PLATFORM_Z_MIN; z <= 5; z++) {
            level.setBlock(new BlockPos(10, PLATFORM_Y + 1, z), wall, 2);
        }

        // Also fill below the platform (y=99..95) to prevent falling through
        for (int x = PLATFORM_X_MIN; x <= PLATFORM_X_MAX; x++) {
            for (int z = PLATFORM_Z_MIN; z <= PLATFORM_Z_MAX; z++) {
                for (int y = PLATFORM_Y - 5; y < PLATFORM_Y; y++) {
                    level.setBlock(new BlockPos(x, y, z), stone, 2);
                }
            }
        }

        LOGGER.info("[Alice][SpikeD] Arena built: platform x=[{},{}] z=[{},{}] y={}, gap at x=5-6, wall at x=10",
                PLATFORM_X_MIN, PLATFORM_X_MAX, PLATFORM_Z_MIN, PLATFORM_Z_MAX, PLATFORM_Y);
    }
}
