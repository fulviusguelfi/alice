package com.projetoalice.alice;

import baritone.Baritone;
import baritone.BaritoneProvider;
import baritone.api.BaritoneAPI;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Manages the Alice FakePlayer lifecycle and its Baritone instance.
 * Spike C/C.5: validates Baritone lifecycle and tick performance on server thread.
 */
public class AliceEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");
    private static final GameProfile ALICE_PROFILE = new GameProfile(
            UUID.fromString("a11ce000-0000-4000-8000-000000000001"),
            "Alice"
    );

    private static final int METRICS_WINDOW = 200; // ~10 seconds at 20 TPS
    private static final int METRICS_LOG_INTERVAL = 6000; // ~5 minutes at 20 TPS
    private static final int POSITION_BROADCAST_INTERVAL = 2; // ~10 Hz position updates
    private static final int PERF_LOG_INTERVAL = 600; // ~30 seconds at 20 TPS
    private int perfLogCounter;

    private FakePlayer fakePlayer;
    private Baritone baritone;
    private boolean attached;

    // Client visibility: manual entity broadcast (Citizens-style)
    // Alice is NOT registered in ChunkMap because her null Connection would NPE
    // on chunk packet sends. We broadcast AddPlayer + position packets ourselves.
    private double lastBroadcastX, lastBroadcastY, lastBroadcastZ;
    private float lastBroadcastYaw, lastBroadcastPitch, lastBroadcastHeadYaw;
    private int positionBroadcastCounter;

    // Tick performance metrics
    private final long[] tickDurations = new long[METRICS_WINDOW];
    private int tickIndex;
    private int tickCount;
    private int totalTickCount;
    private volatile double lastP95Ms = 0.0;

    // Follow/path/stuck diagnostics
    private static final int FOLLOW_LOG_INTERVAL = 40; // ~2 seconds
    private static final int STUCK_WINDOW_TICKS = 60;  // ~3 seconds
    private static final double STUCK_MIN_MOVE = 0.5;
    private int followLogCounter;
    private int stuckCounter;
    private double stuckAnchorX, stuckAnchorY, stuckAnchorZ;
    private boolean stuckReported;

    /**
     * Spawns the Alice FakePlayer in the given level and attaches Baritone.
     */
    public void attach(ServerLevel level) {
        LOGGER.info("[Alice] Attaching FakePlayer to level: {}", level.dimension().location());

        this.fakePlayer = new AliceFakePlayer(level, ALICE_PROFILE);
        LOGGER.info("[Alice] FakePlayer created: uuid={}, name={}",
                fakePlayer.getUUID(), fakePlayer.getName().getString());

        // Position Alice near the first online player, or at world spawn if nobody is on
        var players = level.getServer().getPlayerList().getPlayers();
        if (!players.isEmpty()) {
            var target = players.get(0);
            fakePlayer.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0);
            LOGGER.info("[Alice] FakePlayer positioned near player {} at ({}, {}, {})",
                    target.getName().getString(),
                    (int) target.getX(), (int) target.getY(), (int) target.getZ());
        } else {
            var spawnPos = level.getSharedSpawnPos();
            fakePlayer.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            LOGGER.info("[Alice] FakePlayer positioned at world spawn: {}", spawnPos);
        }

        // Make Alice visible to clients. We broadcast packets manually instead of
        // registering her in ChunkMap because FakePlayer has a null Connection and
        // would NPE when the chunk tracker tries to send her chunk packets.
        // Sequence: tab-list entry (with skin) -> spawn entity -> head rotation.
        broadcastSpawn(level.getServer());

        // Initialize Baritone for this FakePlayer
        BaritoneProvider provider = (BaritoneProvider) BaritoneAPI.getProvider();
        this.baritone = provider.getOrCreateBaritone(fakePlayer);
        LOGGER.info("[Alice] Baritone attached to FakePlayer {}", fakePlayer.getUUID());

        // Disable Y-coordinate simplification for unloaded chunks.
        // Without this, Baritone converts GoalBlock to GoalXZ when underground chunks
        // aren't loaded for the FakePlayer, causing Alice to navigate to surface level
        // instead of the player's actual Y position.
        Baritone.settings().simplifyUnloadedYCoord.value = false;

        Baritone.settings().followRadius.value = Config.followDistance;
        LOGGER.info("[Alice] Baritone followRadius = {}", Config.followDistance);

        // Initialize the world provider so Baritone knows the current world
        this.baritone.getWorldProvider().initWorld(level);
        LOGGER.info("[Alice] Baritone world initialized for dimension: {}", level.dimension().location());

        this.stuckAnchorX = fakePlayer.getX();
        this.stuckAnchorY = fakePlayer.getY();
        this.stuckAnchorZ = fakePlayer.getZ();
        this.stuckCounter = 0;
        this.stuckReported = false;

        this.attached = true;
        LOGGER.info("[Alice] Attach complete. Baritone ready.");
    }

    /**
     * Called each server tick to drive Baritone and the FakePlayer entity.
     * Sequence mirrors vanilla Baritone's client-side event flow:
     *   1. serverTick()           — onTick PRE: Baritone plans paths, sets input overrides
     *   2. serverPlayerUpdate()   — onPlayerUpdate PRE: PathingBehavior executes path, LookBehavior sets rotation
     *   3. fakePlayer.tick()      — Entity physics: processes xxa/zza into actual movement
     *   4. serverPlayerUpdatePost — onPlayerUpdate POST: post-movement processing
     *   5. serverPostTick()       — onPostTick POST: finalize state
     */
    public void tick() {
        if (!attached || baritone == null) return;

        long start = System.nanoTime();
        baritone.serverTick();
        baritone.serverPlayerUpdate();
        fakePlayer.tick();
        baritone.serverPlayerUpdatePost();
        baritone.serverPostTick();

        applyIdleLook();

        positionBroadcastCounter++;
        if (positionBroadcastCounter >= POSITION_BROADCAST_INTERVAL) {
            positionBroadcastCounter = 0;
            broadcastPosition();
        }

        if (Config.logFollow) {
            logFollowAndStuck();
        }

        if (Config.logPerf && ++perfLogCounter >= PERF_LOG_INTERVAL) {
            perfLogCounter = 0;
            logPerf();
        }

        long elapsed = System.nanoTime() - start;

        tickDurations[tickIndex] = elapsed;
        tickIndex = (tickIndex + 1) % METRICS_WINDOW;
        if (tickCount < METRICS_WINDOW) tickCount++;
        totalTickCount++;

        // Log first report at 200 ticks (~10s), then every 6000 ticks (~5min)
        if (totalTickCount == METRICS_WINDOW || totalTickCount % METRICS_LOG_INTERVAL == 0) {
            logMetrics();
        }
    }

    private void logMetrics() {
        if (tickCount == 0) return;
        long[] snapshot = Arrays.copyOf(tickDurations, tickCount);
        Arrays.sort(snapshot);

        double avgMs = 0;
        for (long d : snapshot) avgMs += d;
        avgMs = (avgMs / snapshot.length) / 1_000_000.0;

        double p95Ms = snapshot[(int) (snapshot.length * 0.95)] / 1_000_000.0;
        double p99Ms = snapshot[(int) (snapshot.length * 0.99)] / 1_000_000.0;
        double maxMs = snapshot[snapshot.length - 1] / 1_000_000.0;

        lastP95Ms = p95Ms;

        LOGGER.info("[Alice] baritone tick avg={}ms p95={}ms p99={}ms max={}ms (window={})",
                String.format("%.2f", avgMs),
                String.format("%.2f", p95Ms),
                String.format("%.2f", p99Ms),
                String.format("%.2f", maxMs),
                tickCount);
    }

    /**
     * Periodic diagnostic log: follow target, path state, and stuck detection.
     * Emits every FOLLOW_LOG_INTERVAL ticks when anything interesting is happening.
     */
    private void logFollowAndStuck() {
        if (fakePlayer == null || baritone == null) return;

        var pathing = baritone.getPathingBehavior();
        var follow = baritone.getFollowProcess();
        boolean isPathing = pathing.isPathing();
        boolean followActive = follow.isActive();
        var goal = pathing.getGoal();
        boolean interesting = isPathing || followActive || goal != null;

        followLogCounter++;
        if (interesting && followLogCounter >= FOLLOW_LOG_INTERVAL) {
            followLogCounter = 0;
            int x = (int) fakePlayer.getX();
            int y = (int) fakePlayer.getY();
            int z = (int) fakePlayer.getZ();
            LOGGER.info("[Alice][Path] pos=({},{},{}) goal={} pathing={} followActive={}",
                    x, y, z,
                    goal == null ? "<none>" : goal.toString(),
                    isPathing, followActive);
            if (followActive) {
                LOGGER.info("[Alice][Follow] {}", follow.displayName0());
            }
            var exec = pathing.getCurrent();
            if (exec != null) {
                var path = exec.getPath();
                int idx = exec.getPosition();
                var movs = path.movements();
                if (idx >= 0 && idx < movs.size()) {
                    var m = movs.get(idx);
                    LOGGER.info("[Alice][Move] {} {} -> {} cost={}",
                            m.getClass().getSimpleName(),
                            m.getSrc(), m.getDest(),
                            String.format("%.2f", m.getCost()));
                    AliceMod.JOURNAL.recordMovement(
                            m.getClass().getSimpleName(),
                            m.getSrc().toString(),
                            m.getDest().toString(),
                            m.getCost());
                }
            }
        }

        // Stuck detection: no progress > STUCK_MIN_MOVE over STUCK_WINDOW_TICKS while having a goal.
        if (goal == null && !followActive) {
            stuckCounter = 0;
            stuckReported = false;
            return;
        }
        if (goal != null && goal.isInGoal(
                (int) fakePlayer.getX(), (int) fakePlayer.getY(), (int) fakePlayer.getZ())) {
            stuckCounter = 0;
            stuckReported = false;
            stuckAnchorX = fakePlayer.getX();
            stuckAnchorY = fakePlayer.getY();
            stuckAnchorZ = fakePlayer.getZ();
            return;
        }
        double dx = fakePlayer.getX() - stuckAnchorX;
        double dy = fakePlayer.getY() - stuckAnchorY;
        double dz = fakePlayer.getZ() - stuckAnchorZ;
        double moveSq = dx * dx + dy * dy + dz * dz;
        if (moveSq > STUCK_MIN_MOVE * STUCK_MIN_MOVE) {
            stuckAnchorX = fakePlayer.getX();
            stuckAnchorY = fakePlayer.getY();
            stuckAnchorZ = fakePlayer.getZ();
            stuckCounter = 0;
            stuckReported = false;
        } else {
            stuckCounter++;
            if (stuckCounter >= STUCK_WINDOW_TICKS && !stuckReported) {
                LOGGER.warn("[Alice][Stuck] no progress for {}t near ({},{},{}) goal={} followActive={}",
                        stuckCounter,
                        (int) fakePlayer.getX(), (int) fakePlayer.getY(), (int) fakePlayer.getZ(),
                        goal == null ? "<none>" : goal.toString(),
                        followActive);
                stuckReported = true;
            }
        }
    }

    /**
     * When not actively pathing, level the pitch and face the nearest player — overrides
     * the stale target left behind by Baritone's LookBehavior (which only nudges by 1°/tick).
     */
    private void applyIdleLook() {
        if (baritone.getPathingBehavior().isPathing()) return;
        var server = fakePlayer.getServer();
        if (server == null) return;
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        var tgt = players.get(0);
        double dx = tgt.getX() - fakePlayer.getX();
        double dz = tgt.getZ() - fakePlayer.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        fakePlayer.setYRot(yaw);
        fakePlayer.setYHeadRot(yaw);
        fakePlayer.setXRot(0f);
    }

    private void logPerf() {
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMB = rt.maxMemory() / (1024 * 1024);
        long totalMB = rt.totalMemory() / (1024 * 1024);
        int threads = Thread.activeCount();
        long gcMs = 0, gcCount = 0;
        for (var gc : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            gcMs += gc.getCollectionTime();
            gcCount += gc.getCollectionCount();
        }
        double heapPct = maxMB == 0 ? 0 : (usedMB * 100.0 / maxMB);
        LOGGER.info("[Alice][Perf] heap={}/{} MB (commit={}, {}%) threads={} gcTotal={}ms/{}",
                usedMB, maxMB, totalMB, String.format("%.1f", heapPct),
                threads, gcMs, gcCount);
    }

    /**
     * Broadcasts Alice spawn packets to all currently online players.
     * Called on attach and whenever a new player needs to see Alice.
     */
    public void broadcastSpawn(MinecraftServer server) {
        if (fakePlayer == null) return;
        var infoPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fakePlayer));
        var addPacket = new ClientboundAddPlayerPacket(fakePlayer);
        var headPacket = new ClientboundRotateHeadPacket(fakePlayer, (byte) (fakePlayer.getYHeadRot() * 256f / 360f));
        int count = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sendTo(p, infoPacket);
            sendTo(p, addPacket);
            sendTo(p, headPacket);
            count++;
        }
        LOGGER.info("[Alice] Broadcast spawn packets for {} to {} clients", fakePlayer.getUUID(), count);
        lastBroadcastX = fakePlayer.getX();
        lastBroadcastY = fakePlayer.getY();
        lastBroadcastZ = fakePlayer.getZ();
        lastBroadcastYaw = fakePlayer.getYRot();
        lastBroadcastPitch = fakePlayer.getXRot();
        lastBroadcastHeadYaw = fakePlayer.getYHeadRot();
    }

    /**
     * Sends Alice's current position/rotation to nearby clients.
     * Uses relative MoveEntity packets for small deltas, TeleportEntity for large ones.
     */
    private void broadcastPosition() {
        if (fakePlayer == null || fakePlayer.getServer() == null) return;

        double x = fakePlayer.getX();
        double y = fakePlayer.getY();
        double z = fakePlayer.getZ();
        float yaw = fakePlayer.getYRot();
        float pitch = fakePlayer.getXRot();
        float headYaw = fakePlayer.getYHeadRot();

        double dx = x - lastBroadcastX;
        double dy = y - lastBroadcastY;
        double dz = z - lastBroadcastZ;
        boolean moved = dx * dx + dy * dy + dz * dz > 1e-6;
        boolean rotated = yaw != lastBroadcastYaw || pitch != lastBroadcastPitch;
        boolean headRotated = headYaw != lastBroadcastHeadYaw;
        if (!moved && !rotated && !headRotated) return;

        Packet<?> movePacket;
        boolean needsTeleport = Math.abs(dx) > 7.0 || Math.abs(dy) > 7.0 || Math.abs(dz) > 7.0;
        if (needsTeleport) {
            movePacket = new ClientboundTeleportEntityPacket(fakePlayer);
        } else if (moved && rotated) {
            movePacket = new ClientboundMoveEntityPacket.PosRot(
                    fakePlayer.getId(),
                    (short) (dx * 4096), (short) (dy * 4096), (short) (dz * 4096),
                    (byte) (yaw * 256f / 360f), (byte) (pitch * 256f / 360f),
                    fakePlayer.onGround());
        } else if (moved) {
            movePacket = new ClientboundMoveEntityPacket.Pos(
                    fakePlayer.getId(),
                    (short) (dx * 4096), (short) (dy * 4096), (short) (dz * 4096),
                    fakePlayer.onGround());
        } else if (rotated) {
            movePacket = new ClientboundMoveEntityPacket.Rot(
                    fakePlayer.getId(),
                    (byte) (yaw * 256f / 360f), (byte) (pitch * 256f / 360f),
                    fakePlayer.onGround());
        } else {
            movePacket = null;
        }

        var headPacket = headRotated
                ? new ClientboundRotateHeadPacket(fakePlayer, (byte) (headYaw * 256f / 360f))
                : null;

        for (ServerPlayer p : fakePlayer.getServer().getPlayerList().getPlayers()) {
            if (movePacket != null) sendTo(p, movePacket);
            if (headPacket != null) sendTo(p, headPacket);
        }

        lastBroadcastX = x;
        lastBroadcastY = y;
        lastBroadcastZ = z;
        lastBroadcastYaw = yaw;
        lastBroadcastPitch = pitch;
        lastBroadcastHeadYaw = headYaw;
    }

    private static void sendTo(ServerPlayer p, Packet<?> packet) {
        if (p.connection != null) p.connection.send(packet);
    }

    /**
     * Returns an inline perf snapshot for /alicecmd perfstats RCON command.
     */
    public String getPerfStats() {
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMB = rt.maxMemory() / (1024 * 1024);
        double heapPct = maxMB == 0 ? 0 : usedMB * 100.0 / maxMB;
        return String.format("[Alice][Perf] heap=%d/%d MB (%.1f%%) threads=%d tickP95=%.2fms",
                usedMB, maxMB, heapPct, Thread.activeCount(), lastP95Ms);
    }

    /**
     * Detaches Baritone and cleans up the FakePlayer.
     */
    public void detach() {
        if (!attached) return;
        logMetrics(); // Final metrics report
        LOGGER.info("[Alice] Detaching Baritone from FakePlayer {}", fakePlayer.getUUID());

        baritone.getWorldProvider().closeWorld();
        BaritoneProvider provider = (BaritoneProvider) BaritoneAPI.getProvider();
        provider.destroyBaritone(fakePlayer);

        // Despawn Alice on all clients: remove the entity, then drop the tab-list entry.
        var server = fakePlayer.getServer();
        if (server != null) {
            var removePacket = new ClientboundRemoveEntitiesPacket(fakePlayer.getId());
            var infoRemove = new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID()));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                sendTo(p, removePacket);
                sendTo(p, infoRemove);
            }
        }
        fakePlayer.remove(Entity.RemovalReason.DISCARDED);

        this.baritone = null;
        this.fakePlayer = null;
        this.attached = false;
        LOGGER.info("[Alice] Detach complete.");
    }

    public boolean isAttached() {
        return attached;
    }

    public FakePlayer getFakePlayer() {
        return fakePlayer;
    }

    public Baritone getBaritone() {
        return baritone;
    }
}
