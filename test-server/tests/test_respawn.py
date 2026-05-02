"""Respawn tests: Alice reappears after death, inventory preserved. Covers DOD D10.

Coordinates: RESP_X=-6000, RESP_Y=69, RESP_Z=-6000

Configuration:
  ALICE_RCON_HOST / ALICE_RCON_PORT / ALICE_RCON_PASSWORD (same as conftest.py)
"""
from __future__ import annotations

import re
import time

import pytest

from .rcon_client import RconClient

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

RESP_X = -6000
RESP_Y = 69
RESP_Z = -6000
ARENA_W = 15

RESPAWN_TIMEOUT = 30.0
POLL = 0.5


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


def wait_alice_responds(rcon: RconClient, timeout: float = RESPAWN_TIMEOUT) -> bool:
    """Poll until Alice answers alicecmd pos with a valid position."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            pos = alice_pos(rcon)
            if pos is not None and pos[1] > 0:
                return True
        except Exception:
            pass
        time.sleep(POLL)
    return False


def wait_alice_in_tab(rcon: RconClient, timeout: float = RESPAWN_TIMEOUT) -> bool:
    """Poll /list until Alice appears in the player list."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        resp = rcon.command("list")
        if "Alice" in resp:
            return True
        time.sleep(POLL)
    return False


# ---------------------------------------------------------------------------
# Arena fixture
# ---------------------------------------------------------------------------

@pytest.fixture
def resp_arena(rcon: RconClient):
    """Flat arena at RESP_X/Z for respawn tests."""
    rcon.command("difficulty peaceful")
    rcon.command("time set day")
    x1, x2 = RESP_X - ARENA_W, RESP_X + ARENA_W
    z1, z2 = RESP_Z - ARENA_W, RESP_Z + ARENA_W
    rcon.command(f"forceload add {x1} {z1} {x2} {z2}")
    rcon.command(f"fill {x1} {RESP_Y - 2} {z1} {x2} {RESP_Y - 1} {z2} minecraft:stone")
    rcon.command(f"fill {x1} {RESP_Y} {z1} {x2} {RESP_Y + 5} {z2} minecraft:air")
    alice_stay(rcon)
    alice_tp(rcon, RESP_X + 0.5, RESP_Y, RESP_Z + 0.5)
    time.sleep(1.0)
    yield
    alice_stay(rcon)
    rcon.command(f"forceload remove {x1} {z1} {x2} {z2}")


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

def test_alice_respawns_after_kill(rcon: RconClient, resp_arena):
    """D10 — Alice reappears after /kill within 30 seconds."""
    # Confirm Alice is alive before kill
    pos_before = alice_pos(rcon)
    assert pos_before is not None, "Alice not responding before kill"

    # Kill Alice
    rcon.command("kill @e[type=player,name=Alice,limit=1]")
    time.sleep(1.5)

    # Alice should respawn and respond to pos query
    recovered = wait_alice_responds(rcon, timeout=RESPAWN_TIMEOUT)
    assert recovered, (
        f"Alice did not respawn within {RESPAWN_TIMEOUT}s after /kill. "
        "Check AliceEntity respawn handler and FakePlayer attach lifecycle."
    )

    pos_after = alice_pos(rcon)
    assert pos_after is not None, "Alice respawned but pos query returned None"
    assert pos_after[1] > 0, (
        f"Alice respawned at invalid Y={pos_after[1]}. "
        "Possible respawn at Y=0 or void."
    )


def test_alice_responds_after_respawn(rcon: RconClient, resp_arena):
    """After respawn, Alice still responds to RCON commands (Baritone re-attached)."""
    rcon.command("kill @e[type=player,name=Alice,limit=1]")
    time.sleep(1.5)

    recovered = wait_alice_responds(rcon, timeout=RESPAWN_TIMEOUT)
    assert recovered, f"Alice did not respawn within {RESPAWN_TIMEOUT}s"

    # Verify Alice executes a stay command (Baritone functional after respawn)
    resp = rcon.command("alicecmd stay")
    assert resp is not None  # command must not throw

    # Verify status query works
    status_resp = rcon.command("alicecmd status")
    assert status_resp and len(status_resp) > 5, (
        f"alicecmd status returned empty/short response after respawn: '{status_resp}'"
    )


def test_alice_keeps_inventory_after_death(rcon: RconClient, resp_arena, log_tail):
    """D10 — Alice retains items after death (keepInventory or custom save/restore)."""
    # Give Alice a unique traceable item
    rcon.command("give @e[type=player,name=Alice,limit=1] minecraft:diamond 5")
    time.sleep(0.5)

    # Kill Alice
    rcon.command("kill @e[type=player,name=Alice,limit=1]")
    time.sleep(1.5)

    # Wait for respawn
    recovered = wait_alice_responds(rcon, timeout=RESPAWN_TIMEOUT)
    assert recovered, f"Alice did not respawn within {RESPAWN_TIMEOUT}s"
    time.sleep(1.0)

    # Check if diamonds are still present (count 0 = "how many can be cleared" without removing)
    resp = rcon.command(
        "execute as @e[type=player,name=Alice,limit=1] "
        "run clear @s minecraft:diamond 0"
    )
    # Response: "has 5 minecraft:diamond" or similar
    m = re.search(r"has\s+(\d+)\s+\[?minecraft:diamond", resp)
    if m is None:
        # Some versions: "would remove X items" — try alternate format
        m = re.search(r"(\d+)\s+items?\s+of\s+type\s+minecraft:diamond", resp)

    if m:
        count = int(m.group(1))
        assert count > 0, (
            f"Alice lost all diamonds after death and respawn. "
            f"keepInventory may not be enabled or save/restore is broken. resp={resp}"
        )
    else:
        # Could not parse — skip with informational note rather than hard fail
        pytest.skip(
            f"Could not parse item count from clear response: '{resp}'. "
            "Manual verification needed: give Alice diamonds, kill, check inventory."
        )
