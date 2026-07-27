#!/usr/bin/env python3
"""PHP ↔ USD converter + Travel cost-per-distance (CustomTkinter)."""

from __future__ import annotations

import json
from pathlib import Path

import customtkinter as ctk
import requests

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

KM_PER_MILE = 1.609344
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

        self.title("PHP ↔ USD Converter")
        self.geometry("440x640")
        self.minsize(400, 560)
        self.resizable(True, True)

        self._settings = load_settings()
        size_key = self._settings.get("result_text_size", DEFAULT_RESULT_SIZE)
        if size_key not in RESULT_SIZES:
            size_key = DEFAULT_RESULT_SIZE
        self.result_text_size = size_key

        self.exchange_rate = self.get_live_rate()
        self.php_to_usd = True  # Convert tab primary currency
        self.travel_php = True  # Travel tab primary currency (independent)
        self.use_km = True

        self._build_ui()
        self._apply_direction_labels()
        self._apply_distance_unit_labels()
        self._apply_travel_currency_labels()
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
        self.tabs.add("Settings")

        self._build_convert_tab(self.tabs.tab("Convert"))
        self._build_travel_tab(self.tabs.tab("Travel"))
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

    def _build_settings_tab(self, parent):
        card = ctk.CTkFrame(parent, corner_radius=12)
        card.pack(pady=8, padx=6, fill="both", expand=True)

        ctk.CTkLabel(
            card,
            text="Display",
            font=ctk.CTkFont(size=16, weight="bold"),
        ).pack(anchor="w", padx=16, pady=(16, 8))

        ctk.CTkLabel(
            card,
            text="Result text size (Convert + Travel cost-per-distance)",
            font=ctk.CTkFont(size=13),
            text_color="#9CA3AF",
        ).pack(anchor="w", padx=16, pady=(0, 8))

        self.size_var = ctk.StringVar(value=self.result_text_size)
        for key in RESULT_SIZES:
            ctk.CTkRadioButton(
                card,
                text=key,
                variable=self.size_var,
                value=key,
                command=lambda k=key: self.set_result_text_size(k),
                font=ctk.CTkFont(size=14),
            ).pack(anchor="w", padx=24, pady=4)

        self.settings_status = ctk.CTkLabel(
            card,
            text=f"Result text size: {self.result_text_size}",
            font=ctk.CTkFont(size=12),
            text_color="#9CA3AF",
        )
        self.settings_status.pack(anchor="w", padx=16, pady=(16, 8))

        ctk.CTkLabel(
            card,
            text="Tip: Travel tab scrolls if the window is small.\n"
            "Use Large or Extra large if cost-per-km is hard to read.",
            font=ctk.CTkFont(size=12),
            text_color="#6B7280",
            justify="left",
        ).pack(anchor="w", padx=16, pady=(8, 16))


if __name__ == "__main__":
    app = CurrencyConverterApp()
    app.mainloop()
