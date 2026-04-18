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
    """Session-scoped RCON client. Fails fast if server isn't reachable."""
    with rcon_session(RCON_HOST, RCON_PORT, RCON_PASSWORD) as client:
        # Sanity: server answers a cheap query
        resp = client.command("seed")
        assert resp, "RCON connected but 'seed' returned empty — server broken?"
        yield client


@pytest.fixture
def log_tail() -> LogTail:
    """Per-test log tail — resets baseline at test start so asserts only see NEW lines."""
    assert LATEST_LOG.exists(), f"latest.log not found at {LATEST_LOG} — is server running?"
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


def wait_alice_ready(rcon: RconClient, timeout: float = 10.0) -> bool:
    """Block until Alice responds to a 'status' probe via a chat (no-op for now)."""
    # Simple heuristic: Alice is tracked in player list
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        resp = rcon.command("list")
        if "Alice" in resp:
            return True
        time.sleep(0.3)
    return False
