# Architecture

## Current State

Screens currently talk to repositories **directly**. Each composable holds the
repository (or a flow from it) and collects state inline via
`collectAsStateWithLifecycle()`, doing any derived calculations (volume totals,
1RM estimates, filtering) in the composable itself.

This works and keeps the app simple, but it couples UI to data access: state
survival across configuration changes leans on Compose `remember`/`rememberSaveable`
rather than a lifecycle-scoped owner, business logic is hard to unit-test in
isolation, and the same derived calculations can end up duplicated across screens.

## Planned: ViewModel Extraction

Introduce a `ViewModel` per screen (the `lifecycle-viewmodel-compose` dependency
is already on the classpath). Each ViewModel would own:

- **State** — expose UI state as `StateFlow`, collected in the composable with
  `collectAsStateWithLifecycle()`; survives configuration changes.
- **Repository calls** — all reads/writes move behind the ViewModel; the
  composable no longer holds a repository reference.
- **Derived calculations** — volume, estimated-1RM, filtering, and prescription
  shaping move out of the composable into testable functions.

Screens that benefit most (roughly in priority order):

1. `ProgressScreen` — trend aggregation and Epley 1RM math.
2. `LogWorkoutScreen` — set-logging state, rest-timer, overload suggestions.
3. `ExercisesScreen` — multi-filter and variation-navigation state.
4. `ProgramsScreen` — saved-routine CRUD.

## Migration Approach

Do this **one screen at a time**, running `./gradlew assembleDebug` (and the unit
tests) between each screen so a regression is always isolated to the last change.
Start with `ProgressScreen`, since its logic is the most self-contained.
