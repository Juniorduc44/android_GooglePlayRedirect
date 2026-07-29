"""EVM network registry — Robinhood Chain is the product default."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class Network:
    id: str
    name: str
    chain_id: int
    dexscreener_slug: str
    native_symbol: str
    rpc_public: str
    explorer: str
    notes: str = ""

    def as_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "chain_id": self.chain_id,
            "dexscreener_slug": self.dexscreener_slug,
            "native_symbol": self.native_symbol,
            "rpc_public": self.rpc_public,
            "explorer": self.explorer,
            "notes": self.notes,
        }


NETWORKS: dict[str, Network] = {
    "robinhood": Network(
        id="robinhood",
        name="Robinhood Chain",
        chain_id=4663,
        dexscreener_slug="robinhood",
        native_symbol="ETH",
        rpc_public="https://rpc.mainnet.chain.robinhood.com",
        explorer="https://robinhoodchain.blockscout.com",
        notes=(
            "Permissionless Arbitrum L2 for RWAs / Stock Tokens. "
            "Public RPC is rate-limited; Alchemy recommended for production eth_call. "
            "Prices via DexScreener."
        ),
    ),
    # Placeholders for future multi-chain tracker UI (not default)
    "ethereum": Network(
        id="ethereum",
        name="Ethereum",
        chain_id=1,
        dexscreener_slug="ethereum",
        native_symbol="ETH",
        rpc_public="https://ethereum.publicnode.com",
        explorer="https://etherscan.io",
        notes="Optional future chain — not default.",
    ),
    "base": Network(
        id="base",
        name="Base",
        chain_id=8453,
        dexscreener_slug="base",
        native_symbol="ETH",
        rpc_public="https://mainnet.base.org",
        explorer="https://basescan.org",
        notes="Optional future chain — not default.",
    ),
}

DEFAULT_NETWORK_ID = "robinhood"


def get_network(network_id: str | None = None) -> Network:
    nid = (network_id or DEFAULT_NETWORK_ID).lower().strip()
    if nid not in NETWORKS:
        raise KeyError(f"Unknown network id: {nid}")
    return NETWORKS[nid]


def list_network_labels() -> list[tuple[str, str]]:
    """Return (id, display label) with Robinhood first."""
    order = ["robinhood"] + [k for k in NETWORKS if k != "robinhood"]
    out: list[tuple[str, str]] = []
    for k in order:
        n = NETWORKS[k]
        out.append((k, f"{n.name} ({n.chain_id})"))
    return out
