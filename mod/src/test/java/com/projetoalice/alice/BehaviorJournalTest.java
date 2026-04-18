package com.projetoalice.alice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorJournalTest {

    @Test
    void recordsEventsInOrder() {
        BehaviorJournal j = new BehaviorJournal();
        j.record(BehaviorJournal.Type.CHAT, "jogador disse: oi", "");
        j.record(BehaviorJournal.Type.GOAL, "indo para (10,64,20)", "comando vem ca");
        j.record(BehaviorJournal.Type.MOVEMENT, "MovementTraverse", "custo 3.56");

        List<BehaviorJournal.Event> recent = j.recent(10);
        assertEquals(3, recent.size());
        assertEquals(BehaviorJournal.Type.CHAT, recent.get(0).type);
        assertEquals(BehaviorJournal.Type.MOVEMENT, recent.get(2).type);
    }

    @Test
    void ringBufferDiscardsOldestBeyondCapacity() {
        BehaviorJournal j = new BehaviorJournal();
        for (int i = 0; i < BehaviorJournal.CAPACITY + 10; i++) {
            j.record(BehaviorJournal.Type.SYSTEM, "event-" + i, "");
        }
        List<BehaviorJournal.Event> all = j.recent(BehaviorJournal.CAPACITY * 2);
        assertEquals(BehaviorJournal.CAPACITY, all.size());
        // First retained entry should be #10 (0-9 were dropped)
        assertEquals("event-10", all.get(0).what);
        assertEquals("event-" + (BehaviorJournal.CAPACITY + 9), all.get(all.size() - 1).what);
    }

    @Test
    void recordRuleSwitchSuppressesDuplicates() {
        BehaviorJournal j = new BehaviorJournal();
        j.recordRuleSwitch("FollowPlayer", "idle -> follow");
        j.recordRuleSwitch("FollowPlayer", "still following");
        j.recordRuleSwitch("FollowPlayer", "yet again");
        j.recordRuleSwitch("AttackNearestHostile", "zombie spotted");
        j.recordRuleSwitch("AttackNearestHostile", "still attacking");
        j.recordRuleSwitch(null, "combat cleared");

        List<BehaviorJournal.Event> events = j.recent(100);
        // Expect 3 distinct: FollowPlayer, AttackNearestHostile, <none>
        assertEquals(3, events.size());
        assertEquals("FollowPlayer", events.get(0).what);
        assertEquals("AttackNearestHostile", events.get(1).what);
        assertEquals("<none>", events.get(2).what);
    }

    @Test
    void recordMovementDedupesWhenSameSrcDest() {
        BehaviorJournal j = new BehaviorJournal();
        j.recordMovement("MovementTraverse", "(0,64,0)", "(0,64,1)", 3.56);
        j.recordMovement("MovementTraverse", "(0,64,0)", "(0,64,1)", 3.56);
        j.recordMovement("MovementAscend", "(0,64,1)", "(0,65,1)", 5.0);
        j.recordMovement("MovementTraverse", "(0,64,0)", "(0,64,1)", 3.56); // different from prev → new entry

        List<BehaviorJournal.Event> events = j.recent(100);
        assertEquals(3, events.size());
    }

    @Test
    void summaryPtBrContainsMarkers() {
        BehaviorJournal j = new BehaviorJournal();
        j.record(BehaviorJournal.Type.COMBAT, "engajei zombie", "hostil a 5 blocos");
        j.record(BehaviorJournal.Type.GOAL, "indo para (10,64,20)", "jogador pediu");

        String summary = j.summaryPtBr(10);
        assertTrue(summary.contains("combat"), "should contain type lowercased: " + summary);
        assertTrue(summary.contains("engajei zombie"));
        assertTrue(summary.contains("(porque: hostil a 5 blocos)"));
        assertTrue(summary.contains("ha "), "should include age marker: " + summary);
    }

    @Test
    void summaryEmptyReturnsFriendlyMessage() {
        BehaviorJournal j = new BehaviorJournal();
        String summary = j.summaryPtBr(10);
        assertNotNull(summary);
        assertTrue(summary.toLowerCase().contains("nao fiz nada")
                || summary.toLowerCase().contains("acabei de acordar"),
                "empty journal should return friendly message, got: " + summary);
    }

    @Test
    void compactForLlmKeepsLastTen() {
        BehaviorJournal j = new BehaviorJournal();
        for (int i = 0; i < 15; i++) {
            j.record(BehaviorJournal.Type.SYSTEM, "e" + i, "");
        }
        String compact = j.compactForLlm();
        // Should reference e5..e14 (last 10), not e0..e4
        assertTrue(compact.contains("=e14"), compact);
        assertTrue(compact.contains("=e5"), compact);
        assertFalse(compact.contains("=e4"), compact);
        assertFalse(compact.contains("=e0"), compact);
    }

    @Test
    void clearEmptiesBuffer() {
        BehaviorJournal j = new BehaviorJournal();
        j.record(BehaviorJournal.Type.CHAT, "x", "");
        j.clear();
        assertEquals(0, j.recent(100).size());
        // After clear, dedupe state should reset too
        j.recordRuleSwitch("Same", "first after clear");
        j.recordRuleSwitch("Same", "duplicate");
        assertEquals(1, j.recent(100).size());
    }

    @Test
    void concurrentWritesDoNotCorruptBuffer() throws InterruptedException {
        BehaviorJournal j = new BehaviorJournal();
        int threads = 8;
        int eventsPerThread = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        j.record(BehaviorJournal.Type.SYSTEM, "t" + tid + "-e" + i, "");
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "threads should finish within 5s");
        pool.shutdown();

        assertEquals(0, errors.get(), "no thread should have thrown");
        // Buffer should be capped at CAPACITY regardless of concurrent writes
        assertEquals(BehaviorJournal.CAPACITY, j.recent(10_000).size());
    }

    @Test
    void incrementTickAdvancesCounter() {
        BehaviorJournal j = new BehaviorJournal();
        long t0 = j.currentTick();
        j.incrementTick();
        j.incrementTick();
        j.incrementTick();
        assertEquals(t0 + 3, j.currentTick());
        j.record(BehaviorJournal.Type.SYSTEM, "x", "");
        assertEquals(t0 + 3, j.recent(1).get(0).tick);
    }
}
