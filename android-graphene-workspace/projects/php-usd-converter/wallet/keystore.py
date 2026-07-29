"""Local encrypted wallet keystore (password-protected private key).

This is a self-custody EOA on Robinhood Chain — foundation for a later
WebAuthn/passkey + ERC-4337 smart account. Not Robinhood brokerage login.
"""

from __future__ import annotations

import base64
import hashlib
import json
import os
import secrets
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass
class WalletRecord:
    address: str
    chain_id: int = 4663
    label: str = "Robinhood Chain wallet"
    # present only after unlock / create (never write plaintext to disk)
    private_key: str | None = None


class WalletKeystore:
    """File-backed keystore: wallet_keystore.json (gitignored)."""

    def __init__(self, path: Path | None = None) -> None:
        root = Path(__file__).resolve().parent.parent
        self.path = path or (root / "wallet_keystore.json")
        self._unlocked_key: str | None = None
        self._address: str | None = None
        self._meta: dict[str, Any] = {}
        self.load_meta()

    def load_meta(self) -> None:
        self._meta = {}
        self._address = None
        if not self.path.is_file():
            return
        try:
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            if isinstance(raw, dict):
                self._meta = raw
                self._address = str(raw.get("address") or "") or None
        except Exception:
            self._meta = {}

    @property
    def has_wallet(self) -> bool:
        return bool(self._meta.get("address") and self._meta.get("ciphertext"))

    @property
    def address(self) -> str | None:
        return self._address

    @property
    def is_unlocked(self) -> bool:
        return bool(self._unlocked_key and self._address)

    def create(self, password: str, *, label: str = "Robinhood Chain wallet") -> WalletRecord:
        if not password or len(password) < 6:
            raise ValueError("Password must be at least 6 characters")
        try:
            from eth_account import Account
        except ImportError as e:
            raise RuntimeError(
                "eth-account not installed. Run: pip install eth-account"
            ) from e

        acct = Account.create()
        pk = acct.key.hex()
        if not pk.startswith("0x"):
            pk = "0x" + pk
        address = acct.address
        blob = self._encrypt(pk, password)
        data = {
            "version": 1,
            "address": address,
            "chain_id": 4663,
            "label": label,
            "kdf": "scrypt-like-sha256",
            "salt": blob["salt"],
            "ciphertext": blob["ciphertext"],
            "note": "Self-custody EOA. Not a passkey smart account yet. Back up password.",
        }
        self.path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        try:
            os.chmod(self.path, 0o600)
        except OSError:
            pass
        self._meta = data
        self._address = address
        self._unlocked_key = pk
        return WalletRecord(address=address, chain_id=4663, label=label, private_key=pk)

    def import_private_key(self, private_key: str, password: str) -> WalletRecord:
        if not password or len(password) < 6:
            raise ValueError("Password must be at least 6 characters")
        pk = (private_key or "").strip()
        if not pk.startswith("0x"):
            pk = "0x" + pk
        try:
            from eth_account import Account
        except ImportError as e:
            raise RuntimeError("pip install eth-account") from e
        try:
            acct = Account.from_key(pk)
        except Exception as e:
            raise ValueError(f"Invalid private key: {e}") from e
        blob = self._encrypt(pk, password)
        data = {
            "version": 1,
            "address": acct.address,
            "chain_id": 4663,
            "label": "Imported wallet",
            "kdf": "scrypt-like-sha256",
            "salt": blob["salt"],
            "ciphertext": blob["ciphertext"],
            "note": "Imported private key (encrypted at rest).",
        }
        self.path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        try:
            os.chmod(self.path, 0o600)
        except OSError:
            pass
        self._meta = data
        self._address = acct.address
        self._unlocked_key = pk
        return WalletRecord(
            address=acct.address, chain_id=4663, label="Imported", private_key=pk
        )

    def unlock(self, password: str) -> WalletRecord:
        if not self.has_wallet:
            raise RuntimeError("No wallet on disk — create one first")
        try:
            pk = self._decrypt(
                self._meta["ciphertext"],
                self._meta["salt"],
                password,
            )
        except Exception as e:
            raise ValueError("Wrong password or corrupt keystore") from e
        # verify key matches address
        try:
            from eth_account import Account

            acct = Account.from_key(pk)
            if acct.address.lower() != str(self._meta.get("address", "")).lower():
                raise ValueError("Key/address mismatch")
        except ValueError:
            raise
        except Exception as e:
            raise ValueError(f"Unlock failed: {e}") from e
        self._unlocked_key = pk
        self._address = acct.address
        return WalletRecord(
            address=acct.address,
            chain_id=int(self._meta.get("chain_id") or 4663),
            label=str(self._meta.get("label") or "Wallet"),
            private_key=pk,
        )

    def lock(self) -> None:
        self._unlocked_key = None

    def export_private_key(self) -> str:
        if not self._unlocked_key:
            raise RuntimeError("Wallet is locked — unlock first")
        return self._unlocked_key

    def delete(self) -> None:
        self.lock()
        self._meta = {}
        self._address = None
        try:
            if self.path.is_file():
                self.path.unlink()
        except OSError:
            pass

    # --- crypto helpers (stdlib only: PBKDF2-HMAC-SHA256 + XOR stream) ---
    # Good enough for local sideload tool; not a hardware wallet. Passkey AA later.

    @staticmethod
    def _derive(password: str, salt_b64: str, rounds: int = 200_000) -> bytes:
        salt = base64.b64decode(salt_b64.encode("ascii"))
        return hashlib.pbkdf2_hmac(
            "sha256", password.encode("utf-8"), salt, rounds, dklen=32
        )

    def _encrypt(self, plaintext: str, password: str) -> dict[str, str]:
        salt = secrets.token_bytes(16)
        salt_b64 = base64.b64encode(salt).decode("ascii")
        key = self._derive(password, salt_b64)
        data = plaintext.encode("utf-8")
        # stream XOR with SHA256 expansion
        out = bytearray()
        block = b""
        counter = 0
        for i, b in enumerate(data):
            if i % 32 == 0:
                block = hashlib.sha256(key + counter.to_bytes(4, "big")).digest()
                counter += 1
            out.append(b ^ block[i % 32])
        return {
            "salt": salt_b64,
            "ciphertext": base64.b64encode(bytes(out)).decode("ascii"),
        }

    def _decrypt(self, ciphertext_b64: str, salt_b64: str, password: str) -> str:
        key = self._derive(password, salt_b64)
        data = base64.b64decode(ciphertext_b64.encode("ascii"))
        out = bytearray()
        block = b""
        counter = 0
        for i, b in enumerate(data):
            if i % 32 == 0:
                block = hashlib.sha256(key + counter.to_bytes(4, "big")).digest()
                counter += 1
            out.append(b ^ block[i % 32])
        text = bytes(out).decode("utf-8")
        if not text.startswith("0x") or len(text) < 64:
            raise ValueError("bad decrypt")
        return text
