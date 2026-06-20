package com.kargathra.fitness.data.model

// ---------------------------------------------------------------------------
// Domain model. Kept deliberately small for the foundation; the routine
// generator, progression logic and Health Connect mapping all build on these.
// ---------------------------------------------------------------------------

/** Equipment the user actually owns. The generator and filters key off this. */
enum class Equipment(val display: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbells"),
    WEIGHT_PLATE("Weight plates"),
    MEDICINE_BALL("Medicine ball"),
    FLAT_BENCH("Flat bench"),
    INCLINE_BENCH("Incline bench"),
    PREACHER_STATION("Preacher curl station"),
    SPIN_BIKE("Spin bike"),
    TREADMILL("Incline treadmill"),
    BODYWEIGHT("Bodyweight")
}

/** Primary trainable regions used for "focus on this area" routing. */
enum class MuscleGroup(val display: String) {
    CHEST("Chest"),
    UPPER_BACK("Upper back"),
    LATS("Lats"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    CARDIO("Conditioning")
}

enum class Mechanic { COMPOUND, ISOLATION, CONDITIONING }

/** Coarse movement category — lets the generator balance a session. */
enum class Pattern { HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL,
    SQUAT, HINGE, LUNGE, CURL, EXTENSION, CORE, CARDIO }

data class Exercise(
    val id: String,
    val name: String,
    val primary: MuscleGroup,
    val secondary: List<MuscleGroup> = emptyList(),
    val equipment: List<Equipment>,
    val mechanic: Mechanic,
    val pattern: Pattern,
    /** Default working rep range for a hypertrophy emphasis. */
    val repRange: IntRange = 8..12,
    /** Short, plain-language cues for good form. */
    val cues: List<String> = emptyList(),
    /** Slug used to resolve an illustration from the bundled DB later. */
    val illustrationKey: String? = null
)

/** A planned movement inside a routine (target sets/reps), before logging. */
data class RoutineItem(
    val exercise: Exercise,
    val sets: Int,
    val repTarget: IntRange,
    val restSeconds: Int
)

data class Routine(
    val id: String,
    val title: String,
    val focus: List<MuscleGroup>,
    val items: List<RoutineItem>,
    val estimatedMinutes: Int
)

/** A multi-day structured plan the user can pick from (presets). */
data class Program(
    val id: String,
    val title: String,
    val summary: String,
    val daysPerWeek: Int,
    val days: List<Routine>
)

enum class Goal(val display: String) {
    HYPERTROPHY("Muscle growth"),
    STRENGTH("Strength"),
    GENERAL("General fitness")
}

enum class Experience(val display: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}
