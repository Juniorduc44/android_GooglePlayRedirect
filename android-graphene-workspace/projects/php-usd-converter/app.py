#!/usr/bin/env python3
"""PHP ↔ USD converter + Travel cost-per-distance (CustomTkinter)."""

from __future__ import annotations

import json
import threading
from pathlib import Path

import customtkinter as ctk
import requests

from translator import LANGUAGES, SecretsStore, get_backend, list_backends
from translator.backends import BACKEND_LABELS

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

KM_PER_MILE = 1.609344
# International avoirdupois pound
KG_PER_LB = 0.45359237
G_PER_KG = 1000.0
WEIGHT_UNITS = ("lb", "kg", "g")  # cycle order
WEIGHT_LABELS = {
    "lb": "Weight (lb):",
    "kg": "Weight (kg):",
    "g": "Weight (g):",
}
WEIGHT_NEXT = {"lb": "kg", "kg": "g", "g": "lb"}
WEIGHT_SWITCH_TEXT = {"lb": "⇄ kg", "kg": "⇄ g", "g": "⇄ lb"}
WEIGHT_PLACEHOLDER = {
    "lb": "e.g., 150",
    "kg": "e.g., 70",
    "g": "e.g., 500",
}
SETTINGS_PATH = Path(__file__).resolve().parent / "user_settings.json"

# Result text sizes (main result, secondary FX line)
RESULT_SIZES = {
    "Small": (18, 12),
    "Medium": (24, 14),
    "Large": (32, 16),
    "Extra large": (40, 18),
}
DEFAULT_RESULT_SIZE = "Large"


def load_settings() -> dict:
    try:
        if SETTINGS_PATH.is_file():
            data = json.loads(SETTINGS_PATH.read_text(encoding="utf-8"))
            if isinstance(data, dict):
                return data
    except Exception:
        pass
    return {}


def save_settings(data: dict) -> None:
    try:
        SETTINGS_PATH.write_text(json.dumps(data, indent=2), encoding="utf-8")
    except Exception:
        pass


class CurrencyConverterApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("Toolkit — Convert · Travel · Weight · Translator")
        self.geometry("520x720")
        self.minsize(440, 600)
        self.resizable(True, True)

        self._settings = load_settings()
        size_key = self._settings.get("result_text_size", DEFAULT_RESULT_SIZE)
        if size_key not in RESULT_SIZES:
            size_key = DEFAULT_RESULT_SIZE
        self.result_text_size = size_key
        self.secrets = SecretsStore()
        self._translator_busy = False

        self.exchange_rate = self.get_live_rate()
        self.php_to_usd = True  # Convert tab primary currency
        self.travel_php = True  # Travel tab primary currency (independent)
        self.use_km = True
        self.weight_unit = "lb"  # input unit: lb | kg | g

        self._build_ui()
        self._apply_direction_labels()
        self._apply_distance_unit_labels()
        self._apply_travel_currency_labels()
        self._apply_weight_unit_labels()
        self._apply_result_fonts()

    # ------------------------------------------------------------------ rate
    def get_live_rate(self) -> float:
        try:
            url = "https://api.exchangerate-api.com/v4/latest/PHP"
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            return float(response.json()["rates"]["USD"])
        except Exception:
            return 0.0175

    def _php_to_usd(self, php: float) -> float:
        return php * self.exchange_rate

    def _usd_to_php(self, usd: float) -> float:
        return usd / self.exchange_rate if self.exchange_rate else 0.0

    def _format_money(self, amount: float, as_usd: bool) -> str:
        if as_usd:
            return f"${amount:,.2f} USD"
        return f"₱{amount:,.2f} PHP"

    # -------------------------------------------------------------- fonts
    def _apply_result_fonts(self):
        main_sz, fx_sz = RESULT_SIZES.get(
            self.result_text_size, RESULT_SIZES[DEFAULT_RESULT_SIZE]
        )
        self.result_label.configure(font=ctk.CTkFont(size=main_sz, weight="bold"))
        self.travel_result_label.configure(
            font=ctk.CTkFont(size=main_sz, weight="bold")
        )
        self.travel_result_fx_label.configure(font=ctk.CTkFont(size=fx_sz))
        if hasattr(self, "weight_result_label"):
            self.weight_result_label.configure(
                font=ctk.CTkFont(size=main_sz, weight="bold")
            )
            self.weight_result_secondary.configure(font=ctk.CTkFont(size=fx_sz))
        # Keep convert secondary line readable
        if hasattr(self, "rate_info_label"):
            self.rate_info_label.configure(
                font=ctk.CTkFont(size=max(11, fx_sz - 2))
            )

    def set_result_text_size(self, size_key: str):
        if size_key not in RESULT_SIZES:
            return
        self.result_text_size = size_key
        self._settings["result_text_size"] = size_key
        save_settings(self._settings)
        self._apply_result_fonts()
        self.settings_status.configure(
            text=f"Result text size: {size_key} (saved)",
            text_color="#10B981",
        )

    # -------------------------------------------------------------- convert
    def swap_direction(self):
        self.php_to_usd = not self.php_to_usd
        self.exchange_rate = self.get_live_rate()
        self._apply_direction_labels()
        raw = self.amount_entry.get().strip()
        if raw:
            self.convert_currency()
        else:
            self._reset_convert_result()
            self._show_rate_info()

    def swap_travel_currency(self):
        """Toggle Travel tab cost currency PHP ↔ USD (independent of Convert)."""
        self.travel_php = not self.travel_php
        self.exchange_rate = self.get_live_rate()
        self._apply_travel_currency_labels()
        self._show_rate_info()
        self.calculate_travel()

    def _reset_convert_result(self):
        if self.php_to_usd:
            self.result_label.configure(text="$0.00 USD", text_color="#3B82F6")
        else:
            self.result_label.configure(text="₱0.00 PHP", text_color="#3B82F6")

    def _apply_direction_labels(self):
        if self.php_to_usd:
            self.subtitle_label.configure(
                text="Philippine Peso (PHP)  ➔  US Dollar (USD)"
            )
            self.input_label.configure(text="Amount in Pesos (₱):")
            self.amount_entry.configure(placeholder_text="e.g., 1000")
            self.convert_button.configure(text="Convert to USD")
            self.swap_button.configure(text="⇄ USD")
        else:
            self.subtitle_label.configure(
                text="US Dollar (USD)  ➔  Philippine Peso (PHP)"
            )
            self.input_label.configure(text="Amount in Dollars ($):")
            self.amount_entry.configure(placeholder_text="e.g., 20")
            self.convert_button.configure(text="Convert to PHP")
            self.swap_button.configure(text="⇄ PHP")
        self._show_rate_info()

    def _apply_travel_currency_labels(self):
        if self.travel_php:
            self.travel_cost_label.configure(text="Trip cost (₱ PHP):")
            self.travel_cost_entry.configure(placeholder_text="e.g., 2500")
            self.travel_currency_hint.configure(
                text="Enter cost in pesos; switch for dollars"
            )
            self.travel_currency_button.configure(text="⇄ USD")
        else:
            self.travel_cost_label.configure(text="Trip cost ($ USD):")
            self.travel_cost_entry.configure(placeholder_text="e.g., 45")
            self.travel_currency_hint.configure(
                text="Enter cost in dollars; switch for pesos"
            )
            self.travel_currency_button.configure(text="⇄ PHP")

    def _show_rate_info(self, color: str = "#9CA3AF"):
        if self.php_to_usd:
            text = f"Rate: 1 PHP = ${self.exchange_rate:.4f} USD"
        else:
            inv = (1.0 / self.exchange_rate) if self.exchange_rate else 0.0
            text = f"Rate: 1 USD = ₱{inv:,.2f} PHP"
        self.rate_info_label.configure(text=text, text_color=color)
        self.travel_rate_label.configure(text=text, text_color=color)

    def convert_currency(self):
        raw_input = self.amount_entry.get().strip()
        if not raw_input:
            self._reset_convert_result()
            self.rate_info_label.configure(
                text="Please enter an amount.", text_color="#EF4444"
            )
            return
        try:
            amount = float(raw_input)
            if amount < 0:
                raise ValueError("Negative number")
            if self.php_to_usd:
                out = self._php_to_usd(amount)
                self.result_label.configure(
                    text=self._format_money(out, as_usd=True), text_color="#10B981"
                )
            else:
                out = self._usd_to_php(amount)
                self.result_label.configure(
                    text=self._format_money(out, as_usd=False), text_color="#10B981"
                )
            self._show_rate_info()
        except ValueError:
            self.result_label.configure(text="Invalid Input", text_color="#EF4444")
            self.rate_info_label.configure(
                text="Please enter a valid positive number.",
                text_color="#EF4444",
            )

    # --------------------------------------------------------------- travel
    def swap_distance_unit(self):
        raw = self.distance_entry.get().strip()
        old_use_km = self.use_km
        self.use_km = not self.use_km
        if raw:
            try:
                val = float(raw)
                if val >= 0:
                    if old_use_km and not self.use_km:
                        val = val / KM_PER_MILE
                    elif not old_use_km and self.use_km:
                        val = val * KM_PER_MILE
                    self.distance_entry.delete(0, "end")
                    self.distance_entry.insert(0, f"{val:.2f}")
            except ValueError:
                pass
        self._apply_distance_unit_labels()
        self.calculate_travel()

    def _apply_distance_unit_labels(self):
        if self.use_km:
            self.distance_unit_label.configure(text="Distance (km):")
            self.unit_switch_button.configure(text="⇄ mi")
            self.travel_calc_button.configure(text="Calculate cost per km")
        else:
            self.distance_unit_label.configure(text="Distance (miles):")
            self.unit_switch_button.configure(text="⇄ km")
            self.travel_calc_button.configure(text="Calculate cost per mile")
        self._update_distance_equiv_hint()

    def _update_distance_equiv_hint(self):
        raw = self.distance_entry.get().strip()
        if not raw:
            self.distance_equiv_label.configure(text="")
            return
        try:
            val = float(raw)
            if val < 0:
                raise ValueError
            if self.use_km:
                miles = val / KM_PER_MILE
                self.distance_equiv_label.configure(text=f"(≈ {miles:,.2f} mi)")
            else:
                km = val * KM_PER_MILE
                self.distance_equiv_label.configure(text=f"(≈ {km:,.2f} km)")
        except ValueError:
            self.distance_equiv_label.configure(text="")

    def calculate_travel(self, *_args):
        self._update_distance_equiv_hint()
        dist_raw = self.distance_entry.get().strip()
        cost_raw = self.travel_cost_entry.get().strip()
        unit = "km" if self.use_km else "mi"

        if not dist_raw or not cost_raw:
            self.travel_result_label.configure(
                text=f"— / {unit}", text_color="#3B82F6"
            )
            self.travel_result_fx_label.configure(text="")
            self.travel_cost_fx_label.configure(text="")
            self.travel_status_label.configure(
                text="Enter distance and trip cost.", text_color="#9CA3AF"
            )
            return

        try:
            distance = float(dist_raw)
            cost = float(cost_raw)
            if distance <= 0:
                raise ValueError("Distance must be > 0")
            if cost < 0:
                raise ValueError("Cost cannot be negative")

            per = cost / distance
            if self.travel_php:
                self.travel_cost_fx_label.configure(
                    text=f"(≈ {self._format_money(self._php_to_usd(cost), as_usd=True)})"
                )
                self.travel_result_label.configure(
                    text=f"₱{per:,.2f} / {unit}",
                    text_color="#10B981",
                )
                self.travel_result_fx_label.configure(
                    text=f"(≈ ${self._php_to_usd(per):,.2f} USD / {unit})"
                )
            else:
                self.travel_cost_fx_label.configure(
                    text=f"(≈ {self._format_money(self._usd_to_php(cost), as_usd=False)})"
                )
                self.travel_result_label.configure(
                    text=f"${per:,.2f} / {unit}",
                    text_color="#10B981",
                )
                self.travel_result_fx_label.configure(
                    text=f"(≈ ₱{self._usd_to_php(per):,.2f} PHP / {unit})"
                )

            self.travel_status_label.configure(
                text="Trip cost ÷ distance",
                text_color="#9CA3AF",
            )
            self._show_rate_info()
        except ValueError as e:
            self.travel_result_label.configure(
                text="Invalid input", text_color="#EF4444"
            )
            self.travel_result_fx_label.configure(text="")
            self.travel_cost_fx_label.configure(text="")
            msg = str(e) if str(e) else "Enter valid positive numbers."
            if "could not convert" in msg.lower() or "float" in msg.lower():
                msg = "Enter valid positive numbers."
            self.travel_status_label.configure(text=msg, text_color="#EF4444")

    # --------------------------------------------------------------- weight
    def _to_kg(self, value: float, unit: str) -> float:
        if unit == "lb":
            return value * KG_PER_LB
        if unit == "kg":
            return value
        if unit == "g":
            return value / G_PER_KG
        raise ValueError(f"unknown unit {unit}")

    def _from_kg(self, kg: float) -> dict[str, float]:
        return {
            "kg": kg,
            "g": kg * G_PER_KG,
            "lb": kg / KG_PER_LB if KG_PER_LB else 0.0,
        }

    def cycle_weight_unit(self):
        """Cycle input unit lb → kg → g → lb; convert entered value when possible."""
        raw = self.weight_entry.get().strip()
        old = self.weight_unit
        new = WEIGHT_NEXT[old]
        if raw:
            try:
                val = float(raw)
                if val >= 0:
                    kg = self._to_kg(val, old)
                    converted = self._from_kg(kg)[new]
                    self.weight_entry.delete(0, "end")
                    # more precision for grams
                    fmt = f"{converted:.4f}".rstrip("0").rstrip(".")
                    if new == "g" and converted >= 1:
                        fmt = f"{converted:,.2f}"
                    elif new == "g":
                        fmt = f"{converted:.4f}".rstrip("0").rstrip(".")
                    else:
                        fmt = f"{converted:.4f}".rstrip("0").rstrip(".")
                    self.weight_entry.insert(0, fmt)
            except ValueError:
                pass
        self.weight_unit = new
        self._apply_weight_unit_labels()
        self.calculate_weight()

    def _apply_weight_unit_labels(self):
        self.weight_unit_label.configure(text=WEIGHT_LABELS[self.weight_unit])
        self.weight_unit_button.configure(text=WEIGHT_SWITCH_TEXT[self.weight_unit])
        self.weight_entry.configure(placeholder_text=WEIGHT_PLACEHOLDER[self.weight_unit])
        self.weight_hint.configure(
            text=f"Switch cycles units: lb → kg → g → lb  (input is {self.weight_unit})"
        )

    def calculate_weight(self, *_args):
        raw = self.weight_entry.get().strip()
        if not raw:
            self.weight_result_label.configure(text="—", text_color="#3B82F6")
            self.weight_result_secondary.configure(text="")
            self.weight_status.configure(
                text="Enter a weight to convert.", text_color="#9CA3AF"
            )
            return
        try:
            value = float(raw)
            if value < 0:
                raise ValueError("negative")
            kg = self._to_kg(value, self.weight_unit)
            all_u = self._from_kg(kg)

            def fmt(u: str, v: float) -> str:
                if u == "g":
                    return f"{v:,.2f} g" if v >= 0.01 else f"{v:.6f} g"
                if u == "kg":
                    return f"{v:,.4f} kg"
                return f"{v:,.4f} lb"

            others = [u for u in WEIGHT_UNITS if u != self.weight_unit]
            primary = fmt(others[0], all_u[others[0]])
            secondary = fmt(others[1], all_u[others[1]])
            input_fmt = fmt(self.weight_unit, all_u[self.weight_unit])
            self.weight_result_label.configure(text=primary, text_color="#10B981")
            self.weight_result_secondary.configure(
                text=f"{secondary}\n(from {input_fmt})"
            )
            self.weight_status.configure(
                text="lb ↔ kg ↔ g (1 lb = 0.45359237 kg exact)",
                text_color="#9CA3AF",
            )
        except ValueError:
            self.weight_result_label.configure(text="Invalid input", text_color="#EF4444")
            self.weight_result_secondary.configure(text="")
            self.weight_status.configure(
                text="Enter a valid non-negative number.", text_color="#EF4444"
            )

    # ------------------------------------------------------------------- UI
    def _build_ui(self):
        self.header_frame = ctk.CTkFrame(self, fg_color="transparent")
        self.header_frame.pack(pady=(12, 2), padx=20, fill="x")

        self.title_label = ctk.CTkLabel(
            self.header_frame,
            text="Currency Converter",
            font=ctk.CTkFont(size=22, weight="bold"),
        )
        self.title_label.pack()

        self.subtitle_label = ctk.CTkLabel(
            self.header_frame,
            text="Philippine Peso (PHP)  ➔  US Dollar (USD)",
            font=ctk.CTkFont(size=12),
            text_color="#9CA3AF",
        )
        self.subtitle_label.pack(pady=(2, 0))

        self.tabs = ctk.CTkTabview(self)
        self.tabs.pack(pady=6, padx=12, fill="both", expand=True)
        self.tabs.add("Convert")
        self.tabs.add("Travel")
        self.tabs.add("Weight")
        self.tabs.add("Translator")
        self.tabs.add("Settings")

        self._build_convert_tab(self.tabs.tab("Convert"))
        self._build_travel_tab(self.tabs.tab("Travel"))
        self._build_weight_tab(self.tabs.tab("Weight"))
        self._build_translator_tab(self.tabs.tab("Translator"))
        self._build_settings_tab(self.tabs.tab("Settings"))

    def _build_convert_tab(self, parent):
        card = ctk.CTkFrame(parent, corner_radius=12)
        card.pack(pady=8, padx=6, fill="both", expand=True)

        # Amount label + currency switch on one row
        amount_row = ctk.CTkFrame(card, fg_color="transparent")
        amount_row.pack(fill="x", padx=16, pady=(16, 5))
        self.input_label = ctk.CTkLabel(
            amount_row,
            text="Amount in Pesos (₱):",
            font=ctk.CTkFont(size=14, weight="bold"),
        )
        self.input_label.pack(side="left", anchor="w")
        self.swap_button = ctk.CTkButton(
            amount_row,
            text="⇄ USD",
            width=72,
            height=30,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="transparent",
            border_width=1,
            border_color="#3B82F6",
            text_color="#3B82F6",
            hover_color="#1E293B",
            command=self.swap_direction,
        )
        self.swap_button.pack(side="right", padx=(8, 0))

        self.amount_entry = ctk.CTkEntry(
            card,
            placeholder_text="e.g., 1000",
            height=42,
            font=ctk.CTkFont(size=16),
        )
        self.amount_entry.pack(fill="x", padx=16, pady=(0, 8))
        self.amount_entry.bind("<Return>", lambda e: self.convert_currency())

        self.convert_button = ctk.CTkButton(
            card,
            text="Convert to USD",
            height=40,
            font=ctk.CTkFont(size=14, weight="bold"),
            command=self.convert_currency,
        )
        self.convert_button.pack(fill="x", padx=16, pady=6)

        result_box = ctk.CTkFrame(card, fg_color="#1E293B", corner_radius=8)
        result_box.pack(fill="x", padx=16, pady=(14, 8))
        self.result_label = ctk.CTkLabel(
            result_box,
            text="$0.00 USD",
            font=ctk.CTkFont(size=32, weight="bold"),
            text_color="#3B82F6",
        )
        self.result_label.pack(pady=16)

        self.rate_info_label = ctk.CTkLabel(
            card,
            text=f"Rate: 1 PHP = ${self.exchange_rate:.4f} USD",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.rate_info_label.pack(pady=(0, 12))

    def _build_travel_tab(self, parent):
        # Scrollable so large fonts never clip the cost-per-km result
        scroll = ctk.CTkScrollableFrame(parent, corner_radius=12)
        scroll.pack(pady=8, padx=6, fill="both", expand=True)
        card = scroll

        self.travel_currency_hint = ctk.CTkLabel(
            card,
            text="Enter cost in pesos; switch for dollars",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.travel_currency_hint.pack(anchor="w", padx=12, pady=(10, 6))

        # Distance label + unit switch on one row
        dist_row = ctk.CTkFrame(card, fg_color="transparent")
        dist_row.pack(fill="x", padx=12, pady=(6, 2))
        self.distance_unit_label = ctk.CTkLabel(
            dist_row,
            text="Distance (km):",
            font=ctk.CTkFont(size=14, weight="bold"),
        )
        self.distance_unit_label.pack(side="left", anchor="w")
        self.unit_switch_button = ctk.CTkButton(
            dist_row,
            text="⇄ mi",
            width=72,
            height=30,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="transparent",
            border_width=1,
            border_color="#3B82F6",
            text_color="#3B82F6",
            hover_color="#1E293B",
            command=self.swap_distance_unit,
        )
        self.unit_switch_button.pack(side="right", padx=(8, 0))

        self.distance_entry = ctk.CTkEntry(
            card,
            placeholder_text="e.g., 120",
            height=40,
            font=ctk.CTkFont(size=15),
        )
        self.distance_entry.pack(fill="x", padx=12, pady=(0, 2))
        self.distance_entry.bind("<KeyRelease>", self.calculate_travel)
        self.distance_entry.bind("<Return>", self.calculate_travel)

        self.distance_equiv_label = ctk.CTkLabel(
            card,
            text="",
            font=ctk.CTkFont(size=11),
            text_color="#6B7280",
        )
        self.distance_equiv_label.pack(anchor="w", padx=16, pady=(0, 8))

        # Trip cost label + currency switch on one row
        cost_row = ctk.CTkFrame(card, fg_color="transparent")
        cost_row.pack(fill="x", padx=12, pady=(4, 2))
        self.travel_cost_label = ctk.CTkLabel(
            cost_row,
            text="Trip cost (₱ PHP):",
            font=ctk.CTkFont(size=14, weight="bold"),
        )
        self.travel_cost_label.pack(side="left", anchor="w")
        self.travel_currency_button = ctk.CTkButton(
            cost_row,
            text="⇄ USD",
            width=72,
            height=30,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="transparent",
            border_width=1,
            border_color="#10B981",
            text_color="#10B981",
            hover_color="#1E293B",
            command=self.swap_travel_currency,
        )
        self.travel_currency_button.pack(side="right", padx=(8, 0))

        self.travel_cost_entry = ctk.CTkEntry(
            card,
            placeholder_text="e.g., 2500",
            height=40,
            font=ctk.CTkFont(size=15),
        )
        self.travel_cost_entry.pack(fill="x", padx=12, pady=(0, 2))
        self.travel_cost_entry.bind("<KeyRelease>", self.calculate_travel)
        self.travel_cost_entry.bind("<Return>", self.calculate_travel)

        self.travel_cost_fx_label = ctk.CTkLabel(
            card,
            text="",
            font=ctk.CTkFont(size=11),
            text_color="#6B7280",
        )
        self.travel_cost_fx_label.pack(anchor="w", padx=16, pady=(0, 6))

        # Result box placed early so it is not pushed off-screen
        result_box = ctk.CTkFrame(card, fg_color="#1E293B", corner_radius=8)
        result_box.pack(fill="x", padx=12, pady=(8, 6))
        self.travel_result_label = ctk.CTkLabel(
            result_box,
            text="— / km",
            font=ctk.CTkFont(size=32, weight="bold"),
            text_color="#3B82F6",
        )
        self.travel_result_label.pack(pady=(16, 4), padx=8)
        self.travel_result_fx_label = ctk.CTkLabel(
            result_box,
            text="",
            font=ctk.CTkFont(size=14),
            text_color="#E5E7EB",
        )
        self.travel_result_fx_label.pack(pady=(0, 16), padx=8)

        self.travel_calc_button = ctk.CTkButton(
            card,
            text="Calculate cost per km",
            height=40,
            font=ctk.CTkFont(size=14, weight="bold"),
            command=self.calculate_travel,
        )
        self.travel_calc_button.pack(fill="x", padx=12, pady=8)

        self.travel_status_label = ctk.CTkLabel(
            card,
            text="Enter distance and trip cost.",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.travel_status_label.pack(pady=(2, 2))

        self.travel_rate_label = ctk.CTkLabel(
            card,
            text=f"Rate: 1 PHP = ${self.exchange_rate:.4f} USD",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.travel_rate_label.pack(pady=(0, 12))

    def _build_weight_tab(self, parent):
        card = ctk.CTkFrame(parent, corner_radius=12)
        card.pack(pady=8, padx=6, fill="both", expand=True)

        self.weight_hint = ctk.CTkLabel(
            card,
            text="Switch cycles units: lb → kg → g → lb  (input is lb)",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.weight_hint.pack(anchor="w", padx=16, pady=(14, 6))

        # Label + unit cycle switch adjacent
        w_row = ctk.CTkFrame(card, fg_color="transparent")
        w_row.pack(fill="x", padx=16, pady=(6, 2))
        self.weight_unit_label = ctk.CTkLabel(
            w_row,
            text="Weight (lb):",
            font=ctk.CTkFont(size=14, weight="bold"),
        )
        self.weight_unit_label.pack(side="left", anchor="w")
        self.weight_unit_button = ctk.CTkButton(
            w_row,
            text="⇄ kg",
            width=72,
            height=30,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="transparent",
            border_width=1,
            border_color="#3B82F6",
            text_color="#3B82F6",
            hover_color="#1E293B",
            command=self.cycle_weight_unit,
        )
        self.weight_unit_button.pack(side="right", padx=(8, 0))

        self.weight_entry = ctk.CTkEntry(
            card,
            placeholder_text="e.g., 150",
            height=42,
            font=ctk.CTkFont(size=16),
        )
        self.weight_entry.pack(fill="x", padx=16, pady=(0, 4))
        self.weight_entry.bind("<KeyRelease>", self.calculate_weight)
        self.weight_entry.bind("<Return>", self.calculate_weight)

        self.weight_calc_button = ctk.CTkButton(
            card,
            text="Convert weight",
            height=40,
            font=ctk.CTkFont(size=14, weight="bold"),
            command=self.calculate_weight,
        )
        self.weight_calc_button.pack(fill="x", padx=16, pady=8)

        result_box = ctk.CTkFrame(card, fg_color="#1E293B", corner_radius=8)
        result_box.pack(fill="x", padx=16, pady=(8, 6))
        self.weight_result_label = ctk.CTkLabel(
            result_box,
            text="—",
            font=ctk.CTkFont(size=32, weight="bold"),
            text_color="#3B82F6",
        )
        self.weight_result_label.pack(pady=(16, 4), padx=8)
        self.weight_result_secondary = ctk.CTkLabel(
            result_box,
            text="",
            font=ctk.CTkFont(size=14),
            text_color="#E5E7EB",
            justify="center",
        )
        self.weight_result_secondary.pack(pady=(0, 16), padx=8)

        self.weight_status = ctk.CTkLabel(
            card,
            text="Enter a weight to convert.",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.weight_status.pack(pady=(4, 12))

    # ------------------------------------------------------------ translator
    def _build_translator_tab(self, parent):
        """Port of goobleTranslator: translate + optional phonics, multi-backend AI."""
        scroll = ctk.CTkScrollableFrame(parent, corner_radius=12)
        scroll.pack(pady=6, padx=4, fill="both", expand=True)
        card = scroll

        ctk.CTkLabel(
            card,
            text="Translator (from goobleTranslator)",
            font=ctk.CTkFont(size=15, weight="bold"),
        ).pack(anchor="w", padx=10, pady=(8, 2))

        # Provider row
        prov = ctk.CTkFrame(card, fg_color="transparent")
        prov.pack(fill="x", padx=10, pady=4)
        ctk.CTkLabel(prov, text="AI backend:", font=ctk.CTkFont(size=12, weight="bold")).pack(
            side="left"
        )
        labels = [BACKEND_LABELS[b] for b, _ in list_backends()]
        id_by_label = {BACKEND_LABELS[b]: b for b, _ in list_backends()}
        self._backend_id_by_label = id_by_label
        active = self.secrets.get("active_backend", "offline")
        active_label = BACKEND_LABELS.get(active, BACKEND_LABELS["offline"])
        self.translator_backend_menu = ctk.CTkOptionMenu(
            prov,
            values=labels,
            width=200,
            command=self._on_backend_change,
        )
        self.translator_backend_menu.set(active_label)
        self.translator_backend_menu.pack(side="left", padx=8)
        self.translator_status = ctk.CTkLabel(
            card, text="Checking backend…", font=ctk.CTkFont(size=11), text_color="#9CA3AF"
        )
        self.translator_status.pack(anchor="w", padx=12, pady=(0, 6))

        ctk.CTkLabel(
            card, text="Source text:", font=ctk.CTkFont(size=12, weight="bold")
        ).pack(anchor="w", padx=10, pady=(4, 2))
        self.translator_input = ctk.CTkTextbox(card, height=90, wrap="word")
        self.translator_input.pack(fill="x", padx=10, pady=2)

        lang_row = ctk.CTkFrame(card, fg_color="transparent")
        lang_row.pack(fill="x", padx=10, pady=6)
        ctk.CTkLabel(lang_row, text="Translate to:", font=ctk.CTkFont(size=12)).pack(
            side="left"
        )
        self.translator_lang = ctk.CTkOptionMenu(lang_row, values=LANGUAGES, width=180)
        self.translator_lang.set("Spanish")
        self.translator_lang.pack(side="left", padx=8)
        self.translator_go_btn = ctk.CTkButton(
            lang_row, text="Translate", width=100, command=self._translator_run_translate
        )
        self.translator_go_btn.pack(side="right")

        ctk.CTkLabel(
            card, text="Result:", font=ctk.CTkFont(size=12, weight="bold")
        ).pack(anchor="w", padx=10, pady=(6, 2))
        self.translator_output = ctk.CTkTextbox(card, height=100, wrap="word")
        self.translator_output.pack(fill="x", padx=10, pady=2)

        phon_row = ctk.CTkFrame(card, fg_color="transparent")
        phon_row.pack(fill="x", padx=10, pady=6)
        ctk.CTkLabel(phon_row, text="Phonics as:", font=ctk.CTkFont(size=12)).pack(
            side="left"
        )
        self.translator_phon_lang = ctk.CTkOptionMenu(
            phon_row, values=LANGUAGES, width=180
        )
        self.translator_phon_lang.set("English")
        self.translator_phon_lang.pack(side="left", padx=8)
        self.translator_phon_btn = ctk.CTkButton(
            phon_row, text="Phonics", width=100, command=self._translator_run_phonics
        )
        self.translator_phon_btn.pack(side="right")

        btn_row = ctk.CTkFrame(card, fg_color="transparent")
        btn_row.pack(fill="x", padx=10, pady=8)
        ctk.CTkButton(
            btn_row, text="Clear result", width=120, command=self._translator_clear
        ).pack(side="left")
        ctk.CTkButton(
            btn_row,
            text="Check backend",
            width=120,
            fg_color="transparent",
            border_width=1,
            border_color="#3B82F6",
            text_color="#3B82F6",
            command=self._translator_refresh_status,
        ).pack(side="left", padx=8)

        ctk.CTkLabel(
            card,
            text="Set API keys / Ollama model in Settings. Keys are stored in secrets.json (gitignored).",
            font=ctk.CTkFont(size=11),
            text_color="#6B7280",
            wraplength=460,
            justify="left",
        ).pack(anchor="w", padx=10, pady=(4, 10))

        self._translator_refresh_status()

    def _on_backend_change(self, label: str):
        bid = self._backend_id_by_label.get(label, "offline")
        self.secrets.set("active_backend", bid)
        self.secrets.save()
        self._translator_refresh_status()

    def _translator_refresh_status(self):
        try:
            backend = get_backend(self.secrets)
            ok, msg = backend.available()
            color = "#10B981" if ok else "#EF4444"
            self.translator_status.configure(text=msg, text_color=color)
        except Exception as e:
            self.translator_status.configure(
                text=f"Backend error: {e}", text_color="#EF4444"
            )

    def _translator_clear(self):
        self.translator_output.delete("0.0", "end")

    def _translator_set_busy(self, busy: bool):
        self._translator_busy = busy
        state = "disabled" if busy else "normal"
        for w in (
            self.translator_go_btn,
            self.translator_phon_btn,
            self.translator_backend_menu,
        ):
            try:
                w.configure(state=state)
            except Exception:
                pass
        if busy:
            self.translator_status.configure(
                text="Working… (AI call in background)", text_color="#FBBF24"
            )

    def _translator_run_translate(self):
        if self._translator_busy:
            return
        text = self.translator_input.get("0.0", "end").strip()
        lang = self.translator_lang.get()
        if not text:
            self.translator_status.configure(
                text="Enter source text first.", text_color="#EF4444"
            )
            return
        if lang not in LANGUAGES:
            self.translator_status.configure(
                text="Pick a target language.", text_color="#EF4444"
            )
            return

        def work():
            try:
                backend = get_backend(self.secrets)
                result = backend.translate(text, lang)
                self.after(0, lambda: self._translator_show_result(result, ok=True))
            except Exception as e:
                err = str(e)
                self.after(0, lambda: self._translator_show_result(err, ok=False))

        self._translator_set_busy(True)
        threading.Thread(target=work, daemon=True).start()

    def _translator_run_phonics(self):
        if self._translator_busy:
            return
        # Prefer output text (translated); fall back to source
        text = self.translator_output.get("0.0", "end").strip()
        if not text:
            text = self.translator_input.get("0.0", "end").strip()
        lang = self.translator_phon_lang.get()
        if not text:
            self.translator_status.configure(
                text="Translate first (or enter text), then run Phonics.",
                text_color="#EF4444",
            )
            return

        def work():
            try:
                backend = get_backend(self.secrets)
                result = backend.phonetics(text, lang)
                self.after(0, lambda: self._translator_show_result(result, ok=True, append=True))
            except Exception as e:
                err = str(e)
                self.after(0, lambda: self._translator_show_result(err, ok=False))

        self._translator_set_busy(True)
        threading.Thread(target=work, daemon=True).start()

    def _translator_show_result(self, text: str, ok: bool, append: bool = False):
        self._translator_set_busy(False)
        if ok:
            if not append:
                self.translator_output.delete("0.0", "end")
            else:
                self.translator_output.insert("end", "\n\n— phonics —\n")
            self.translator_output.insert("end" if append else "0.0", text)
            self._translator_refresh_status()
        else:
            self.translator_status.configure(text=text[:400], text_color="#EF4444")

    def _build_settings_tab(self, parent):
        scroll = ctk.CTkScrollableFrame(parent, corner_radius=12)
        scroll.pack(pady=8, padx=6, fill="both", expand=True)
        card = scroll

        ctk.CTkLabel(
            card,
            text="Display",
            font=ctk.CTkFont(size=16, weight="bold"),
        ).pack(anchor="w", padx=12, pady=(12, 8))

        ctk.CTkLabel(
            card,
            text="Result text size (Convert + Travel + Weight)",
            font=ctk.CTkFont(size=13),
            text_color="#9CA3AF",
        ).pack(anchor="w", padx=12, pady=(0, 8))

        self.size_var = ctk.StringVar(value=self.result_text_size)
        for key in RESULT_SIZES:
            ctk.CTkRadioButton(
                card,
                text=key,
                variable=self.size_var,
                value=key,
                command=lambda k=key: self.set_result_text_size(k),
                font=ctk.CTkFont(size=14),
            ).pack(anchor="w", padx=20, pady=4)

        self.settings_status = ctk.CTkLabel(
            card,
            text=f"Result text size: {self.result_text_size}",
            font=ctk.CTkFont(size=12),
            text_color="#9CA3AF",
        )
        self.settings_status.pack(anchor="w", padx=12, pady=(12, 8))

        # --- AI / Translator secrets ---
        ctk.CTkLabel(
            card,
            text="AI provider (Translator)",
            font=ctk.CTkFont(size=16, weight="bold"),
        ).pack(anchor="w", padx=12, pady=(16, 6))

        ctk.CTkLabel(
            card,
            text="Keys are saved only in secrets.json (gitignored, mode 600). Never commit them.",
            font=ctk.CTkFont(size=11),
            text_color="#6B7280",
            wraplength=440,
            justify="left",
        ).pack(anchor="w", padx=12, pady=(0, 8))

        # Ollama
        ctk.CTkLabel(card, text="Ollama base URL", font=ctk.CTkFont(size=12, weight="bold")).pack(
            anchor="w", padx=12
        )
        self.set_ollama_url = ctk.CTkEntry(card, height=32)
        self.set_ollama_url.insert(0, str(self.secrets.get("ollama_base_url", "")))
        self.set_ollama_url.pack(fill="x", padx=12, pady=2)
        ctk.CTkLabel(card, text="Ollama model (e.g. tinyllama)", font=ctk.CTkFont(size=12, weight="bold")).pack(
            anchor="w", padx=12, pady=(6, 0)
        )
        self.set_ollama_model = ctk.CTkEntry(card, height=32)
        self.set_ollama_model.insert(0, str(self.secrets.get("ollama_model", "tinyllama")))
        self.set_ollama_model.pack(fill="x", padx=12, pady=2)

        # xAI
        ctk.CTkLabel(
            card,
            text=f"xAI / Grok API key  {SecretsStore.mask_key(str(self.secrets.get('xai_api_key') or ''))}",
            font=ctk.CTkFont(size=12, weight="bold"),
        ).pack(anchor="w", padx=12, pady=(10, 0))
        self.set_xai_key = ctk.CTkEntry(card, height=32, placeholder_text="xai-… (leave blank to keep)")
        self.set_xai_key.pack(fill="x", padx=12, pady=2)
        self.set_xai_model = ctk.CTkEntry(card, height=32, placeholder_text="model e.g. grok-4.5")
        self.set_xai_model.insert(0, str(self.secrets.get("xai_model", "grok-4.5")))
        self.set_xai_model.pack(fill="x", padx=12, pady=2)

        # OpenAI
        ctk.CTkLabel(
            card,
            text=f"OpenAI API key  {SecretsStore.mask_key(str(self.secrets.get('openai_api_key') or ''))}",
            font=ctk.CTkFont(size=12, weight="bold"),
        ).pack(anchor="w", padx=12, pady=(10, 0))
        self.set_openai_key = ctk.CTkEntry(card, height=32, placeholder_text="sk-… (leave blank to keep)")
        self.set_openai_key.pack(fill="x", padx=12, pady=2)
        self.set_openai_model = ctk.CTkEntry(card, height=32)
        self.set_openai_model.insert(0, str(self.secrets.get("openai_model", "gpt-4o-mini")))
        self.set_openai_model.pack(fill="x", padx=12, pady=2)

        # HF
        ctk.CTkLabel(
            card,
            text=f"Hugging Face token  {SecretsStore.mask_key(str(self.secrets.get('hf_token') or ''))}",
            font=ctk.CTkFont(size=12, weight="bold"),
        ).pack(anchor="w", padx=12, pady=(10, 0))
        self.set_hf_token = ctk.CTkEntry(card, height=32, placeholder_text="hf_… (leave blank to keep)")
        self.set_hf_token.pack(fill="x", padx=12, pady=2)
        self.set_hf_model = ctk.CTkEntry(card, height=32)
        self.set_hf_model.insert(
            0, str(self.secrets.get("hf_model", "meta-llama/Llama-3.2-3B-Instruct"))
        )
        self.set_hf_model.pack(fill="x", padx=12, pady=2)

        ctk.CTkButton(
            card, text="Save AI settings", command=self._save_ai_settings, height=36
        ).pack(fill="x", padx=12, pady=12)

        self.ai_settings_status = ctk.CTkLabel(
            card, text="", font=ctk.CTkFont(size=12), text_color="#9CA3AF"
        )
        self.ai_settings_status.pack(anchor="w", padx=12, pady=(0, 8))

        ctk.CTkLabel(
            card,
            text="Default backend: Free offline (no API key) on the Translator tab.\n"
            "xAI needs team credits at https://console.x.ai/ (key alone is not enough).\n"
            "Ollama (true local LLM): install from ollama.com, then\n"
            "  ./scripts/setup_ollama_tiny.sh tinyllama\n"
            "On-device MT (Opus-MT / llama.cpp) is planned — see docs/MOBILE_LLM_PLAN.md.",
            font=ctk.CTkFont(size=11),
            text_color="#6B7280",
            justify="left",
        ).pack(anchor="w", padx=12, pady=(4, 16))

    def _save_ai_settings(self):
        self.secrets.set("ollama_base_url", self.set_ollama_url.get().strip())
        self.secrets.set("ollama_model", self.set_ollama_model.get().strip() or "tinyllama")
        xai = self.set_xai_key.get().strip()
        if xai:
            self.secrets.set("xai_api_key", xai)
        self.secrets.set("xai_model", self.set_xai_model.get().strip() or "grok-4.5")
        oai = self.set_openai_key.get().strip()
        if oai:
            self.secrets.set("openai_api_key", oai)
        self.secrets.set(
            "openai_model", self.set_openai_model.get().strip() or "gpt-4o-mini"
        )
        hf = self.set_hf_token.get().strip()
        if hf:
            self.secrets.set("hf_token", hf)
        self.secrets.set(
            "hf_model",
            self.set_hf_model.get().strip() or "meta-llama/Llama-3.2-3B-Instruct",
        )
        self.secrets.save()
        # clear password fields after save
        self.set_xai_key.delete(0, "end")
        self.set_openai_key.delete(0, "end")
        self.set_hf_token.delete(0, "end")
        self.ai_settings_status.configure(
            text="AI settings saved to secrets.json", text_color="#10B981"
        )
        if hasattr(self, "translator_status"):
            self._translator_refresh_status()


if __name__ == "__main__":
    app = CurrencyConverterApp()
    app.mainloop()
