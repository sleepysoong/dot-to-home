# Project: dot-to-home Refactoring

## Architecture
- **Target**: Kotlin / Jetpack Compose Android app.
- **Entry point**: `MainActivity.kt`
- **Data persistence**: SharedPreferences for saving D-Day card configurations.
- **Background updates**: `WallpaperWorker` and `MidnightUpdateReceiver` trigger `WallpaperGenerator` to render the wallpaper.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|---|---|---|---|
| 1 | M1: Exploration & Architecture | Analyze codebase structure and test environment | None | DONE |
| 2 | M2: E2E Test Suite Development | Create requirement-driven E2E test suite (Tiers 1-4) | M1 | IN_PROGRESS |
| 3 | M3: R1 Modularization | Extract Glass UI and other composables, reduce MainActivity line count | M1 | IN_PROGRESS |
| 4 | M4: R2 UI/UX Improvements | Single-screen CRUD, animations, and immediate preview updates | M3 | PLANNED |
| 5 | M5: R3 Stability Hardening | Add fallbacks for SharedPreferences, bitmaps, and enforce minimum 1 card | M3 | PLANNED |
| 6 | M6: E2E Integration & Verification | Run tests, fix bugs, adversarial hardening, integrity audit | M2, M4, M5 | PLANNED |

## Interface Contracts
### `MainActivity` ↔ `AppConfig`
- AppConfig represents the list of DDayItem configurations.
- Read/Write operations via SharedPreferences.
- If JSON deserialization fails or config is empty/invalid, must return default config with at least 1 DDayItem.
