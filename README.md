# Kargathra — Training

A native Android (Kotlin / Jetpack Compose) strength-and-conditioning app with
on-device Google Health Connect sync. Navy-and-gold identity, legibility-first.

> **Status: 0.1.0 — foundation build.** Runnable, branded, navigable shell with
> the data model, equipment-aware seed content, bundled illustrations, the
> Health Connect layer, **set logging (weight × reps) with a Room database**, and
> **native strength-trend charts**. The routine generator lands on top of this.

## Build

Open the project root in Android Studio (Narwhal / 2025.3+), let it sync, and
run on the Pixel 8 Pro. No `local.properties` is committed — Studio writes the
SDK path on first sync, and it will fetch the Gradle wrapper itself.

Pinned, coherent toolchain (chosen for first-compile reliability, not bleeding
edge):

- Android Gradle Plugin 8.7.3 · Gradle 8.11.1 · Kotlin 2.1.0
- Compose BOM 2024.12.01 · Material 3
- `androidx.health.connect:connect-client:1.1.0` (stable)
- minSdk 30 · target/compile SDK 35

## Health Connect

All training data is written **locally** to Health Connect on the device —
no account, no cloud, no telemetry. Permissions are declared in the manifest
and requested at runtime via `HealthConnectManager`:

- `WRITE_EXERCISE` / `READ_EXERCISE` — strength + cardio sessions
- `WRITE_DISTANCE` — app-logged cardio distance
- `READ_HEART_RATE`, `READ_ACTIVE_CALORIES_BURNED` — **read-only**, sourced
  from the user's Fitbit. The app never writes HR/calories, so the Fitbit stays
  the single source of truth and there is no duplicate data. `readSessionVitals()`
  pulls avg/max HR over a logged session window.

`PermissionsRationaleActivity` satisfies Health Connect's required
"why does this app need access" screen (both framework and legacy intents).

## Layout

```
app/src/main/java/com/kargathra/fitness/
  MainActivity.kt                  HC availability + permission flow, theme host
  ui/
    KargathraApp.kt                Scaffold: top bar, bottom nav, NavHost
    theme/                         Color · Type · Theme (navy/gold dark scheme)
    navigation/Destinations.kt     Today · Train · Exercises · Progress (+Settings)
    components/Common.kt           KCard, SectionLabel, Tag
    screens/                       Today, Routines, Exercises, Progress, Settings
  data/
    model/Models.kt                Equipment, MuscleGroup, Exercise, Routine, Program
    sample/SampleData.kt           Equipment-aware seed (no squat rack assumed)
  health/
    HealthConnectManager.kt        availability, permissions, write strength/cardio
    PermissionsRationaleActivity.kt
```

## Illustrations

Bundled under `app/src/main/assets/exercises/<id>/{0,1}.jpg` from
[`free-exercise-db`](https://github.com/yuhonas/free-exercise-db) (**The Unlicense**,
public domain). Only the frames for the seeded exercises are included (~1.6 MB).
Frame 0 (start) is shown; frame 1 (end) ships for a future start/end toggle.

## Equipment assumed

Barbell, dumbbells, plates, medicine ball, flat bench, incline bench, preacher
curl station, spin bike, incline treadmill. **No squat rack** — so leg work
uses split squats, RDLs, goblet squats and the treadmill rather than back squats.

## Roadmap (next sessions)

1. **Routine generator** — target areas + days/week + time → balanced,
   pattern-aware session.
3. **Set logging** — per-set weight/reps, rest timer, write sessions (with
   `ExerciseSegment` reps) to Health Connect.
4. **Progression** — working-weight / volume / est-1RM trends, add-load prompts.
5. **Branding polish** — launcher mark, optional display serif for headings.
