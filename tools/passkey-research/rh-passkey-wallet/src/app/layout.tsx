import type { Metadata } from "next";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "RH Passkey Wallet PoC",
  description: "Turnkey passkey login for Robinhood Chain",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          background: "#020617",
          color: "#f8fafc",
          fontFamily:
            "ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif",
        }}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
