# FocusGuard Findings

## Project Brief

FocusGuard is a Kotlin + Jetpack Compose Android app for focus and environment monitoring. It should record focus duration, noise, light, phone movement, app usage/distraction context, and generate reports.

## Target Technical Depth

- SensorManager for light and accelerometer.
- AudioRecord for relative environmental noise.
- Foreground Service and notifications for background monitoring.
- Room for focus sessions and samples.
- DataStore for settings.
- WorkManager for daily reports.
- StateFlow/SharedFlow and Repository boundaries.
- Compose Canvas/chart performance, avoiding full-page recomposition from high-frequency samples.

## Current Repository State

- Fresh Android app project named `FocusGuard`.
- Package: `com.ywb.focusguard`.
- Existing files include default MainActivity, theme, launcher assets, Gradle wrapper, and `docs/FocusGuard项目方案.md`.
- Current app module dependencies only include basic Compose, Activity Compose, Material3, Core KTX, and lifecycle runtime.

## User Context From Prior Thread

- User is a junior software engineering student and Android/Kotlin+Compose learner.
- Internship work uses AI heavily, but user wants deeper technical understanding.
- First project covered accessibility/notification-based accounting and some app architecture.
- Second project should be useful in daily life and demonstrable to interviewers.
- User can spend around two hours per evening and wants portfolio value around August.

## Implementation Notes

- The initial skeleton should compile before real sensor/audio work lands.
- Real sensor and AudioRecord implementations can be added in later phases behind data source interfaces.
- Permission prompts should be non-blocking and explanatory; do not ask all permissions at launch.
