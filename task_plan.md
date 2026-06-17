# FocusGuard Skeleton Plan

## Goal

Build the initial Android project skeleton from `docs/FocusGuard项目方案.md`: Compose app shell, navigation, layered packages, domain models, repository contracts, Room/DataStore/Hilt/WorkManager-ready structure, and starter UI screens.

## Context

- User is a junior software engineering student focusing on Android with Kotlin + Compose.
- Project should be useful in daily life and interview-friendly, emphasizing audio, sensors, foreground service, local persistence, Flow, permissions, and performance.
- Current repo is a fresh Compose Android project with only default `MainActivity`, theme, Gradle files, and docs.

## Phases

### Phase 1: Inspect Current Project
Status: complete

- Read project docs and old thread context.
- Inspect Gradle catalog, app module, manifest, and current MainActivity.

### Phase 2: Configure Build Skeleton
Status: complete

- Add Android/Kotlin/Hilt/KSP plugins as needed.
- Add Navigation Compose, lifecycle-compose, ViewModel Compose, Room, DataStore, WorkManager, and Hilt dependencies.
- Keep versions conservative and consistent with the existing catalog.

### Phase 3: Create Layered Packages
Status: complete

- Add `data`, `domain`, `service`, `ui`, and `di` package skeletons.
- Add Room entities/DAO/database.
- Add repository contracts and placeholder implementations.
- Add app-level Hilt module and Application class.

### Phase 4: Compose App Shell
Status: complete

- Replace default greeting with FocusGuard app shell.
- Add destination model, bottom navigation, and starter screens for Today, Session, Reports, Settings, plus detail/permission/onboarding placeholders.
- Add reusable UI components and starter state models.

### Phase 5: Verify
Status: complete

- Run Gradle compile or build task.
- Fix compile issues if they appear.
- Record final files and verification result.

## Decisions

- Build the skeleton around the final MD plan, not the earlier BLE-first idea.
- Include domain layer now because the project is intended as a second portfolio project and interview explanation benefits from clear boundaries.
- Use placeholder/mock repository data initially so UI can run before sensors/audio/Room behavior is fully implemented.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---|---|
| GitHub curated skill listing returned HTTP 403 | Tried `list-skills.py --format json` | Used local skill directories instead |
| `read_thread` rejected first calls | Passed optional args in a shape tool rejected | Retried with all fields populated; succeeded |
| Gradle wrapper timed out in sandbox | Ran `:app:assembleDebug` | Retried with approved escalation so Gradle distribution could download |
| AGP 9 built-in Kotlin rejected kapt/Hilt plugin combo | Tried normal Hilt+kapt setup | Switched project to AGP 8.13.2, Kotlin Android plugin, KSP |
| AndroidX dependency check failed | `android.useAndroidX` missing | Added `android.useAndroidX=true` to `gradle.properties` |
| Kotlin/Javac target mismatch | `compileDebugKotlin` used JVM 21 while Java used 11 | Set Kotlin `jvmTarget = "11"` |
| Deprecated Kotlin target DSL warning | Used `kotlinOptions` | Migrated to `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }` |

## Verification

- `./gradlew.bat :app:assembleDebug` passed on 2026-06-16.
- Build still reports third-party/plugin deprecation notices about future Gradle 10 compatibility, but no project compile errors remain.
