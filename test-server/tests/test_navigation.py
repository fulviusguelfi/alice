"""Navigation tests: goto, stay, ascend, descend. Covers DOD D04 + D05.

All tests run in an isolated flat arena at NAV_X/Z. No real player required.

Configuration:
  ALICE_RCON_HOST / ALICE_RCON_PORT / ALICE_RCON_PASSWORD (same as other suites)
"""
from __future__ import annotations

import math
import re
import time

import pytest

from .rcon_client import RconClient

# ---------------------------------------------------------------------------
# Arena coordinates — far from spawn to avoid chunk/mob interference
# ---------------------------------------------------------------------------

NAV_X = -5900
NAV_Y = 69
NAV_Z = -5900
ARENA_W = 25
MOVE_TIMEOUT = 35.0
POLL = 0.25


# ---------------------------------------------------------------------------
# Helpers (duplicated locally to avoid cross-file fixture coupling)
# ---------------------------------------------------------------------------

def alice_pos(rcon: RconClient):
    resp = rcon.command("alicecmd pos")
    m = re.search(r"pos\s+(-?[\d.]+)\s+(-?[\d.]+)\s+(-?[\d.]+)", resp)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), float(m.group(3))


def alice_tp(rcon: RconClient, x: float, y: float, z: float) -> None:
    rcon.command(f"alicecmd tp {x} {y} {z}")


def alice_goto(rcon: RconClient, x: int, y: int, z: int) -> str:
    return rcon.command(f"alicecmd goto {x} {y} {z}")


def alice_stay(rcon: RconClient) -> None:
    rcon.command("alicecmd stay")


def dist3(a, b) -> float:
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def wait_alice_near(rcon: RconClient, goal, threshold: float = 2.0,
                    timeout: float = MOVE_TIMEOUT) -> tuple[bool, tuple | None]:
    """Poll until Alice is within `threshold` blocks of goal. Returns (reached, last_pos)."""
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        pos = alice_pos(rcon)
        if pos is not None:
            last = pos
            if dist3(pos, goal) <= threshold:
                return True, pos
        time.sleep(POLL)
    return False, last


def wait_y_gte(rcon: RconClient, target_y: float, timeout: float = MOVE_TIMEOUT) -> tuple[bool, tuple | None]:
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        pos = alice_pos(rcon)
        if pos is not None:
            last = pos
            if pos[1] >= target_y - 0.5:
                return True, pos
        time.sleep(POLL)
    return False, last


def wait_y_lte(rcon: RconClient, target_y: float, timeout: float = MOVE_TIMEOUT) -> tuple[bool, tuple | None]:
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        pos = alice_pos(rcon)
        if pos is not None:
            last = pos
            if pos[1] <= target_y + 0.6:
                return True, pos
        time.sleep(POLL)
    return False, last


# ---------------------------------------------------------------------------
# Arena fixture
# ---------------------------------------------------------------------------

@pytest.fixture
def nav_arena(rcon: RconClient):
    """Flat 50×50 stone floor arena at NAV_X/Z, air above."""
    rcon.command("difficulty peaceful")
    rcon.command("time set day")
    x1, x2 = NAV_X - ARENA_W, NAV_X + ARENA_W
    z1, z2 = NAV_Z - ARENA_W, NAV_Z + ARENA_W
    rcon.command(f"forceload add {x1} {z1} {x2} {z2}")
    rcon.command(f"fill {x1} {NAV_Y - 2} {z1} {x2} {NAV_Y - 1} {z2} minecraft:stone")
    rcon.command(f"fill {x1} {NAV_Y} {z1} {x2} {NAV_Y + 10} {z2} minecraft:air")
    alice_stay(rcon)
    alice_tp(rcon, NAV_X + 0.5, NAV_Y, NAV_Z + 0.5)
    time.sleep(1.0)
    yield
    alice_stay(rcon)
    rcon.command(f"forceload remove {x1} {z1} {x2} {z2}")


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

def test_alice_goto_open_field(rcon: RconClient, nav_arena):
    """D05 — Alice navigates to a coordinate 15 blocks away in open air."""
    goal = (NAV_X + 15, NAV_Y, NAV_Z)
    alice_goto(rcon, *map(int, goal))

    reached, last = wait_alice_near(rcon, goal, threshold=2.5, timeout=35)
    assert reached, (
        f"Alice did not reach {goal} within 35s. last_pos={last} "
        f"dist={dist3(last, goal):.1f} if last else 'unknown'"
    )


def test_alice_stay_stops_navigation(rcon: RconClient, nav_arena):
    """D04 — /alicecmd stay cancels active navigation immediately."""
    far_goal = (NAV_X + 22, NAV_Y, NAV_Z)
    alice_goto(rcon, *map(int, far_goal))
    time.sleep(2.5)  # Alice begins moving

    pos_before = alice_pos(rcon)
    alice_stay(rcon)
    time.sleep(3.5)  # Give time to stop

    pos_after = alice_pos(rcon)
    assert pos_before is not None and pos_after is not None, \
        "Could not read Alice position"
    moved = dist3(pos_before, pos_after)
    assert moved < 2.0, (
        f"Alice moved {moved:.2f} blocks after stay command. "
        f"before={pos_before} after={pos_after}"
    )


def test_alice_ascends_one_block_step(rcon: RconClient, nav_arena):
    """Alice uses MovementAscend to step up 1 block.

    Arena floor is at NAV_Y-1=68 (stone). Alice walks at NAV_Y=69.
    Platform blocks placed at NAV_Y=69 create a 1-block step; top is at NAV_Y+1=70.
    """
    px = NAV_X + 8
    # Place stone at NAV_Y=69 to create a 1-block step above the arena floor
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            rcon.command(f"setblock {px + dx} {NAV_Y} {NAV_Z + dz} minecraft:stone replace")

    # Place Alice 3 blocks south of the platform
    alice_tp(rcon, px - 3 + 0.5, NAV_Y, NAV_Z + 0.5)
    time.sleep(0.8)
    # Goal: on top of the step (NAV_Y+1=70), at the platform center
    goal = (px, NAV_Y + 1, NAV_Z)
    alice_goto(rcon, *map(int, goal))

    reached, last = wait_y_gte(rcon, NAV_Y + 0.8, timeout=25)
    assert reached, (
        f"Alice did not ascend to y>={NAV_Y + 0.8} within 25s. last_pos={last}"
    )


def test_alice_descends_two_blocks(rcon: RconClient, nav_arena):
    """Alice descends from a 2-block-high platform to ground level."""
    # Build 2-block-high platform
    px = NAV_X + 10
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            rcon.command(f"setblock {px + dx} {NAV_Y} {NAV_Z + dz} minecraft:stone replace")

    alice_tp(rcon, px + 0.5, NAV_Y + 1, NAV_Z + 0.5)
    time.sleep(0.8)
    # Goal at ground level, 6 blocks away
    goal = (NAV_X - 5, NAV_Y, NAV_Z)
    alice_goto(rcon, *map(int, goal))

    reached, last = wait_y_lte(rcon, NAV_Y, timeout=25)
    assert reached, (
        f"Alice did not descend to y<={NAV_Y + 0.6} within 25s. last_pos={last}"
    )


def test_alice_goto_sequential_responsiveness(rcon: RconClient, nav_arena):
    """D04 smoke — Alice executes sequential gotos, demonstrating navigation responsiveness."""
    waypoints = [
        (NAV_X + 12, NAV_Y, NAV_Z),
        (NAV_X + 12, NAV_Y, NAV_Z + 12),
        (NAV_X, NAV_Y, NAV_Z + 12),
    ]
    for goal in waypoints:
        alice_goto(rcon, *map(int, goal))
        reached, last = wait_alice_near(rcon, goal, threshold=3.0, timeout=35)
        assert reached, (
            f"Alice did not reach waypoint {goal} within 35s. "
            f"last_pos={last} dist={dist3(last, goal):.1f if last else 'unknown'}"
        )
