# Kargathra — Training

A native Android (Kotlin / Jetpack Compose) strength-and-conditioning app with
on-device Google Health Connect sync. Navy-and-gold identity, legibility-first.
Fully local: no account, no cloud APIs, no keys in the APK.

> **Status: 0.2.10 — feature build.** A working, navigable app with the full
> local data model, a 471-exercise on-device library, set logging to a Room
> database, native strength-trend charts, muscle-anatomy visualisation, and an
> on-device routine generator.

## Features

- **On-device routine generator** — 3-pass, compound-first algorithm
  (`LocalRoutineGenerator`) that filters to owned equipment, covers target
  muscle groups, de-duplicates movement families and assigns goal-based
  prescriptions. No network, deterministic given the same inputs.
- **Exercise browser** — multi-filter (mechanic, equipment, body region,
  favourites) over the 471-exercise cache, with per-exercise variation
  navigation.
- **Set logging** — per-set weight × reps written to a Room database, with an
  animated rest-timer countdown banner during logging.
- **Progressive overload hints** — suggests +2.5 kg once your reps reach the top
  of the target range.
- **Plate calculator** — greedy per-side plate breakdown for a target load.
- **Strength trends** — total-volume and estimated-1RM (Epley) charts, drawn on
  a native Compose Canvas (no charting library).
- **Muscle anatomy visualisation** — front/back SVG body maps with
  primary/secondary engagement shading.
- **Weekly muscle-volume chart** — native Compose Canvas bar chart, no external
  library.
- **Per-exercise demo video** — bundled clips plus user-uploaded custom videos,
  stored in internal app storage.
- **Session summary** — post-workout muscle-engagement anatomy map with a
  per-muscle volume breakdown.
- **Saved programs** — full CRUD: create, rename, delete, and remove individual
  exercises.

## Build

Open the project root in Android Studio (Narwhal / 2025.3+), let it sync, and
run on the Pixel 8 Pro. No `local.properties` is committed — Studio writes the
SDK path on first sync, and it will fetch the Gradle wrapper itself. CI builds a
debug APK on every push to `main` (see `.github/workflows/android-build.yml`).

Pinned, coherent toolchain (chosen for first-compile reliability, not bleeding
edge):

- Android Gradle Plugin 8.9.2 · Gradle 8.11.1 · Kotlin 2.1.0 · KSP 2.1.0-1.0.29
- Compose BOM 2024.12.01 · Material 3
- Room 2.6.1 (local persistence for logged sets, favourites, saved routines)
- `androidx.health.connect:connect-client:1.1.0` (stable)
- minSdk 30 · target/compile SDK 36

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
    navigation/Destinations.kt     Workout · Programs · Exercises · Progress (+Settings)
    components/                     Common, PlateCalculator, TrendChart,
                                    MuscleMapView, MuscleVolumeChart, VideoPlayer
    screens/                       Today, Workout, Programs, Exercises, Progress,
                                    LogWorkout, SessionSummary, WorkoutGenerator, Settings
  data/
    model/Models.kt                Equipment, MuscleGroup (12), Exercise, Routine, Program, Goal
    generator/LocalRoutineGenerator.kt   3-pass on-device routine builder
    db/                            Room entities + DAOs (exercises, workouts, favourites, routines)
    repo/                          Exercise / Workout / Favourite / SavedRoutine repositories
    anatomy/                       MuscleMap + engagement model
    video/UserVideoStore.kt        user-uploaded demo video storage
    sample/SampleData.kt           Equipment-aware seed (no squat rack assumed)
  health/
    HealthConnectManager.kt        availability, permissions, write strength/cardio
    PermissionsRationaleActivity.kt
```

## Muscle groups & equipment

The app models **12 muscle groups**: Chest, Upper back, Lats, Shoulders,
Biceps, Triceps, Quads, Hamstrings, Glutes, Calves, Core and Conditioning.

Equipment assumed: Barbell, Dumbbells, Weight plates, Medicine ball, Flat bench,
**Incline bench**, **Preacher curl station**, Spin bike, Incline treadmill (plus
Bodyweight). **No squat rack** — so leg work uses split squats, RDLs, goblet
squats and the treadmill rather than back squats.

## Illustrations

Bundled under `app/src/main/assets/exercises/<id>/{0,1}.jpg` from
[`free-exercise-db`](https://github.com/yuhonas/free-exercise-db) (**The Unlicense**,
public domain). Only the frames for the seeded exercises are included (~1.6 MB).
Frame 0 (start) is shown; frame 1 (end) ships for a future start/end toggle.

## Roadmap (next sessions)

1. **ViewModel extraction** — move repository access and derived state out of
   screens into ViewModels (see `ARCHITECTURE.md`).
2. **Unit test coverage** — broaden beyond the plate calculator and routine
   generator to repositories and prescription edge cases.
3. **Cardio session logging** — richer interval/distance capture and Health
   Connect write-back improvements.
