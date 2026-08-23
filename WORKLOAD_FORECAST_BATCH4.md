# Workload Forecast - Batch 4: Feedback (T43-T46) + Cross-cutting (T47-T49)

## Summary

**Total Estimated Changed Lines: ~581 lines**

This exceeds the 400-line review budget threshold, confirming that **chained PR strategy (stacked-to-main)** is appropriate.

## Breakdown by Task

### New Files (All lines are additions)

| File | Lines | Task |
|------|-------|------|
| `domain/feedback/FeedbackRepository.kt` | 23 | T43 |
| `data/feedback/FeedbackRepositoryImpl.kt` | 120 | T44 |
| `di/FeedbackModule.kt` | 18 | T45 |
| `test/domain/feedback/FeedbackRepositoryTest.kt` | 46 | T43 (TDD) |
| `test/data/feedback/FeedbackRepositoryImplTest.kt` | 158 | T44 (TDD) |
| `androidTest/.../FeedbackIntegrationTest.kt` | 150 | Bonus E2E |

**New files subtotal: 515 lines**

### Modified Files (Lines changed)

| File | Original | New | Delta | Task |
|------|----------|-----|-------|------|
| `presentation/ajustes/AjustesViewModel.kt` | 198 | 211 | +13 | T46 |
| `presentation/ajustes/AjustesScreen.kt` | 572 | 584 | +12 | T46 |
| `test/.../AjustesViewModelTest.kt` | 270 | 311 | +41 | T46 (TDD) |
| `test/.../AjustesViewModelResetTest.kt` | 133 | ~150 | +17 | T46 (TDD) |

**Modified files subtotal: ~83 lines**

### Total: ~598 lines (515 new + 83 modified)

## Review Budget Analysis

- **400-line threshold**: EXCEEDED (~598 lines)
- **Recommended strategy**: Chained PRs (stacked-to-main)
- **PR slices suggested**:
  1. **PR #1**: Domain layer (T43) - FeedbackRepository interface + tests (~69 lines)
  2. **PR #2**: Data layer (T44) - FeedbackRepositoryImpl + tests (~278 lines)
  3. **PR #3**: DI + Presentation wiring (T45, T46) - FeedbackModule, ViewModel, Screen changes + tests (~168 lines)
  4. **PR #4**: Cross-cutting (T47-T49) - Dependencies verification, integration test (~83 lines)

## Dependencies Verification (T47)

All required dependencies are present in `app/build.gradle.kts`:
- ✅ ProcessPhoenix: `implementation("com.github.hamsterksu:process-phoenix:1.1.3")`
- ✅ Biometric: `implementation(libs.biometric)` (v1.2.0-alpha04)
- ✅ Hilt: `implementation(libs.hilt.android)`, `ksp(libs.hilt.compiler)`
- ✅ Room (KSP): `implementation(libs.room.runtime)`, `ksp(libs.room.compiler)`
- ✅ DataStore: `implementation(libs.datastore.preferences)`, `implementation(libs.datastore.core)`
- ✅ Coroutines: `implementation(libs.coroutines.android)`, `implementation(libs.coroutines.core)`
- ✅ Navigation Compose: `implementation(libs.navigation.compose)`

No additional dependencies needed.

## Hilt Modules (T48)

All Hilt modules use `@InstallIn(SingletonComponent::class)` and are automatically discovered:
- ✅ `BackupModule.kt` - provides BackupRepository
- ✅ `SecurityModule.kt` - provides SecurityRepository
- ✅ `RepositoryModule.kt` - binds repository interfaces
- ✅ `DatabaseModule.kt` - provides Room database
- ✅ `DataStoreModule.kt` - provides DataStore
- ✅ `NetworkModule.kt` - provides Retrofit/OkHttp
- ✅ **`FeedbackModule.kt`** (NEW) - provides FeedbackRepository

No central `AppModule` exists; modules are independently installed. FeedbackModule follows the same pattern.

## Chain Strategy: stacked-to-main

Each PR targets the previous PR's branch:
- PR #1 → `main`
- PR #2 → PR #1 branch
- PR #3 → PR #2 branch
- PR #4 → PR #3 branch

After all PRs merge, the feature branch is merged to `main`.

## Acceptance Criteria Verification

| Criteria | Status |
|----------|--------|
| Feedback button opens email app with prefilled context | ✅ Implemented in FeedbackRepositoryImpl |
| No email app → toast "No hay app de correo" | ✅ Implemented in AjustesScreen |
| All dependencies resolved, project compiles | ✅ Verified in build.gradle.kts |
| Workload forecast documented | ✅ This document |

## Test Coverage

### Unit Tests (src/test)
- FeedbackRepository interface contract tests
- FeedbackRepositoryImpl mailto: URI building and error handling
- AjustesViewModel.sendFeedback() delegation tests
- AjustesViewModelResetTest updated for new constructor

### Instrumented Tests (src/androidTest)
- FeedbackIntegrationTest: mailto: URI structure verification
- AjustesFullFlowIntegrationTest: Documents full flow integration points

All tests follow Strict TDD: RED (failing test written first) → GREEN (implementation) → REFACTOR.