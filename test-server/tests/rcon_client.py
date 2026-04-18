"""Minimal RCON client for Minecraft server — stdlib only, no third-party deps.

Protocol reference: https://wiki.vg/RCON
Packet: length(4) + id(4) + type(4) + body(nul-terminated) + pad(1)
  types: 3=login, 2=command, 0=response
"""
from __future__ import annotations

import socket
import struct
from contextlib import contextmanager


class RconError(Exception):
    pass


class RconClient:
    PACKET_TYPE_LOGIN = 3
    PACKET_TYPE_COMMAND = 2
    PACKET_TYPE_RESPONSE = 0

    def __init__(self, host: str, port: int, password: str, timeout: float = 10.0):
        self.host = host
        self.port = port
        self.password = password
        self.timeout = timeout
        self._sock: socket.socket | None = None
        self._req_id = 0

    def connect(self) -> None:
        if self._sock is not None:
            return
        s = socket.create_connection((self.host, self.port), timeout=self.timeout)
        self._sock = s
        self._send(self.PACKET_TYPE_LOGIN, self.password)
        resp_id, _, _ = self._recv()
        if resp_id == -1:
            self.close()
            raise RconError("RCON auth failed (bad password?)")

    def close(self) -> None:
        if self._sock is not None:
            try:
                self._sock.close()
            finally:
                self._sock = None

    def command(self, cmd: str) -> str:
        if self._sock is None:
            raise RconError("Not connected — call connect() first")
        self._send(self.PACKET_TYPE_COMMAND, cmd)
        _, _, body = self._recv()
        return body

    def _send(self, pkt_type: int, payload: str) -> None:
        self._req_id += 1
        body = payload.encode("utf-8") + b"\x00\x00"
        packet = struct.pack("<ii", self._req_id, pkt_type) + body
        length_prefix = struct.pack("<i", len(packet))
        assert self._sock is not None
        self._sock.sendall(length_prefix + packet)

    def _recv(self) -> tuple[int, int, str]:
        assert self._sock is not None
        length_bytes = self._recv_exactly(4)
        (length,) = struct.unpack("<i", length_bytes)
        payload = self._recv_exactly(length)
        req_id, pkt_type = struct.unpack("<ii", payload[:8])
        body = payload[8:-2].decode("utf-8", errors="replace")
        return req_id, pkt_type, body

    def _recv_exactly(self, n: int) -> bytes:
        assert self._sock is not None
        buf = bytearray()
        while len(buf) < n:
            chunk = self._sock.recv(n - len(buf))
            if not chunk:
                raise RconError(f"Connection closed while reading (got {len(buf)}/{n})")
            buf.extend(chunk)
        return bytes(buf)


@contextmanager
def rcon_session(host: str, port: int, password: str):
    client = RconClient(host, port, password)
    client.connect()
    try:
        yield client
    finally:
        client.close()
