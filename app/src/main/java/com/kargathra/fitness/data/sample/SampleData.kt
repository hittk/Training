package com.kargathra.fitness.data.sample

import com.kargathra.fitness.data.model.*

object SampleData {

    val ownedEquipment = listOf(
        Equipment.BARBELL, Equipment.DUMBBELL, Equipment.WEIGHT_PLATE,
        Equipment.MEDICINE_BALL, Equipment.FLAT_BENCH, Equipment.INCLINE_BENCH,
        Equipment.PREACHER_STATION, Equipment.SPIN_BIKE, Equipment.TREADMILL,
        Equipment.BODYWEIGHT
    )

    // ── Exercise library ──────────────────────────────────────────────────────

    val bbBench = Exercise(
        id = "bb_bench", name = "Barbell Bench Press",
        primary = MuscleGroup.CHEST,
        secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
        equipment = listOf(Equipment.BARBELL, Equipment.FLAT_BENCH),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PUSH,
        repRange = 6..10,
        cues = listOf("Shoulder blades pinned back and down",
            "Bar to lower chest, elbows ~45°", "Drive feet into the floor")
    )
    val inclineDbPress = Exercise(
        id = "incline_db_press", name = "Incline Dumbbell Press",
        primary = MuscleGroup.CHEST,
        secondary = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
        equipment = listOf(Equipment.DUMBBELL, Equipment.INCLINE_BENCH),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PUSH,
        cues = listOf("Bench at 30°", "Lower under control to upper chest")
    )
    val dbRow = Exercise(
        id = "db_row", name = "Single-Arm Dumbbell Row",
        primary = MuscleGroup.LATS,
        secondary = listOf(MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
        equipment = listOf(Equipment.DUMBBELL, Equipment.FLAT_BENCH),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PULL,
        cues = listOf("Drive elbow to hip", "No torso rotation")
    )
    val dbShoulderPress = Exercise(
        id = "db_shoulder_press", name = "Seated Dumbbell Shoulder Press",
        primary = MuscleGroup.SHOULDERS,
        secondary = listOf(MuscleGroup.TRICEPS),
        equipment = listOf(Equipment.DUMBBELL, Equipment.INCLINE_BENCH),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.VERTICAL_PUSH,
        cues = listOf("Bench upright", "Stop at ear height on the way down")
    )
    val preacherCurl = Exercise(
        id = "preacher_curl", name = "Barbell Preacher Curl",
        primary = MuscleGroup.BICEPS,
        equipment = listOf(Equipment.PREACHER_STATION, Equipment.BARBELL),
        mechanic = Mechanic.ISOLATION, pattern = Pattern.CURL,
        repRange = 10..14,
        cues = listOf("Upper arms flat on pad", "Full stretch at the bottom")
    )
    val dbRdl = Exercise(
        id = "db_rdl", name = "Dumbbell Romanian Deadlift",
        primary = MuscleGroup.HAMSTRINGS,
        secondary = listOf(MuscleGroup.GLUTES),
        equipment = listOf(Equipment.DUMBBELL),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.HINGE,
        repRange = 8..12,
        cues = listOf("Hips back, soft knees", "Dumbbells track the shins")
    )
    val bulgarianSplit = Exercise(
        id = "bulgarian_split", name = "Bulgarian Split Squat",
        primary = MuscleGroup.QUADS,
        secondary = listOf(MuscleGroup.GLUTES),
        equipment = listOf(Equipment.DUMBBELL, Equipment.FLAT_BENCH),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.LUNGE,
        cues = listOf("Rear foot on bench", "Front shin roughly vertical")
    )
    val gobletSquat = Exercise(
        id = "goblet_squat", name = "Goblet Squat",
        primary = MuscleGroup.QUADS,
        secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
        equipment = listOf(Equipment.DUMBBELL),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.SQUAT,
        cues = listOf("Elbows inside knees at the bottom", "Chest tall")
    )
    val medBallTwist = Exercise(
        id = "med_ball_twist", name = "Medicine Ball Russian Twist",
        primary = MuscleGroup.CORE,
        equipment = listOf(Equipment.MEDICINE_BALL),
        mechanic = Mechanic.ISOLATION, pattern = Pattern.CORE,
        repRange = 12..20,
        cues = listOf("Rotate from the ribcage", "Heels light, chest open")
    )
    val spinIntervals = Exercise(
        id = "spin_intervals", name = "Spin Bike Intervals",
        primary = MuscleGroup.CARDIO,
        equipment = listOf(Equipment.SPIN_BIKE),
        mechanic = Mechanic.CONDITIONING, pattern = Pattern.CARDIO,
        repRange = 0..0,
        cues = listOf("40s hard / 80s easy", "Hold cadence on the work bouts")
    )
    val treadmillIncline = Exercise(
        id = "treadmill_incline", name = "Incline Treadmill Walk",
        primary = MuscleGroup.CARDIO,
        secondary = listOf(MuscleGroup.GLUTES),
        equipment = listOf(Equipment.TREADMILL),
        mechanic = Mechanic.CONDITIONING, pattern = Pattern.CARDIO,
        repRange = 0..0,
        cues = listOf("Incline 8–12%", "Don't hold the rails")
    )
    val closeGripPress = Exercise(
        id = "close_grip_press", name = "Close-Grip Barbell Press",
        primary = MuscleGroup.TRICEPS,
        secondary = listOf(MuscleGroup.CHEST),
        equipment = listOf(Equipment.BARBELL, Equipment.FLAT_BENCH),
        mechanic = Mechanic.COMPOUND, pattern = Pattern.HORIZONTAL_PUSH,
        repRange = 8..12,
        cues = listOf("Hands shoulder-width", "Elbows tucked")
    )
    val dbLateralRaise = Exercise(
        id = "db_lateral", name = "Dumbbell Lateral Raise",
        primary = MuscleGroup.SHOULDERS,
        equipment = listOf(Equipment.DUMBBELL),
        mechanic = Mechanic.ISOLATION, pattern = Pattern.VERTICAL_PUSH,
        repRange = 12..20,
        cues = listOf("Lead with the elbows", "Slight forward lean")
    )
    val dbCurl = Exercise(
        id = "db_curl", name = "Dumbbell Curl",
        primary = MuscleGroup.BICEPS,
        equipment = listOf(Equipment.DUMBBELL),
        mechanic = Mechanic.ISOLATION, pattern = Pattern.CURL,
        repRange = 10..15,
        cues = listOf("Supinate at the top", "No shoulder swinging")
    )
    val dbTricepKickback = Exercise(
        id = "db_kickback", name = "Dumbbell Tricep Kickback",
        primary = MuscleGroup.TRICEPS,
        equipment = listOf(Equipment.DUMBBELL, Equipment.FLAT_BENCH),
        mechanic = Mechanic.ISOLATION, pattern = Pattern.EXTENSION,
        repRange = 12..15,
        cues = listOf("Upper arm parallel to floor", "Squeeze at lockout")
    )
    val calfRaise = Exercise(
        id = "calf_raise", name = "Standing Calf Raise",
        primary = MuscleGroup.CALVES,
        equipment = listOf(Equipment.BODYWEIGHT),
        mechanic = Mechanic.ISOLATION, pattern = Pattern.SQUAT,
        repRange = 15..25,
        cues = listOf("Full range — heel below platform", "Pause at top")
    )

    // ── Routines ──────────────────────────────────────────────────────────────

    val upperBodyDay = Routine(
        id = "upper_a",
        title = "Upper Body A",
        focus = listOf(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS),
        estimatedMinutes = 52,
        items = listOf(
            RoutineItem(bbBench,          sets = 4, repTarget = 6..8,   restSeconds = 150),
            RoutineItem(dbRow,            sets = 4, repTarget = 8..10,  restSeconds = 120),
            RoutineItem(dbShoulderPress,  sets = 3, repTarget = 8..12,  restSeconds = 120),
            RoutineItem(preacherCurl,     sets = 3, repTarget = 10..14, restSeconds = 90)
        )
    )

    val upperBodyB = Routine(
        id = "upper_b",
        title = "Upper Body B",
        focus = listOf(MuscleGroup.CHEST, MuscleGroup.UPPER_BACK, MuscleGroup.TRICEPS, MuscleGroup.BICEPS),
        estimatedMinutes = 50,
        items = listOf(
            RoutineItem(inclineDbPress,    sets = 4, repTarget = 8..12,  restSeconds = 120),
            RoutineItem(dbRow,             sets = 4, repTarget = 8..10,  restSeconds = 120),
            RoutineItem(closeGripPress,    sets = 3, repTarget = 8..12,  restSeconds = 90),
            RoutineItem(dbLateralRaise,    sets = 3, repTarget = 12..20, restSeconds = 60),
            RoutineItem(dbCurl,            sets = 3, repTarget = 10..15, restSeconds = 60)
        )
    )

    val lowerBodyDay = Routine(
        id = "lower_a",
        title = "Lower Body",
        focus = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
        estimatedMinutes = 48,
        items = listOf(
            RoutineItem(gobletSquat,    sets = 4, repTarget = 10..15, restSeconds = 120),
            RoutineItem(dbRdl,          sets = 4, repTarget = 8..12,  restSeconds = 120),
            RoutineItem(bulgarianSplit, sets = 3, repTarget = 10..12, restSeconds = 90),
            RoutineItem(calfRaise,      sets = 3, repTarget = 15..25, restSeconds = 60)
        )
    )

    val conditioningDay = Routine(
        id = "conditioning_a",
        title = "Conditioning",
        focus = listOf(MuscleGroup.CARDIO, MuscleGroup.CORE),
        estimatedMinutes = 35,
        items = listOf(
            RoutineItem(spinIntervals,    sets = 6, repTarget = 0..0,  restSeconds = 80),
            RoutineItem(medBallTwist,     sets = 3, repTarget = 12..20, restSeconds = 60),
            RoutineItem(treadmillIncline, sets = 1, repTarget = 0..0,  restSeconds = 0)
        )
    )

    val armBlast = Routine(
        id = "arm_blast",
        title = "Arm Blast",
        focus = listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
        estimatedMinutes = 40,
        items = listOf(
            RoutineItem(preacherCurl,     sets = 4, repTarget = 10..14, restSeconds = 90),
            RoutineItem(closeGripPress,   sets = 4, repTarget = 8..12,  restSeconds = 90),
            RoutineItem(dbCurl,           sets = 3, repTarget = 10..15, restSeconds = 60),
            RoutineItem(dbTricepKickback, sets = 3, repTarget = 12..15, restSeconds = 60)
        )
    )

    // ── Programs ──────────────────────────────────────────────────────────────

    val upperLowerProgram = Program(
        id = "upper_lower",
        title = "Upper / Lower Split",
        summary = "Classic 4-day split alternating upper and lower sessions — no rack required.",
        daysPerWeek = 4,
        days = listOf(upperBodyDay, lowerBodyDay, upperBodyB, lowerBodyDay)
    )

    val fullBodyProgram = Program(
        id = "full_body_3",
        title = "Full Body ×3",
        summary = "Three total-body sessions per week with rotating emphasis. Good for beginners.",
        daysPerWeek = 3,
        days = listOf(upperBodyDay, lowerBodyDay, upperBodyB)
    )

    val conditioningFocus = Program(
        id = "conditioning_focus",
        title = "Conditioning Focus",
        summary = "Strength twice a week, conditioning twice a week. Keeps cardio front and centre.",
        daysPerWeek = 4,
        days = listOf(upperBodyDay, conditioningDay, lowerBodyDay, conditioningDay)
    )

    /** All programs shown on the Programs screen */
    val allPrograms = listOf(upperLowerProgram, fullBodyProgram, conditioningFocus)

    /** All standalone routines (for the AI generator result and preset browser) */
    val allRoutines = listOf(upperBodyDay, upperBodyB, lowerBodyDay, conditioningDay, armBlast)

    /** Lookup a routine by id — used when loading a preset into the Workout page */
    fun routineById(id: String): Routine? = allRoutines.firstOrNull { it.id == id }

    /** Legacy single-program reference kept for compatibility */
    val starterProgram = upperLowerProgram

    /** Flat list of every exercise in the library — used by the Exercises tab. */
    val exercises: List<Exercise> by lazy {
        allRoutines.flatMap { it.items.map { i -> i.exercise } }
            .distinctBy { it.id }
            .sortedBy { it.name }
    }
}
