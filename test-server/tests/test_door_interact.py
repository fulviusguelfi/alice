"""Door/trapdoor interact tests. Run against a live Alice-enabled server via RCON.

Driven by /alicecmd commands (registered by AliceCommands.java) so tests don't
need a real player to send chat messages. All tests pick coordinates far from
spawn (x=-6000..-5000) to avoid disturbing any village/player structures.

Configuration via env vars:
  ALICE_RCON_HOST (default 127.0.0.1) — 192.168.0.225 for Larry
  ALICE_RCON_PORT (default 25575)
  ALICE_RCON_PASSWORD (default alicetest) — alice2026 for Larry

Run:
  ALICE_RCON_HOST=192.168.0.225 ALICE_RCON_PASSWORD=alice2026 \
    pytest test-server/tests/test_door_interact.py -v
"""
from __future__ import annotations

import re
import time

import pytest

from .rcon_client import RconClient


# ---------------------------------------------------------------------------
# Test-area coordinates (flat, far from any existing structure)
# ---------------------------------------------------------------------------

BASE_X = -5500
BASE_Y = 68
BASE_Z = -5500
ARENA_W = 10  # half-width around BASE for the test arena


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def setblock(rcon: RconClient, x: int, y: int, z: int, block: str) -> str:
    return rcon.command(f"setblock {x} {y} {z} {block} replace")


def fill(rcon: RconClient, x1: int, y1: int, z1: int, x2: int, y2: int, z2: int, block: str) -> str:
    return rcon.command(f"fill {x1} {y1} {z1} {x2} {y2} {z2} {block} replace")


def tp_alice(rcon: RconClient, x: float, y: int, z: float) -> None:
    # /tp Alice does NOT work — FakePlayer is not selectable via entity selectors.
    # Use our custom /alicecmd tp which calls fakePlayer.moveTo() directly.
    rcon.command(f"alicecmd tp {x} {y} {z}")


def block_matches(rcon: RconClient, x: int, y: int, z: int, blockspec: str) -> bool:
    """Query block via `/execute if block X Y Z <spec>`. True if spec matches.

    `/data get block` only works on block entities; doors/gates/trapdoors are not
    block entities, so we use the `execute if block` predicate instead, which
    returns "Test passed" / "Test failed" over RCON.
    """
    resp = rcon.command(f"execute if block {x} {y} {z} {blockspec}")
    return "Test passed" in resp


def block_is_open(rcon: RconClient, x: int, y: int, z: int, block_type: str) -> bool | None:
    """Return True if block has open=true, False if open=false, None if neither matches."""
    if block_matches(rcon, x, y, z, f"{block_type}[open=true]"):
        return True
    if block_matches(rcon, x, y, z, f"{block_type}[open=false]"):
        return False
    return None


def alice_pos(rcon: RconClient) -> tuple[float, float, float] | None:
    """Get Alice position via /alicecmd pos (exact float coords)."""
    resp = rcon.command("alicecmd pos")
    m = re.search(r"pos\s+(-?[\d.]+)\s+(-?[\d.]+)\s+(-?[\d.]+)", resp)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), float(m.group(3))


def alice_goto(rcon: RconClient, x: int, y: int, z: int) -> str:
    return rcon.command(f"alicecmd goto {x} {y} {z}")


def alice_stay(rcon: RconClient) -> str:
    return rcon.command("alicecmd stay")


def clean_arena(rcon: RconClient) -> None:
    """Wipe arena volume to stone floor + air above.

    Forceload the covering chunks first — at distant coords the chunk may not be
    loaded and `fill` fails silently with "That position is not loaded".
    """
    x1, x2 = BASE_X - ARENA_W, BASE_X + ARENA_W
    z1, z2 = BASE_Z - ARENA_W, BASE_Z + ARENA_W
    rcon.command(f"forceload add {x1} {z1} {x2} {z2}")
    # Floor (thick so Alice can't fall through if terrain below is hollow)
    fill(rcon, x1, BASE_Y - 2, z1, x2, BASE_Y - 1, z2, "minecraft:stone")
    # Interior air
    fill(rcon, x1, BASE_Y, z1, x2, BASE_Y + 5, z2, "minecraft:air")


def wait_reached(rcon: RconClient, predicate, timeout: float = 45.0, poll: float = 0.5) -> tuple[bool, tuple[float, float, float] | None]:
    """Poll alice_pos until predicate(pos) is True or timeout."""
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        pos = alice_pos(rcon)
        if pos is not None:
            last = pos
            if predicate(pos):
                return True, pos
        time.sleep(poll)
    return False, last


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def arena(rcon: RconClient):
    """Per-test arena wipe; ensures a clean, flat stone floor."""
    rcon.command("difficulty peaceful")
    rcon.command("time set day")
    clean_arena(rcon)
    # Park Alice at arena center
    tp_alice(rcon, BASE_X + 0.5, BASE_Y, BASE_Z + 0.5)
    alice_stay(rcon)
    time.sleep(1.0)
    yield
    alice_stay(rcon)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

def test_command_registered(rcon: RconClient):
    """Sanity: /alicecmd status replies with Alice's state."""
    resp = rcon.command("alicecmd status")
    assert "[Alice]" in resp, f"/alicecmd status returned unexpected: {resp!r}"
    assert "pos=" in resp, f"status missing pos field: {resp!r}"


def test_alice_opens_oak_door(rcon: RconClient, arena):
    """Alice should open a closed oak door to cross a 1-block-thick wall.

    Layout (facing +X; wall runs along z-axis at x=BASE_X+4):

        BASE_X+4  wall of stone (y=68..70) with DOOR at z=BASE_Z (y=68 lower, y=69 upper)

    Alice spawns west of wall (BASE_X), goal is east of wall (BASE_X+8).
    """
    door_x = BASE_X + 4
    door_z = BASE_Z
    # Build wall: solid at all z in [BASE_Z-2, BASE_Z+2] except (door_x, 68..69, door_z)
    for z in range(BASE_Z - 2, BASE_Z + 3):
        for y in (BASE_Y, BASE_Y + 1, BASE_Y + 2):
            setblock(rcon, door_x, y, z, "minecraft:stone")
    # Carve door opening
    setblock(rcon, door_x, BASE_Y, door_z, "minecraft:oak_door[half=lower,facing=east]")
    setblock(rcon, door_x, BASE_Y + 1, door_z, "minecraft:oak_door[half=upper,facing=east]")
    # Verify door is placed and currently closed
    time.sleep(0.5)
    assert block_matches(rcon, door_x, BASE_Y, door_z, "minecraft:oak_door"), \
        f"door did not place at ({door_x},{BASE_Y},{door_z})"
    assert block_is_open(rcon, door_x, BASE_Y, door_z, "minecraft:oak_door") is False, \
        f"door did not start closed"

    # Spawn Alice west of wall, goal east of wall
    tp_alice(rcon, BASE_X + 0.5, BASE_Y, door_z + 0.5)
    time.sleep(1.0)
    alice_goto(rcon, BASE_X + 8, BASE_Y, door_z)

    # Alice must reach the east side of the wall (x >= door_x + 1) within 45s
    reached, final = wait_reached(rcon, lambda p: p[0] >= door_x + 1 and abs(p[2] - door_z) < 3.0, timeout=45.0)

    # Also check door state post-crossing
    open_after = block_is_open(rcon, door_x, BASE_Y, door_z, "minecraft:oak_door")

    assert reached, f"Alice did not cross the door within 45s (last pos={final}, open_after={open_after})"
    assert open_after is True, f"Alice crossed but door is not open (open={open_after})"


def test_alice_opens_fence_gate(rcon: RconClient, arena):
    """Fence gate variant — separate block class from door, uses isGatePassable."""
    gate_x = BASE_X + 4
    gate_z = BASE_Z
    # Wall
    for z in range(BASE_Z - 2, BASE_Z + 3):
        for y in (BASE_Y, BASE_Y + 1, BASE_Y + 2):
            setblock(rcon, gate_x, y, z, "minecraft:oak_planks")
    # Gate
    setblock(rcon, gate_x, BASE_Y, gate_z, "minecraft:oak_fence_gate[facing=east]")
    setblock(rcon, gate_x, BASE_Y + 1, gate_z, "minecraft:air")

    time.sleep(0.5)
    assert block_matches(rcon, gate_x, BASE_Y, gate_z, "minecraft:oak_fence_gate"), \
        "fence gate did not place"
    assert block_is_open(rcon, gate_x, BASE_Y, gate_z, "minecraft:oak_fence_gate") is False, \
        "gate did not start closed"

    tp_alice(rcon, BASE_X + 0.5, BASE_Y, gate_z + 0.5)
    time.sleep(1.0)
    alice_goto(rcon, BASE_X + 8, BASE_Y, gate_z)

    reached, final = wait_reached(rcon, lambda p: p[0] >= gate_x + 1 and abs(p[2] - gate_z) < 3.0, timeout=45.0)
    open_after = block_is_open(rcon, gate_x, BASE_Y, gate_z, "minecraft:oak_fence_gate")

    assert reached, f"Alice did not cross the fence gate within 45s (last pos={final}, open_after={open_after})"
    assert open_after is True, f"Alice crossed but gate is not open (open={open_after})"


@pytest.mark.xfail(reason="TrapDoor-as-horizontal-wall requires trapdoor oriented to block traversal — may need redstone preview")
def test_alice_opens_horizontal_trapdoor(rcon: RconClient, arena):
    """Trapdoor blocking horizontal path (mounted on side). Alice should right-click to open.

    TrapDoor placed vertically along +X so it blocks a 1-block passage.
    """
    trap_x = BASE_X + 4
    trap_z = BASE_Z
    # Wall with slot for trapdoor
    for z in range(BASE_Z - 2, BASE_Z + 3):
        for y in (BASE_Y, BASE_Y + 1, BASE_Y + 2):
            if z == trap_z and y == BASE_Y:
                continue
            setblock(rcon, trap_x, y, z, "minecraft:oak_planks")
    # Trapdoor as side-barrier (closed, facing=west so it blocks +X travel)
    setblock(rcon, trap_x, BASE_Y, trap_z,
             "minecraft:oak_trapdoor[facing=west,half=bottom,open=false]")

    tp_alice(rcon, BASE_X + 0.5, BASE_Y, trap_z + 0.5)
    time.sleep(0.5)
    alice_goto(rcon, BASE_X + 8, BASE_Y, trap_z)

    reached, final = wait_reached(rcon, lambda p: p[0] >= trap_x + 1 and abs(p[2] - trap_z) < 3.0, timeout=45.0)
    assert reached, f"Alice did not cross trapdoor within 45s (last pos={final})"
