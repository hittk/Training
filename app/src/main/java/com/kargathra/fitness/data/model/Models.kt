package com.kargathra.fitness.data.model

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
    BODYWEIGHT("Bodyweight"),
    KETTLEBELL("Kettlebell"),
    MACHINE("Machine"),
    OTHER("Other")
}

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

enum class Pattern {
    HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL,
    SQUAT, HINGE, LUNGE, CURL, EXTENSION, CORE, CARDIO
}

data class Exercise(
    val id: String,
    val name: String,
    val primary: MuscleGroup,
    val secondary: List<MuscleGroup> = emptyList(),
    val equipment: List<Equipment>,
    val mechanic: Mechanic,
    val pattern: Pattern,
    val repRange: IntRange = 8..12,
    val cues: List<String> = emptyList(),
    val illustrationKey: String? = null
)

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
    val estimatedMinutes: Int,
    /** True for AI-generated routines shown in the AI result flow */
    val isGenerated: Boolean = false
)

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
    FAT_LOSS("Fat loss"),
    GENERAL("General fitness")
}

enum class Experience(val display: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}
