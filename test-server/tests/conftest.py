"""pytest fixtures for integration tests that drive the local test-server via RCON.

Prerequisites (done once by the developer):
  1. In test-server/server.properties:
       enable-rcon=true
       rcon.port=25575
       rcon.password=alicetest
  2. Start the server manually (run.bat / run.sh). Wait until "Done!" appears.
  3. Run tests: pytest test-server/tests/

The fixtures do NOT start/stop the server — the 205-mod modpack takes too long
to boot for per-test startup. Tests share a running server and are expected to
clean up their world state (tp, kill, setblock) themselves.
"""
from __future__ import annotations

import os
import re
import time
from pathlib import Path

import pytest

from .log_tail import LogTail
from .rcon_client import RconClient, rcon_session


RCON_HOST = os.environ.get("ALICE_RCON_HOST", "127.0.0.1")
RCON_PORT = int(os.environ.get("ALICE_RCON_PORT", "25575"))
RCON_PASSWORD = os.environ.get("ALICE_RCON_PASSWORD", "alicetest")

TEST_SERVER_ROOT = Path(__file__).resolve().parents[1]
LATEST_LOG = TEST_SERVER_ROOT / "logs" / "latest.log"


@pytest.fixture(scope="session")
def rcon():
    """Session-scoped RCON client. Fails fast if server isn't reachable.

    Timeout is set to 30s (not 10s) because large fill/setblock commands on a
    heavily-modded server can take longer than the default socket timeout.
    """
    client = RconClient(RCON_HOST, RCON_PORT, RCON_PASSWORD, timeout=30.0)
    client.connect()
    try:
        resp = client.command("seed")
        assert resp, "RCON connected but 'seed' returned empty — server broken?"
        yield client
    finally:
        client.close()


@pytest.fixture
def log_tail() -> LogTail:
    """Per-test log tail — resets baseline at test start so asserts only see NEW lines.

    Only available when targeting the LOCAL test-server. Tests targeting a remote
    server (Larry) should skip this fixture and assert via RCON only.
    """
    if not LATEST_LOG.exists():
        pytest.skip(f"latest.log not found at {LATEST_LOG} — remote server target or local server not running")
    return LogTail(LATEST_LOG)


@pytest.fixture
def clean_world(rcon: RconClient):
    """Reset world to a quiet, known state: peaceful, day, no hostile entities."""
    rcon.command("difficulty peaceful")
    rcon.command("time set day")
    rcon.command("weather clear")
    rcon.command("kill @e[type=!player,type=!item]")
    time.sleep(0.5)
    yield
    # teardown: nothing — each test should clean its own structures


@pytest.fixture
def peaceful(rcon: RconClient):
    """Ensure peaceful difficulty for the duration of the test."""
    rcon.command("difficulty peaceful")
    yield
    rcon.command("difficulty peaceful")


@pytest.fixture
def easy(rcon: RconClient):
    """Switch to easy difficulty; restore peaceful after."""
    rcon.command("difficulty easy")
    yield
    rcon.command("difficulty peaceful")


# ---------------------------------------------------------------------------
# Combat arena fixture — coordinates at (-5800, 69, -5800)
# ---------------------------------------------------------------------------

COMBAT_X, COMBAT_Y, COMBAT_Z = -5800, 69, -5800
COMBAT_W = 15


def _alice_tp(rcon: RconClient, x: float, y: float, z: float) -> None:
    rcon.command(f"alicecmd tp {x} {y} {z}")


def _alice_stay(rcon: RconClient) -> None:
    rcon.command("alicecmd stay")


def _alice_pos(rcon: RconClient):
    resp = rcon.command("alicecmd pos")
    m = re.search(r"pos\s+(-?[\d.]+)\s+(-?[\d.]+)\s+(-?[\d.]+)", resp)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), float(m.group(3))


@pytest.fixture
def combat_arena(rcon: RconClient):
    """Flat arena at (-5800, 69, -5800). Clears mobs, builds floor, parks Alice."""
    rcon.command("difficulty peaceful")
    rcon.command("kill @e[type=zombie]")
    rcon.command("kill @e[type=skeleton]")
    x1, x2 = COMBAT_X - COMBAT_W, COMBAT_X + COMBAT_W
    z1, z2 = COMBAT_Z - COMBAT_W, COMBAT_Z + COMBAT_W
    rcon.command(f"forceload add {x1} {z1} {x2} {z2}")
    rcon.command(f"fill {x1} {COMBAT_Y - 2} {z1} {x2} {COMBAT_Y - 1} {z2} minecraft:stone")
    rcon.command(f"fill {x1} {COMBAT_Y} {z1} {x2} {COMBAT_Y + 4} {z2} minecraft:air")
    _alice_stay(rcon)
    _alice_tp(rcon, COMBAT_X + 0.5, COMBAT_Y, COMBAT_Z + 0.5)
    time.sleep(0.8)
    yield
    _alice_stay(rcon)
    rcon.command("kill @e[type=zombie]")
    rcon.command("kill @e[type=skeleton]")
    rcon.command(f"forceload remove {x1} {z1} {x2} {z2}")
    rcon.command("difficulty peaceful")


def wait_alice_ready(rcon: RconClient, timeout: float = 10.0) -> bool:
    """Block until Alice responds to alicecmd status with an HP/pos line.

    FakePlayer is NOT tracked in /list (no connection object), so we probe
    alicecmd status instead, which returns '[Alice] hp=...' when attached.
    """
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            resp = rcon.command("alicecmd status")
            if "[Alice]" in resp and "hp=" in resp:
                return True
        except Exception:
            pass
        time.sleep(0.3)
    return False
