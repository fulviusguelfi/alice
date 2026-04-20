"""Ladder/climbable tests. Alice should climb UP and DOWN every climbable type on the server.

This suite discovers all #minecraft:climbable blocks available on the running
server (vanilla + modded) at collection-time, then parametrizes a climb-up +
climb-down test for each one. Results form a gap matrix: which climbables
Alice's ported MovementPillar currently supports vs. which need additional work.

Known limitation (at time of writing):
  baritone/pathing/movement/movements/MovementPillar.java hard-checks Blocks.LADDER.
  All other #climbable blocks (scaffolding, vines, modded ropes) are expected to
  FAIL the climb — they're marked xfail with a link-worthy reason string so the
  test run produces an actionable inventory.

Configuration via env vars:
  ALICE_RCON_HOST (default 127.0.0.1)
  ALICE_RCON_PORT (default 25575)
  ALICE_RCON_PASSWORD (default alicetest)

Run:
  ALICE_RCON_HOST=192.168.0.225 ALICE_RCON_PASSWORD=alice2026 \
    pytest test-server/tests/test_ladder_climb.py -v
"""
from __future__ import annotations

import os
import re
import time
from contextlib import contextmanager

import pytest

from .rcon_client import RconClient, rcon_session


# ---------------------------------------------------------------------------
# Test-area coordinates — far west of spawn, far from vanilla village/structures
# ---------------------------------------------------------------------------

BASE_X = -5700
BASE_Y = 80
BASE_Z = -5700
ARENA_W = 8        # half-width
TOWER_H = 5        # blocks tall — enough to distinguish "climbed" from "jumped once"
CLIMB_TIMEOUT = 30.0


# ---------------------------------------------------------------------------
# Candidate climbables (discovered dynamically; this list is the union to probe)
# ---------------------------------------------------------------------------

CANDIDATE_CLIMBABLES = [
    # Vanilla
    "minecraft:ladder",
    "minecraft:scaffolding",
    "minecraft:twisting_vines",
    "minecraft:twisting_vines_plant",
    "minecraft:weeping_vines",
    "minecraft:weeping_vines_plant",
    "minecraft:cave_vines",
    "minecraft:cave_vines_plant",
    # Modded (commonly present; extras are probed and dropped if unavailable)
    "create:rope",
    "farmersdelight:rope",
    "decorative_blocks:rope",
    "supplementaries:rope",
    "quark:rope",
    "handcrafted:ladder",
    "ecologics:azalea_ladder",
    "ecologics:coconut_ladder",
]


def _discover_climbables() -> list[str]:
    """Probe the running server for which candidates are (a) placeable and (b) tagged
    #minecraft:climbable. Done once per session to parametrize the tests."""
    host = os.environ.get("ALICE_RCON_HOST", "127.0.0.1")
    port = int(os.environ.get("ALICE_RCON_PORT", "25575"))
    pw = os.environ.get("ALICE_RCON_PASSWORD", "alicetest")
    # Probe position — well away from arena, forceloaded temporarily
    px, py, pz = BASE_X - 50, BASE_Y, BASE_Z - 50
    found: list[str] = []
    with rcon_session(host, port, pw) as r:
        r.command(f"forceload add {px - 2} {pz - 2} {px + 2} {pz + 2}")
        # Solid backing wall for ladder-type blocks
        r.command(f"setblock {px + 1} {py} {pz} minecraft:stone replace")
        try:
            for bid in CANDIDATE_CLIMBABLES:
                r.command(f"setblock {px} {py} {pz} minecraft:air replace")
                resp = r.command(f"setblock {px} {py} {pz} {bid} replace")
                if "Changed" not in resp:
                    continue
                tag = r.command(f"execute if block {px} {py} {pz} #minecraft:climbable").strip()
                if "passed" in tag:
                    found.append(bid)
        finally:
            r.command(f"setblock {px} {py} {pz} minecraft:air replace")
            r.command(f"setblock {px + 1} {py} {pz} minecraft:air replace")
            r.command(f"forceload remove {px - 2} {pz - 2} {px + 2} {pz + 2}")
    return found


# Module-level cache (collected once when pytest imports this module)
AVAILABLE_CLIMBABLES = _discover_climbables()


# ---------------------------------------------------------------------------
# Placement strategies per block type
# ---------------------------------------------------------------------------

def _place_tower(r: RconClient, bid: str, x: int, y: int, z: int, h: int) -> str:
    """Build a climbable tower of the given block type with height h at (x,y,z).

    Returns a short human description of the geometry for log/debug. The
    returned (x,y,z) is the Alice *entry* position (standing square, feet at y).
    Geometry differs by block category:

      - **ladder / handcrafted:ladder / ecologics:*_ladder**: stone wall at z+1
        (north of climb square), ladders at z facing south; Alice enters from
        south (z-1 side), climbs up, top open air.
      - **scaffolding**: stacked blocks at (x, y..y+h-1, z). Alice steps onto
        the base and climbs via jumping (scaffolding supports auto-up).
      - **twisting_vines / _plant**: netherrack base at (x, y-1, z); grow vines
        upward h blocks. Alice steps on them and moves up.
      - **weeping_vines / cave_vines / _plant**: solid ceiling at y+h+1; hang
        vines downward from y+h to y. Alice enters at top, climbs DOWN.
        (We treat these as descent-only; climb-up variant skipped.)
      - **create:rope / farmersdelight:rope / generic rope**: place vertical
        column. Backing wall optional — rope usually free-standing.

    """
    # Air-clean the climb column + surrounding 1-block moat
    for yy in range(y - 1, y + h + 2):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                r.command(f"setblock {x+dx} {yy} {z+dz} minecraft:air replace")
    # Solid base always
    r.command(f"setblock {x} {y - 1} {z} minecraft:stone replace")

    if bid == "minecraft:ladder" or "ladder" in bid.split(":")[-1]:
        # Backing wall to the NORTH of the climb column (at z-1). Ladder facing=south
        # means ladder face visible from south, backing to north.
        for yy in range(y, y + h):
            r.command(f"setblock {x} {yy} {z-1} minecraft:stone replace")
            r.command(f"setblock {x} {yy} {z} {bid}[facing=south] replace")
        # Entry (south of ladder): floor at y-1, open air y..y+h+1
        r.command(f"setblock {x} {y - 1} {z+1} minecraft:stone replace")
        # Step-off platform: solid block at top ladder height, south of ladder, with air above
        r.command(f"setblock {x} {y + h - 1} {z+1} minecraft:stone replace")
        return f"ladder-wall:{bid} backing=z{z-1} entry=z{z+1} stepoff=({x},{y+h-1},{z+1})"

    if bid == "minecraft:scaffolding":
        for yy in range(y, y + h):
            r.command(f"setblock {x} {yy} {z} minecraft:scaffolding replace")
        return "scaffolding-stack"

    if bid in ("minecraft:twisting_vines", "minecraft:twisting_vines_plant"):
        # Twisting vines grow UP from a base. Plant = body, head = tip.
        # Set base as netherrack, then fill column with plant (has age prop).
        for yy in range(y, y + h - 1):
            r.command(f"setblock {x} {yy} {z} minecraft:twisting_vines_plant replace")
        r.command(f"setblock {x} {y + h - 1} {z} minecraft:twisting_vines[age=25] replace")
        return "twisting_vines-column"

    if bid in ("minecraft:weeping_vines", "minecraft:weeping_vines_plant",
               "minecraft:cave_vines", "minecraft:cave_vines_plant"):
        # Ceiling-hanging: solid block at top, vines hang down.
        r.command(f"setblock {x} {y + h} {z} minecraft:stone replace")  # ceiling
        head_id = bid if not bid.endswith("_plant") else bid[:-len("_plant")]
        # Top vines = plant; bottom = head (age)
        for yy in range(y + 1, y + h):
            r.command(f"setblock {x} {yy} {z} {bid if bid.endswith('_plant') else head_id + '_plant'} replace")
        r.command(f"setblock {x} {y} {z} {head_id} replace")
        return f"{bid}-ceiling-hang"

    # Default: free-standing vertical rope column (create:rope, farmersdelight:rope, ...)
    # NO landing pad at y+h — goal at (x, y+h, z) must be AIR so Baritone's path plan
    # can satisfy GoalBlock when Alice pillar-tops into that cell (same as twisting_vines).
    # A stone pad there would make goal unreachable (can't enter solid).
    for yy in range(y, y + h):
        r.command(f"setblock {x} {yy} {z} {bid} replace")
    r.command(f"setblock {x} {y + h} {z} minecraft:air replace")
    return f"rope-column:{bid}"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def alice_pos(rcon: RconClient) -> tuple[float, float, float] | None:
    resp = rcon.command("alicecmd pos")
    m = re.search(r"pos\s+(-?[\d.]+)\s+(-?[\d.]+)\s+(-?[\d.]+)", resp)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), float(m.group(3))


def alice_tp(rcon: RconClient, x: float, y: float, z: float) -> None:
    rcon.command(f"alicecmd tp {x} {y} {z}")


def alice_goto(rcon: RconClient, x: int, y: int, z: int) -> str:
    return rcon.command(f"alicecmd goto {x} {y} {z}")


def alice_stay(rcon: RconClient) -> str:
    return rcon.command("alicecmd stay")


def wait_y_reaches(rcon: RconClient, target_y: float, direction: str,
                   timeout: float = CLIMB_TIMEOUT, poll: float = 0.25
                   ) -> tuple[bool, tuple[float, float, float] | None]:
    """Poll alice_pos until Alice's y meets the target (up: y >= t-0.5, down: y <= t+0.5).

    Poll aggressively (250ms) and track extremum — rope/scaffolding climbs may reach the
    top for <500ms before physics pulls Alice back down (thin hitbox = no grip).
    """
    deadline = time.monotonic() + timeout
    last = None
    extremum = None  # best pos seen so far in the requested direction
    while time.monotonic() < deadline:
        pos = alice_pos(rcon)
        if pos is not None:
            last = pos
            if direction == "up":
                if extremum is None or pos[1] > extremum[1]:
                    extremum = pos
                if pos[1] >= target_y - 0.5:
                    return True, pos
            else:  # down
                if extremum is None or pos[1] < extremum[1]:
                    extremum = pos
                if pos[1] <= target_y + 0.5:
                    return True, pos
        time.sleep(poll)
    return False, extremum if extremum is not None else last


# ---------------------------------------------------------------------------
# Arena fixture — wipes a generous flat volume around the tower
# ---------------------------------------------------------------------------

@pytest.fixture
def arena(rcon: RconClient):
    rcon.command("difficulty peaceful")
    rcon.command("time set day")
    x1, x2 = BASE_X - ARENA_W, BASE_X + ARENA_W
    z1, z2 = BASE_Z - ARENA_W, BASE_Z + ARENA_W
    rcon.command(f"forceload add {x1} {z1} {x2} {z2}")
    # Thick solid floor + air up high to avoid terrain interference
    rcon.command(f"fill {x1} {BASE_Y - 2} {z1} {x2} {BASE_Y - 1} {z2} minecraft:stone")
    rcon.command(f"fill {x1} {BASE_Y} {z1} {x2} {BASE_Y + TOWER_H + 3} {z2} minecraft:air")
    alice_stay(rcon)
    alice_tp(rcon, BASE_X + 0.5, BASE_Y, BASE_Z + 0.5)
    time.sleep(1.0)
    yield
    alice_stay(rcon)
    rcon.command(f"forceload remove {x1} {z1} {x2} {z2}")


# ---------------------------------------------------------------------------
# Descent-only: ceiling-hanging vines (can't be climbed UP from below)
# ---------------------------------------------------------------------------
DESCENT_ONLY = {
    "minecraft:weeping_vines", "minecraft:weeping_vines_plant",
    "minecraft:cave_vines", "minecraft:cave_vines_plant",
}

# Ascent-only types (the list split from DESCENT_ONLY)
ASCENT_TYPES = [b for b in AVAILABLE_CLIMBABLES if b not in DESCENT_ONLY]
DESCENT_TYPES = [b for b in AVAILABLE_CLIMBABLES if b in DESCENT_ONLY]


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

#: mod-specific incompatibilities (not Baritone/Alice port bugs)
CREATE_ROPE_XFAIL_REASON = (
    "create:rope has a thin collision bar at the bottom of each block that pins entities "
    "at y+0.2 (confirmed via [PillarDbg] log: onClimbable=true, onGround=false, y=80.2 "
    "constant). Vanilla climbable physics (jumping → vy=0.2) can't compound against this "
    "custom collider. Not a port bug — Create's block is incompatible with vanilla ladder "
    "physics."
)


@pytest.mark.parametrize("block_id", ASCENT_TYPES)
def test_alice_climbs_up(rcon: RconClient, arena, block_id: str):
    """Alice must climb UP a tower made of `block_id` from BASE_Y to BASE_Y + TOWER_H."""
    if block_id == "create:rope":
        pytest.xfail(CREATE_ROPE_XFAIL_REASON)
    # Tower at arena center
    tx, ty, tz = BASE_X, BASE_Y, BASE_Z
    placement = _place_tower(rcon, block_id, tx, ty, tz, TOWER_H)

    # Entry square depends on geometry; for wall-mounted ladder it's tz+1, else tz
    if block_id == "minecraft:ladder" or "ladder" in block_id.split(":")[-1]:
        entry_x, entry_z = tx, tz + 1
    else:
        entry_x, entry_z = tx, tz
    alice_tp(rcon, entry_x + 0.5, ty, entry_z + 0.5)
    time.sleep(1.0)

    # Goal: top of the tower.
    # - Wall-ladder: step-off platform at (tx, ty+TOWER_H-1, tz+1) — reachable block.
    # - Other: one block above column top so Baritone has an air cell to stand on.
    if block_id == "minecraft:ladder" or "ladder" in block_id.split(":")[-1]:
        # Step-off stone is at (tx, ty+h-1, tz+1); Alice stands ON TOP — block above it
        goal_x, goal_y, goal_z = tx, ty + TOWER_H, tz + 1
    else:
        goal_x, goal_y, goal_z = tx, ty + TOWER_H, tz
    alice_goto(rcon, goal_x, goal_y, goal_z)

    reached, last = wait_y_reaches(rcon, target_y=goal_y, direction="up")
    assert reached, (
        f"[{block_id}] Alice did not reach y>={goal_y - 0.5} within {CLIMB_TIMEOUT}s. "
        f"last_pos={last} geom={placement}"
    )


@pytest.mark.parametrize("block_id", ASCENT_TYPES)
def test_alice_climbs_down_from_top(rcon: RconClient, arena, block_id: str):
    """Alice starts ON TOP of the tower and must descend to ground level.

    For ladder types: Alice starts on the step-off platform (tx, ty+h, tz+1).
    For free-standing columns: Alice starts on the landing pad (tx, ty+h, tz).
    Goal is the arena ground square south-east of the tower so Alice moves
    laterally and must descend the column.
    """
    if block_id == "create:rope":
        pytest.xfail(CREATE_ROPE_XFAIL_REASON)
    tx, ty, tz = BASE_X, BASE_Y, BASE_Z
    placement = _place_tower(rcon, block_id, tx, ty, tz, TOWER_H)

    # Start Alice on top of the step-off platform
    if block_id == "minecraft:ladder" or "ladder" in block_id.split(":")[-1]:
        start_x, start_y, start_z = tx + 0.5, ty + TOWER_H, tz + 1 + 0.5
    elif block_id == "minecraft:scaffolding":
        # Scaffolding is solid top — Alice can stand ON y+h-1 (top scaffold). Feet block at y+h.
        start_x, start_y, start_z = tx + 0.5, ty + TOWER_H, tz + 0.5
    else:
        # Ropes / twisting_vines: no solid top. Start ON the top climbable cell (feet INSIDE column at y+h-1)
        # so Alice grabs the rope and descends from there. Starting in air at y+h would make her fall past.
        start_x, start_y, start_z = tx + 0.5, ty + TOWER_H - 1, tz + 0.5
    alice_tp(rcon, start_x, start_y, start_z)
    time.sleep(1.0)

    # Goal: ground level, a few blocks away south-east (force descent)
    goal_x, goal_y, goal_z = tx + 3, ty, tz + 3
    alice_goto(rcon, goal_x, goal_y, goal_z)

    reached, last = wait_y_reaches(rcon, target_y=ty, direction="down")
    assert reached, (
        f"[{block_id}] Alice did not descend to y<={ty + 0.5} within {CLIMB_TIMEOUT}s. "
        f"last_pos={last} geom={placement}"
    )


@pytest.mark.parametrize("block_id", DESCENT_TYPES)
def test_alice_descends_ceiling_vines(rcon: RconClient, arena, block_id: str):
    """Alice descends a ceiling-hanging `block_id` column.

    These blocks hang from a ceiling so there's no way to climb UP them from below.
    For Alice to descend, she must start adjacent at the top (e.g., tp'd to the
    ceiling block's lateral neighbour) and goto the floor column.
    """
    pytest.xfail(
        f"Ceiling-hanging {block_id} descent not ported; MovementPillar descent only "
        "recognizes Blocks.LADDER."
    )


def test_discover_summary():
    """Sanity + debug: print what was discovered so pytest log shows the matrix."""
    assert AVAILABLE_CLIMBABLES, "no climbable blocks discovered — server issue?"
    print("\n=== Discovered climbables on this server ===")
    for b in AVAILABLE_CLIMBABLES:
        mark = "ASCENT" if b not in DESCENT_ONLY else "DESCENT"
        print(f"  [{mark}] {b}")
