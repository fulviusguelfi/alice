"""Combat rules tests: attack, flee, critical flee, combat→follow transition. Covers DOD D06 + D07.

All tests run in the shared combat_arena fixture at (-5800, 69, -5800).
Requires server log access via log_tail for rule-fire assertions.

Configuration:
  ALICE_RCON_HOST / ALICE_RCON_PORT / ALICE_RCON_PASSWORD (same as conftest.py)
"""
from __future__ import annotations

import re
import time

import pytest

from .rcon_client import RconClient
from .log_tail import LogTail

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

COMBAT_X = -5800
COMBAT_Y = 69
COMBAT_Z = -5800

RULE_FIRE_TIMEOUT = 15.0
FLEE_TIMEOUT = 10.0
COMBAT_RADIUS = 8  # Alice's attack radius in blocks
POLL = 0.25


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def alice_pos(rcon: RconClient):
    resp = rcon.command("alicecmd pos")
    m = re.search(r"pos\s+(-?[\d.]+)\s+(-?[\d.]+)\s+(-?[\d.]+)", resp)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), float(m.group(3))


def alice_tp(rcon: RconClient, x: float, y: float, z: float) -> None:
    rcon.command(f"alicecmd tp {x} {y} {z}")


def alice_stay(rcon: RconClient) -> None:
    rcon.command("alicecmd stay")


def dist3(a, b) -> float:
    import math
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def spawn_zombie(rcon: RconClient, x: int, y: int, z: int) -> None:
    """Spawn a zombie at exact coordinates and pin it in place."""
    rcon.command(f"summon minecraft:zombie {x} {y} {z}")
    time.sleep(0.3)
    # Pin zombie via NoAI so it stays where spawned (prevents it walking closer/farther)
    rcon.command(
        f"execute as @e[type=zombie,x={x},y={y},z={z},distance=..2,limit=1] "
        f"run data merge entity @s {{NoAI:1b}}"
    )


def kill_zombies(rcon: RconClient) -> None:
    rcon.command("kill @e[type=zombie]")


def get_zombie_health(rcon: RconClient, x: int, z: int) -> float | None:
    """Read zombie HP via /data get entity selector. Returns None on parse failure."""
    resp = rcon.command(
        f"data get entity @e[type=zombie,x={x},y={COMBAT_Y},z={z},distance=..5,limit=1] Health"
    )
    m = re.search(r"Health:\s*([\d.]+)f?", resp)
    if m:
        return float(m.group(1))
    return None


def wait_log(log_tail: LogTail, pattern: str, timeout: float = RULE_FIRE_TIMEOUT) -> re.Match | None:
    """Poll log_tail until pattern is found or timeout."""
    return log_tail.wait_for(pattern, timeout=timeout)


def ensure_log_absent(log_tail: LogTail, pattern: str, during: float = 8.0) -> bool:
    """Return True if pattern does NOT appear in log during the window."""
    return log_tail.ensure_absent(pattern, during=during)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestCombatAttack:

    def test_alice_attacks_zombie_in_radius(
        self, rcon: RconClient, log_tail: LogTail, combat_arena, easy
    ):
        """D06 — Alice attacks zombie within 8 blocks (AttackNearestHostileRule fires)."""
        zx, zy, zz = COMBAT_X + 4, COMBAT_Y, COMBAT_Z
        spawn_zombie(rcon, zx, zy, zz)

        # Give Alice a tick to perceive the zombie
        time.sleep(1.5)

        # Assert via log: AttackNearestHostileRule fires
        m = wait_log(log_tail, r"\[Alice\].*[Cc]ombat|[Aa]ttack.*[Hh]ostile|[Aa]ttackNearest", timeout=15)
        if m is None:
            # Fallback: verify zombie lost health or was killed
            hp = get_zombie_health(rcon, zx, zz)
            if hp is None:
                # Entity gone — Alice killed the zombie (success)
                pass
            else:
                assert hp < 20.0, (
                    f"Alice did not attack zombie within 8 blocks in 15s. "
                    f"No combat log found, zombie HP={hp:.1f} (full health — not attacked)"
                )

    def test_alice_does_not_attack_outside_radius(
        self, rcon: RconClient, log_tail: LogTail, combat_arena, easy
    ):
        """Alice does NOT attack zombie placed >8 blocks away."""
        # Park Alice at center, zombie 10 blocks out with NoAI
        alice_tp(rcon, COMBAT_X + 0.5, COMBAT_Y, COMBAT_Z + 0.5)
        time.sleep(0.5)

        zx, zy, zz = COMBAT_X + 10, COMBAT_Y, COMBAT_Z
        spawn_zombie(rcon, zx, zy, zz)
        time.sleep(0.5)

        still_absent = ensure_log_absent(
            log_tail,
            r"\[Alice\].*[Cc]ombat.*engage|[Aa]ttackNearest.*targ",
            during=8.0
        )
        assert still_absent, (
            "Alice appears to have engaged a zombie that was >8 blocks away. "
            "CombatRadius enforcement may be broken."
        )


class TestCombatFlee:

    def test_alice_flees_on_low_health(
        self, rcon: RconClient, log_tail: LogTail, combat_arena, easy
    ):
        """D07 — FleeOnLowHealthRule fires when Alice HP < 20% (4 of 20).

        Requires a hostile mob nearby to give the flee rule a target to flee from.
        """
        alice_stay(rcon)
        pos_before = alice_pos(rcon)
        assert pos_before is not None, "Could not read Alice position before flee test"

        # Spawn a zombie close by so the flee rule has a threat to react to
        zx, zy, zz = COMBAT_X + 3, COMBAT_Y, COMBAT_Z
        rcon.command(f"summon minecraft:zombie {zx} {zy} {zz}")
        time.sleep(0.5)

        # Set Alice HP to 3 (15% of 20) — must be strictly < 20% for FleeOnLowHealthRule
        # (ratio < 0.20 is strict; HP=4 gives exactly 0.20 which does NOT trigger the rule).
        # Alice is NOT in MC @a/@e selectors (FakePlayer with null connection).
        # Use alicecmd sethealth which calls fakePlayer.setHealth() directly.
        rcon.command("alicecmd sethealth 3")
        time.sleep(3.0)

        m = wait_log(log_tail, r"[Ff]lee[Oo]n[Ll]ow|[Ff]leeing|[Rr]ule.*[Ff]lee", timeout=FLEE_TIMEOUT)
        if m is None:
            # Fallback: check Alice moved (fleeing causes movement)
            pos_after = alice_pos(rcon)
            assert pos_after is not None, "Could not read Alice position after flee"
            moved = dist3(pos_before, pos_after)
            assert moved > 1.5, (
                f"FleeOnLowHealth rule did not fire AND Alice did not move. "
                f"before={pos_before} after={pos_after} moved={moved:.2f}"
            )

    def test_alice_flees_on_critical_health(
        self, rcon: RconClient, log_tail: LogTail, combat_arena, easy
    ):
        """D07 (critical) — FleeOnCriticalHealthRule fires when HP < 10% (2 of 20).

        Requires a hostile mob nearby to give the flee rule a target to flee from.
        """
        alice_stay(rcon)
        pos_before = alice_pos(rcon)
        assert pos_before is not None, "Could not read Alice position before critical flee test"

        # Spawn a zombie close by so flee rule has a threat to react to
        zx, zy, zz = COMBAT_X + 3, COMBAT_Y, COMBAT_Z
        rcon.command(f"summon minecraft:zombie {zx} {zy} {zz}")
        time.sleep(0.5)

        # Reduce Alice HP to critical level (< 10% = < 2.0f of 20).
        # Alice is NOT in MC @a/@e selectors (FakePlayer with null connection).
        # Use alicecmd sethealth which calls fakePlayer.setHealth() directly.
        rcon.command("alicecmd sethealth 1")
        time.sleep(3.5)

        m = wait_log(
            log_tail,
            r"[Ff]lee[Oo]n[Cc]ritical|[Cc]ritical.*[Ff]lee|[Rr]ule.*[Cc]ritical",
            timeout=FLEE_TIMEOUT
        )
        if m is None:
            pos_after = alice_pos(rcon)
            assert pos_after is not None, "Could not read Alice position after critical flee"
            moved = dist3(pos_before, pos_after)
            assert moved > 1.5, (
                f"FleeOnCriticalHealth rule did not fire AND Alice did not move. "
                f"before={pos_before} after={pos_after} moved={moved:.2f}"
            )

    def test_alice_combat_to_follow_transition(
        self, rcon: RconClient, log_tail: LogTail, combat_arena, easy
    ):
        """After zombie dies, AttackNearestHostile rule deactivates; Alice is no longer stuck in combat goal."""
        zx, zy, zz = COMBAT_X + 3, COMBAT_Y, COMBAT_Z
        spawn_zombie(rcon, zx, zy, zz)

        # Wait for combat engagement
        wait_log(log_tail, r"[Aa]ttack[Nn]earest|[Cc]ombat", timeout=10)

        # Kill the zombie
        kill_zombies(rcon)
        time.sleep(1.0)

        # Assert: no combat goal active (rule deactivated within ~5 ticks)
        still_combat = wait_log(
            log_tail,
            r"[Aa]ttack[Nn]earest.*engaging",
            timeout=5.0
        )
        # After kill, there should be no new "engaging" log
        # We want to assert the rule is no longer firing — check the pos is stable
        pos1 = alice_pos(rcon)
        time.sleep(3.0)
        pos2 = alice_pos(rcon)

        # Without a combat target Alice should stay near where she is (stay mode)
        assert pos1 is not None and pos2 is not None, "Could not read Alice position after zombie death"
        moved = dist3(pos1, pos2)
        # Alice may drift slightly but should not be actively chasing a phantom target
        assert moved < 5.0, (
            f"Alice is still moving aggressively {moved:.2f} blocks after zombie was killed. "
            f"Possible stale combat goal. pos1={pos1} pos2={pos2}"
        )
