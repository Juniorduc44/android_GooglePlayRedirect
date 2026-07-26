#!/usr/bin/env python3
"""PHP to USD currency converter GUI (CustomTkinter)."""

import customtkinter as ctk
import requests

# Set default theme and appearance mode
ctk.set_appearance_mode("Dark")  # Options: "System", "Dark", "Light"
ctk.set_default_color_theme("blue")  # Options: "blue", "green", "dark-blue"


class CurrencyConverterApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        # Configure Main Window
        self.title("PHP to USD Converter")
        self.geometry("400x480")
        self.resizable(False, False)

        # Application State
        self.exchange_rate = self.get_live_rate()

        # Build UI Components
        self._build_ui()

    def get_live_rate(self) -> float:
        """Fetch real-time PHP to USD rate with a fallback if offline."""
        try:
            url = "https://api.exchangerate-api.com/v4/latest/PHP"
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            data = response.json()
            return float(data["rates"]["USD"])
        except Exception:
            # Fallback approximate conversion rate
            return 0.0175

    def convert_currency(self):
        """Converts the entered PHP amount into USD."""
        raw_input = self.amount_entry.get().strip()

        if not raw_input:
            self.result_label.configure(
                text="$0.00 USD", text_color="#3B82F6"
            )
            self.rate_info_label.configure(
                text="Please enter an amount.", text_color="#EF4444"
            )
            return

        try:
            php_amount = float(raw_input)
            if php_amount < 0:
                raise ValueError("Negative number")

            usd_amount = php_amount * self.exchange_rate
            self.result_label.configure(
                text=f"${usd_amount:,.2f} USD", text_color="#10B981"
            )
            self.rate_info_label.configure(
                text=f"Rate: 1 PHP = ${self.exchange_rate:.4f} USD",
                text_color="#9CA3AF",
            )

        except ValueError:
            self.result_label.configure(
                text="Invalid Input", text_color="#EF4444"
            )
            self.rate_info_label.configure(
                text="Please enter a valid positive number.",
                text_color="#EF4444",
            )

    def _build_ui(self):
        # Header Container
        self.header_frame = ctk.CTkFrame(self, fg_color="transparent")
        self.header_frame.pack(pady=(25, 10), padx=20, fill="x")

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

        # Main Card Frame
        self.card_frame = ctk.CTkFrame(self, corner_radius=12)
        self.card_frame.pack(pady=15, padx=20, fill="both", expand=True)

        # Input Label
        self.input_label = ctk.CTkLabel(
            self.card_frame,
            text="Amount in Pesos (₱):",
            font=ctk.CTkFont(size=14, weight="bold"),
        )
        self.input_label.pack(anchor="w", pady=(20, 5), padx=20)

        # Amount Entry Field
        self.amount_entry = ctk.CTkEntry(
            self.card_frame,
            placeholder_text="e.g., 1000",
            height=45,
            font=ctk.CTkFont(size=16),
        )
        self.amount_entry.pack(fill="x", padx=20, pady=(0, 15))
        self.amount_entry.bind(
            "<Return>", lambda event: self.convert_currency()
        )

        # Convert Button
        self.convert_button = ctk.CTkButton(
            self.card_frame,
            text="Convert to USD",
            height=40,
            font=ctk.CTkFont(size=14, weight="bold"),
            command=self.convert_currency,
        )
        self.convert_button.pack(fill="x", padx=20, pady=5)

        # Result Display Box
        self.result_display_frame = ctk.CTkFrame(
            self.card_frame,
            fg_color="#1E293B",
            corner_radius=8,
        )
        self.result_display_frame.pack(fill="x", padx=20, pady=(20, 10))

        self.result_label = ctk.CTkLabel(
            self.result_display_frame,
            text="$0.00 USD",
            font=ctk.CTkFont(size=24, weight="bold"),
            text_color="#3B82F6",
        )
        self.result_label.pack(pady=12)

        # Exchange Rate Info Footer
        self.rate_info_label = ctk.CTkLabel(
            self.card_frame,
            text=f"Rate: 1 PHP = ${self.exchange_rate:.4f} USD",
            font=ctk.CTkFont(size=11),
            text_color="#9CA3AF",
        )
        self.rate_info_label.pack(pady=(0, 15))


if __name__ == "__main__":
    app = CurrencyConverterApp()
    app.mainloop()
