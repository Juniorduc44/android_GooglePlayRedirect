"""Self-custody EVM wallet for Robinhood Chain (local keystore first; passkey later)."""

from .keystore import WalletKeystore, WalletRecord
from .rpc import eth_get_balance_eth, eth_chain_id

__all__ = [
    "WalletKeystore",
    "WalletRecord",
    "eth_get_balance_eth",
    "eth_chain_id",
]
