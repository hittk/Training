package com.kargathra.fitness.data.sample

import com.kargathra.fitness.data.model.*

// ---------------------------------------------------------------------------
// Seed content for the foundation so screens render real, equipment-correct
// exercises. NOTE: the user has a flat bench, incline bench, preacher station,
// spin bike and incline treadmill — but NO squat rack — so leg work here uses
// split squats, RDLs, goblet squats and the treadmill rather than back squats.
// The full library (filtered free-exercise-db) replaces this seed later.
// ---------------------------------------------------------------------------

object SampleData {

    val ownedEquipment = listOf(
        Equipment.BARBELL, Equipment.DUMBBELL, Equipment.WEIGHT_PLATE,
        Equipment.MEDICINE_BALL, Equipment.FLAT_BENCH, Equipment.INCLINE_BENCH,
        Equipment.PREACHER_STATION, Equipment.SPIN_BIKE, Equipment.TREADMILL,
        Equipment.BODYWEIGHT
    )

    val exercises = listOf(
        Exercise(
            id = "bb_bench", name = "Barbell Bench Press",
            primary = MuscleGroup.CHEST,
            secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = listOf(Equipment.BARBELL, Equipment.FLAT_BENCH),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PUSH,
            repRange = 6..10,
            cues = listOf(
                "Shoulder blades pinned back and down",
                "Bar travels to lower chest, elbows ~45°",
                "Drive feet into the floor"
            )
        ),
        Exercise(
            id = "incline_db_press", name = "Incline Dumbbell Press",
            primary = MuscleGroup.CHEST,
            secondary = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            equipment = listOf(Equipment.DUMBBELL, Equipment.INCLINE_BENCH),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PUSH,
            cues = listOf("Bench at 30°", "Lower under control to upper chest")
        ),
        Exercise(
            id = "db_row", name = "Single-Arm Dumbbell Row",
            primary = MuscleGroup.LATS,
            secondary = listOf(MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
            equipment = listOf(Equipment.DUMBBELL, Equipment.FLAT_BENCH),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PULL,
            cues = listOf("Drive elbow to hip", "No torso rotation")
        ),
        Exercise(
            id = "db_shoulder_press", name = "Seated Dumbbell Shoulder Press",
            primary = MuscleGroup.SHOULDERS,
            secondary = listOf(MuscleGroup.TRICEPS),
            equipment = listOf(Equipment.DUMBBELL, Equipment.INCLINE_BENCH),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.VERTICAL_PUSH,
            cues = listOf("Bench upright", "Stop at ear height on the way down")
        ),
        Exercise(
            id = "preacher_curl", name = "Barbell Preacher Curl",
            primary = MuscleGroup.BICEPS,
            equipment = listOf(Equipment.PREACHER_STATION, Equipment.BARBELL),
            mechanic = Mechanic.ISOLATION, pattern = Pattern.CURL,
            repRange = 10..14,
            cues = listOf("Upper arms flat on the pad", "Full stretch at the bottom")
        ),
        Exercise(
            id = "db_rdl", name = "Dumbbell Romanian Deadlift",
            primary = MuscleGroup.HAMSTRINGS,
            secondary = listOf(MuscleGroup.GLUTES),
            equipment = listOf(Equipment.DUMBBELL),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.HINGE,
            repRange = 8..12,
            cues = listOf("Hips back, soft knees", "Dumbbells track the shins")
        ),
        Exercise(
            id = "bulgarian_split", name = "Bulgarian Split Squat",
            primary = MuscleGroup.QUADS,
            secondary = listOf(MuscleGroup.GLUTES),
            equipment = listOf(Equipment.DUMBBELL, Equipment.FLAT_BENCH),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.LUNGE,
            cues = listOf("Rear foot on bench", "Front shin roughly vertical")
        ),
        Exercise(
            id = "goblet_squat", name = "Goblet Squat",
            primary = MuscleGroup.QUADS,
            secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
            equipment = listOf(Equipment.DUMBBELL),
            mechanic = Mechanic.COMPOUND, pattern = Pattern.SQUAT,
            cues = listOf("Elbows inside knees at the bottom", "Chest tall")
        ),
        Exercise(
            id = "med_ball_twist", name = "Medicine Ball Russian Twist",
            primary = MuscleGroup.CORE,
            equipment = listOf(Equipment.MEDICINE_BALL),
            mechanic = Mechanic.ISOLATION, pattern = Pattern.CORE,
            repRange = 12..20,
            cues = listOf("Rotate from the ribcage", "Heels light, chest open")
        ),
        Exercise(
            id = "spin_intervals", name = "Spin Bike Intervals",
            primary = MuscleGroup.CARDIO,
            equipment = listOf(Equipment.SPIN_BIKE),
            mechanic = Mechanic.CONDITIONING, pattern = Pattern.CARDIO,
            repRange = 0..0,
            cues = listOf("e.g. 40s hard / 80s easy", "Hold cadence on the work bouts")
        ),
        Exercise(
            id = "treadmill_incline", name = "Incline Treadmill Walk",
            primary = MuscleGroup.CARDIO,
            secondary = listOf(MuscleGroup.GLUTES),
            equipment = listOf(Equipment.TREADMILL),
            mechanic = Mechanic.CONDITIONING, pattern = Pattern.CARDIO,
            repRange = 0..0,
            cues = listOf("Incline 8–12%", "Don't hold the rails")
        )
    )

    /** One worked preset so the Routines tab isn't empty. */
    val upperBodyDay = Routine(
        id = "upper_a",
        title = "Upper Body A",
        focus = listOf(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS),
        estimatedMinutes = 52,
        items = listOf(
            RoutineItem(exercises[0], sets = 4, repTarget = 6..8, restSeconds = 150),
            RoutineItem(exercises[2], sets = 4, repTarget = 8..10, restSeconds = 120),
            RoutineItem(exercises[3], sets = 3, repTarget = 8..12, restSeconds = 120),
            RoutineItem(exercises[4], sets = 3, repTarget = 10..14, restSeconds = 90)
        )
    )

    val starterProgram = Program(
        id = "ppl_lite",
        title = "Upper / Lower (Home)",
        summary = "Two-day rotation built around your bench, dumbbells and preacher station — no rack required.",
        daysPerWeek = 4,
        days = listOf(upperBodyDay)
    )
}
