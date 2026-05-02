"""Performance tests: heap stability, TPS, tick P95 latency. Covers DOD D11.

Uses `alicecmd perfstats` RCON command for inline metrics.
Slow tests are marked @pytest.mark.slow — run explicitly for release validation.

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

HEAP_MAX_PCT = 80.0       # heap must stay below 80%
TPS_MIN = 17.0            # minimum acceptable TPS
TICK_P95_MAX_MS = 10.0    # Baritone tick P95 < 10ms
SAMPLE_INTERVAL = 30.0    # seconds between heap samples


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def get_perf_stats(rcon: RconClient) -> dict:
    """Call `alicecmd perfstats` and parse the response."""
    resp = rcon.command("alicecmd perfstats")
    result = {}

    # heap=1753/4000 MB (43.8%)
    m = re.search(r"heap=(\d+)/(\d+)\s*MB\s*\(([0-9.]+)%\)", resp)
    if m:
        result["heap_used_mb"] = int(m.group(1))
        result["heap_max_mb"] = int(m.group(2))
        result["heap_pct"] = float(m.group(3))

    # threads=34
    m = re.search(r"threads=(\d+)", resp)
    if m:
        result["threads"] = int(m.group(1))

    # tickP95=1.23ms
    m = re.search(r"tickP95=([\d.]+)ms", resp)
    if m:
        result["tick_p95_ms"] = float(m.group(1))

    result["raw"] = resp
    return result


def get_tps(rcon: RconClient) -> float | None:
    """Read server TPS via /forge tps (returns Overall TPS)."""
    resp = rcon.command("forge tps")
    m = re.search(r"Overall\s*[Tt]ick\s*[Rr]ate[:\s]*([\d.]+)", resp)
    if m:
        return float(m.group(1))
    # Fallback: look for just a number near "Overall"
    m = re.search(r"Overall.*?([\d.]+)\s*TPS", resp, re.IGNORECASE)
    if m:
        return float(m.group(1))
    return None


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestHeap:

    def test_heap_within_bounds_single_sample(self, rcon: RconClient, peaceful):
        """Heap usage is below 80% immediately after startup."""
        stats = get_perf_stats(rcon)
        assert "heap_pct" in stats, (
            f"Could not parse heap percentage from perfstats response: {stats.get('raw')}\n"
            "Ensure `alicecmd perfstats` is registered in AliceCommands."
        )
        assert stats["heap_pct"] < HEAP_MAX_PCT, (
            f"Heap at {stats['heap_pct']:.1f}% exceeds {HEAP_MAX_PCT}% threshold. "
            f"raw={stats['raw']}"
        )

    @pytest.mark.slow
    def test_heap_not_monotonically_increasing(self, rcon: RconClient, peaceful):
        """Heap does not grow monotonically over 5 samples × 30s = ~2.5 minutes (leak indicator)."""
        samples = []
        for i in range(5):
            stats = get_perf_stats(rcon)
            if "heap_pct" in stats:
                samples.append(stats["heap_pct"])
            if i < 4:
                time.sleep(SAMPLE_INTERVAL)

        assert len(samples) >= 3, f"Not enough heap samples collected: {samples}"

        # Check that it's not strictly monotonically increasing
        # (i.e., at least one GC cycle brought it down at some point)
        is_monotonic = all(samples[i] <= samples[i + 1] for i in range(len(samples) - 1))
        assert not is_monotonic or samples[-1] - samples[0] < 5.0, (
            f"Heap is monotonically increasing over {len(samples)} samples — possible leak.\n"
            f"Samples: {[f'{s:.1f}%' for s in samples]}"
        )

        # Final sample must still be under 80%
        assert samples[-1] < HEAP_MAX_PCT, (
            f"Heap reached {samples[-1]:.1f}% after {len(samples)} samples. "
            f"All samples: {[f'{s:.1f}%' for s in samples]}"
        )


class TestTickPerformance:

    def test_tick_p95_within_bound(self, rcon: RconClient, peaceful):
        """Baritone tick P95 latency < 10ms (server thread impact acceptable)."""
        # Wait for at least one metrics window to populate (200 ticks ≈ 10s)
        time.sleep(12)
        stats = get_perf_stats(rcon)

        if "tick_p95_ms" not in stats or stats["tick_p95_ms"] == 0.0:
            pytest.skip(
                "tickP95 not yet computed (server may have just started — "
                "need 200+ ticks). Re-run after 15s."
            )

        assert stats["tick_p95_ms"] < TICK_P95_MAX_MS, (
            f"Tick P95 {stats['tick_p95_ms']:.2f}ms exceeds {TICK_P95_MAX_MS}ms threshold. "
            "Baritone is spending too long on the server thread. "
            f"raw={stats['raw']}"
        )

    def test_tps_above_minimum(self, rcon: RconClient, peaceful):
        """Server TPS is above 17.0 while Alice is active."""
        tps = get_tps(rcon)
        if tps is None:
            pytest.skip(
                "Could not read TPS from `/forge tps`. "
                "Command may not be available or output format changed."
            )
        assert tps >= TPS_MIN, (
            f"Server TPS {tps:.1f} is below minimum {TPS_MIN}. "
            "Server is under load — check for pathfinding loops or large world structures."
        )


class TestCrashStability:

    @pytest.mark.slow
    def test_no_crash_30_minutes(self, rcon: RconClient, log_tail, peaceful):
        """D11 — Zero Java crash / FATAL error in 30 continuous minutes."""
        crash_pattern = re.compile(
            r"hs_err_pid|Exception in thread ['\"]main['\"]|"
            r"FATAL\s+\[|Reached end of stream|"
            r"java\.lang\.OutOfMemoryError"
        )
        duration = 30 * 60  # 1800 seconds
        start = time.monotonic()
        end = start + duration
        poll_interval = 10.0

        while time.monotonic() < end:
            elapsed = time.monotonic() - start
            m = log_tail.wait_for(crash_pattern.pattern, timeout=poll_interval)
            if m:
                pytest.fail(
                    f"Crash/fatal error detected at elapsed={elapsed:.0f}s: {m.group(0)}"
                )
            # Also verify Alice is still responding
            try:
                resp = rcon.command("alicecmd status")
                assert resp, f"Alice stopped responding to RCON at elapsed={elapsed:.0f}s"
            except Exception as e:
                pytest.fail(f"RCON connection lost at elapsed={elapsed:.0f}s: {e}")
