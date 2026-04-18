"""Log tailing helper — tracks a log file from a stable baseline and lets tests
assert that a regex appeared (or did not appear) within a time budget.
"""
from __future__ import annotations

import os
import re
import time
from pathlib import Path
from typing import Pattern


class LogTail:
    """Snapshot a log file's current size, then poll for new content after that."""

    def __init__(self, path: Path):
        self.path = path
        self._baseline = self._current_size()

    def _current_size(self) -> int:
        try:
            return os.path.getsize(self.path)
        except FileNotFoundError:
            return 0

    def reset_baseline(self) -> None:
        self._baseline = self._current_size()

    def read_new(self) -> str:
        size = self._current_size()
        if size <= self._baseline:
            return ""
        with open(self.path, "rb") as f:
            f.seek(self._baseline)
            data = f.read(size - self._baseline)
        return data.decode("utf-8", errors="replace")

    def wait_for(self, pattern: str | Pattern[str], timeout: float = 15.0,
                 poll: float = 0.2) -> re.Match[str] | None:
        """Return the first Match for `pattern` in new log content, or None on timeout."""
        rx = re.compile(pattern) if isinstance(pattern, str) else pattern
        deadline = time.monotonic() + timeout
        accumulated = ""
        while time.monotonic() < deadline:
            accumulated += self.read_new()
            m = rx.search(accumulated)
            if m:
                return m
            time.sleep(poll)
        return None

    def ensure_absent(self, pattern: str | Pattern[str], during: float = 5.0,
                      poll: float = 0.2) -> bool:
        """Return True if the pattern never appeared during the window."""
        rx = re.compile(pattern) if isinstance(pattern, str) else pattern
        deadline = time.monotonic() + during
        accumulated = ""
        while time.monotonic() < deadline:
            accumulated += self.read_new()
            if rx.search(accumulated):
                return False
            time.sleep(poll)
        return True
