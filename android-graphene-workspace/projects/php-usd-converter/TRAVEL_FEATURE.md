# Plan: Travel tab (php-usd-converter)

## Goal

Add a **Travel** tab for trip cost analysis that works for both **US (miles/USD)**
and **Philippines (km/PHP)** users, reusing the existing live FX rate.

## UX

| Control | Behavior |
|---|---|
| **Distance** | Numeric entry |
| **Unit switch** | km ↔ miles |
| **Subtle unit hint** | If miles active: `(≈ X.XX km)`; if km: `(≈ X.XX mi)` |
| **Trip cost** | Total cost for the trip (same primary currency as Convert tab) |
| **Subtle cost FX** | Opposite currency total in parentheses using live rate |
| **Result** | `cost ÷ distance` → cost per km or per mile |
| **Subtle per-unit FX** | Opposite currency per unit in parentheses |

Currency primary follows the Convert tab’s PHP↔USD direction (shared rate +
swap). Travel does not invent a second FX source.

## Math

```text
KM_PER_MILE = 1.609344
miles → km:  mi * KM_PER_MILE
km → miles:  km / KM_PER_MILE
cost_per_unit = trip_cost / distance   (distance in active unit)
PHP → USD:  php * rate
USD → PHP:  usd / rate
```

Validate: distance > 0, cost ≥ 0, numeric only.

## Surfaces

1. **Desktop** `app.py` — `CTkTabview`: Convert | Travel  
2. **Android** — `TabLayout` + two panels (Convert keeps current UI; Travel new)

## Version

SemVer **minor**: `1.1.1` → **`1.2.0`** (`versionCode` 10200).

## Out of scope (this iteration)

- Maps / GPS / routing APIs  
- Fuel economy models  
- Multi-leg trips  
