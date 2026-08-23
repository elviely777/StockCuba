# Proposal: Ajustes Complete Implementation

## Intent

The Ajustes (Settings) screen has 6 of 8 features stubbed or broken. Users cannot actually change themes, secure sensitive sections, backup/restore data, validate business inputs, or send feedback. This change delivers production-ready implementations for all missing features following Clean Architecture + MVVM patterns with strict TDD.

## Scope

### In Scope
- **Theme Application**: CompositionLocal + SideEffect in StockCubaTheme reading from DataStore (SYSTEM/LIGHT/DARK)
- **Validation**: Inline validation for nombre (required, max 100), teléfono (Cuban format regex), impuesto (0-100, 2 decimals)
- **Export Backup (real)**: Copy Room DB files to Downloads via MediaStore with IS_PENDING pattern, return real URI
- **Import Backup (real)**: ACTION_OPEN_DOCUMENT (SAF) to pick .db file, atomic replace + app restart
- **Reset Data with Confirmation**: Typed confirmation dialog ("REINICIAR"), delete DB files + selective DataStore clear
- **PIN + Biometric Security**: PBKDF2 PIN hash in DataStore, BiometricPrompt as convenience, fallback PIN screen, SecurityGate composable for sensitive routes (ventas, inventario, ajustes)
- **Feedback (mailto:)**: Simple intent with device context prefilled

### Out of Scope
- Cloud sync/backup (Google Drive, etc.)
- Multi-device settings sync
- Advanced biometric policies (device credential fallback, auth validity duration)
- Settings versioning/migration framework
- Export to formats other than .db (CSV, JSON)

## Capabilities

### New Capabilities
- `theme-application`: Live theme switching via CompositionLocal, reads from DataStore, applies SYSTEM/LIGHT/DARK
- `settings-validation`: Inline validation rules for business fields with error states
- `backup-export`: MediaStore export of Room DB to Downloads with IS_PENDING, returns content URI
- `backup-import`: SAF document picker for .db, atomic replace + process restart
- `data-reset`: Typed confirmation ("REINICIAR"), deletes DB files, clears selective DataStore keys
- `pin-biometric-security`: PBKDF2 PIN hash in DataStore, BiometricPrompt convenience, SecurityGate for protected routes
- `feedback-mailto`: mailto intent with device/app context prefilled

### Modified Capabilities
- `settings-data-store`: Add PIN hash/salt keys, biometric availability flag, validation error states
- `settings-screen`: Wire all 7 features, add validation UI, SecurityGate integration, confirmation dialogs

## Approach

All work follows existing Clean + MVVM + Hilt + DataStore + Room + Compose M3 patterns. No structural refactors needed.

**Architecture decisions:**
1. Theme: `ThemeViewModel` + `CompositionLocalProvider` in `StockCubaTheme` reads `tema` from DataStore via `AjustesDataStore.tema` flow
2. Validation: Pure Kotlin validation functions in `domain/validation/`, exposed via ViewModel, UI shows inline errors
3. Export: `BackupRepository` uses `MediaStore` + `IS_PENDING=1` → write DB bytes → `IS_PENDING=0`, returns `content://` URI
4. Import: `ACTION_OPEN_DOCUMENT` (mime `application/x-sqlite3`), copy to app databases dir, `Room.databaseBuilder().createFromAsset()` not needed — direct file replace + `ProcessPhoenix.triggerRebirth()`
5. Reset: `AlertDialog` with `TextField` requiring exact "REINICIAR", `RoomDatabase.clearAllTables()` + delete WAL/SHM + selective DataStore clear (keep PIN hash if set)
6. Security: `SecurityRepository` (PBKDF2 100k iterations, 256-bit salt), `BiometricPrompt` with `CryptoObject` (optional), `SecurityGate` composable checks `isUnlocked` flow
7. Feedback: `Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:..."))` with subject/body extras

**Testing:** Strict TDD — each capability gets unit tests (ViewModel, Repository, validation) + compose UI tests for dialogs/gates.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `presentation/theme/Theme.kt` | Modified | Add CompositionLocal + SideEffect to read tema from DataStore |
| `presentation/ajustes/AjustesDataStore.kt` | Modified | Add PIN hash/salt/biometric keys, validation error flows |
| `presentation/ajustes/AjustesViewModel.kt` | Modified | Wire all 7 features, validation logic, security state |
| `presentation/ajustes/AjustesScreen.kt` | Modified | Validation UI, confirmation dialogs, SecurityGate usage |
| `presentation/ajustes/AjustesUiState.kt` | Modified | Add validation error fields, security state, loading states |
| `domain/validation/` | New | Pure validation functions for negocio fields |
| `domain/backup/` | New | BackupRepository interface + MediaStore implementation |
| `domain/security/` | New | SecurityRepository (PIN PBKDF2), BiometricPrompt wrapper, SecurityGate |
| `data/backup/` | New | MediaStoreBackupRepository implementation |
| `data/security/` | New | DataStoreSecurityRepository implementation |
| `di/BackupModule.kt` | New | Hilt module for backup dependencies |
| `di/SecurityModule.kt` | New | Hilt module for security dependencies |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| MediaStore IS_PENDING race on Android 10-11 | Medium | Test on API 29+, fallback to legacy `MediaStore.Images.Media.insertImage` if needed |
| SAF import permissions / URI persistence | Medium | Use `takePersistableUriPermission`, handle `SecurityException` gracefully |
| Room DB file lock during atomic replace | High | Close DB via `RoomDatabase.close()` before replace, use `ProcessPhoenix` for clean restart |
| BiometricPrompt API differences across API levels | Low | Use `androidx.biometric:biometric:1.2.0` unified API, test on API 23+ |
| PBKDF2 performance on low-end devices | Low | 100k iterations ~50ms on modern, acceptable; configurable via BuildConfig |
| Theme flicker on app start before DataStore loads | Medium | Default to SYSTEM in CompositionLocal, apply theme synchronously in `Application.onCreate` |
| DataStore migration for new PIN keys | Low | New keys default to null/false, no migration needed |

## Rollback Plan

1. Revert `Theme.kt` to hardcoded `isSystemInDarkTheme()`
2. Revert `AjustesDataStore.kt` to pre-PIN keys (delete new keys)
3. Remove `domain/validation/`, `domain/backup/`, `domain/security/` packages
4. Remove `data/backup/`, `data/security/` implementations
5. Remove `di/BackupModule.kt`, `di/SecurityModule.kt`
6. Revert `AjustesViewModel`/`AjustesScreen`/`AjustesUiState` to stub implementations
7. All changes are additive or localized — no schema migrations to reverse

## Dependencies

- `androidx.biometric:biometric:1.2.0` (already in build.gradle.kts)
- `androidx.security:security-crypto:1.1.0` (for MasterKeys if needed)
- `com.jakewharton.processphoenix:process-phoenix:2.0.1` (for app restart on import/reset)
- Android 10+ (API 29) for MediaStore IS_PENDING pattern

## Success Criteria

- [ ] Theme changes apply immediately without app restart (SYSTEM/LIGHT/DARK)
- [ ] Validation shows inline errors: nombre required/≤100, teléfono Cuban format (+53 X XXXXXXX), impuesto 0-100 with 2 decimals
- [ ] Export creates `.db` in Downloads, shareable via system share sheet, returns valid content URI
- [ ] Import picks `.db` via system picker, replaces app DB, restarts app, data visible
- [ ] Reset shows typed confirmation ("REINICIAR"), clears all business data, keeps PIN if set
- [ ] PIN setup: 4-6 digits, PBKDF2 hash stored, BiometricPrompt unlocks if enrolled, fallback PIN screen works
- [ ] SecurityGate blocks ventas/inventario/ajustes when locked, unlocks after auth
- [ ] Feedback opens email client with device model, Android version, app version prefilled
- [ ] Unit test coverage ≥80% for new domain/data code
- [ ] Compose UI tests for validation errors, confirmation dialogs, SecurityGate states
- [ ] All existing tests pass

## Effort Estimates

| Task | Effort (Story Points) | Notes |
|------|----------------------|-------|
| Theme Application | 3 | CompositionLocal + SideEffect + ViewModel wiring |
| Validation | 3 | Pure functions + UI error states + ViewModel integration |
| Export Backup | 5 | MediaStore IS_PENDING, byte copy, URI handling, share intent |
| Import Backup | 5 | SAF picker, atomic replace, ProcessPhoenix restart, error handling |
| Reset Data | 3 | Typed dialog, Room clearAllTables, selective DataStore clear |
| PIN + Biometric Security | 8 | PBKDF2, BiometricPrompt, SecurityGate, PIN screen, route integration |
| Feedback (mailto:) | 1 | Simple intent with extras |
| **Total** | **28** | |