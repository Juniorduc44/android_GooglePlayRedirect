"""Minimal JSON-RPC helpers for Robinhood Chain (and generic EVM)."""

from __future__ import annotations

import json
from urllib import error, request

DEFAULT_RH_RPC = "https://rpc.mainnet.chain.robinhood.com"
EXPECTED_CHAIN_ID = 4663


class RpcError(RuntimeError):
    pass


def _rpc_call(
    method: str,
    params: list | None = None,
    *,
    rpc_url: str = DEFAULT_RH_RPC,
    timeout: float = 12.0,
) -> object:
    payload = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": method,
        "params": params or [],
    }
    data = json.dumps(payload).encode("utf-8")
    req = request.Request(
        rpc_url,
        data=data,
        headers={
            "Content-Type": "application/json",
            "User-Agent": "php-usd-converter-wallet/1.7",
        },
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            out = json.loads(body) if body else {}
    except error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")[:200]
        raise RpcError(f"HTTP {e.code}: {detail or e.reason}") from e
    except error.URLError as e:
        raise RpcError(f"Network error: {e.reason}") from e
    except TimeoutError as e:
        raise RpcError("RPC timed out") from e
    except json.JSONDecodeError as e:
        raise RpcError(f"Invalid JSON from RPC: {e}") from e
    except Exception as e:
        raise RpcError(f"{type(e).__name__}: {e}") from e

    if not isinstance(out, dict):
        raise RpcError("Unexpected RPC response")
    if out.get("error"):
        err = out["error"]
        raise RpcError(str(err))
    return out.get("result")


def eth_chain_id(rpc_url: str = DEFAULT_RH_RPC, timeout: float = 12.0) -> int:
    result = _rpc_call("eth_chainId", rpc_url=rpc_url, timeout=timeout)
    if not isinstance(result, str):
        raise RpcError("eth_chainId missing")
    return int(result, 16)


def eth_get_balance_eth(
    address: str,
    *,
    rpc_url: str = DEFAULT_RH_RPC,
    timeout: float = 12.0,
) -> float:
    """Return native ETH balance as float (wei / 1e18)."""
    addr = (address or "").strip()
    if not addr.startswith("0x") or len(addr) != 42:
        raise RpcError("Invalid address")
    result = _rpc_call(
        "eth_getBalance",
        [addr, "latest"],
        rpc_url=rpc_url,
        timeout=timeout,
    )
    if not isinstance(result, str):
        raise RpcError("eth_getBalance missing")
    wei = int(result, 16)
    return wei / 1e18
