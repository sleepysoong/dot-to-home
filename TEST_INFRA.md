# E2E Test Infra: dot-to-home

## Test Philosophy
- Use Robolectric to emulate Android SDK context (graphics, SharedPreferences, resources) on the JVM.
- Verify AppSettings deserialization, AppConfig boundaries, WallpaperGenerator drawing fallback, and MainActivity composition state flow.

## Feature Inventory
| # | Feature | Source (requirement) | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|:------:|:------:|:------:|
| 1 | D-Day Card CRUD | ORIGINAL_REQUEST R2 | 5 | 5 | ✓ |
| 2 | Config Persistence / Restore | ORIGINAL_REQUEST R3 | 5 | 5 | ✓ |
| 3 | Wallpaper Bitmap Rendering | ORIGINAL_REQUEST R3 | 5 | 5 | ✓ |
| 4 | Preview & Positioning | ORIGINAL_REQUEST R2 | 5 | 5 | ✓ |

## Test Architecture
- **Frameworks**: JUnit 4, Robolectric, Mockk.
- **Location**: `app/src/test/java/com/sleepysoong/dottohome/`
- **Runner**: `./gradlew testDebugUnitTest`

## Test Suite Case Inventory

### Tier 1: Feature Coverage (Happy Path)
1. **testAddDDayItem**: Verifies adding a new card increases count and returns valid AppConfig.
2. **testDeleteDDayItem**: Verifies deleting a card decreases count.
3. **testEditDDayItem**: Verifies editing shape/color/date updates fields.
4. **testSaveAndLoadConfig**: Verifies valid AppConfig JSON is saved and restored from SharedPreferences.
5. **testWallpaperBitmapGeneration**: Verifies WallpaperGenerator creates a non-null Bitmap under normal conditions.

### Tier 2: Boundary & Corner Cases (Robustness)
6. **testMaxCardsConstraint**: Attempting to add >5 cards fails/reverted.
7. **testMinCardsConstraint**: Attempting to delete when count is 1 fails/reverted.
8. **testCorruptJsonRestoreFallback**: Corrupted JSON in SharedPreferences restores default config instead of crashing.
9. **testEmptyItemListFallback**: AppConfig loaded with empty DDayItem list fallbacks to a default list containing 1 item.
10. **testBitmapGenerationFailureFallback**: Drawing canvas exceptions/OOM fallbacks to drawing a white background without throwing.

### Tier 3: Cross-Feature Combinations
11. **testEditAndInstantPreviewState**: Modifying multiple card attributes and offset sliders, verifying state updates immediately.
12. **testToggleLockHomeScreens**: Switching between Home and Lock screen previews updates preview configuration.

### Tier 4: Real-World Application Scenarios
13. **testColdStartSettingsRestorationAndEditing**: Emulates app startup, reading saved cards, adding new ones, updating sliders, and writing final config to disk.
