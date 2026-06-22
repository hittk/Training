package com.kargathra.fitness.data.api

data class FetchBatch(
    val category: String,
    val equipment: String,
    val mechanic: String,
    val level: String,
    val slotLimit: Int,
    val requiresPunchBag: Boolean = false
)

object ExerciseFetchPlan {

    val batches = listOf(
        // Barbell strength: 120 slots
        FetchBatch("strength", "barbell",      "compound",  "", 80),
        FetchBatch("strength", "barbell",      "isolation", "", 40),
        // Dumbbell strength: 160 slots
        FetchBatch("strength", "dumbbell",     "compound",  "", 100),
        FetchBatch("strength", "dumbbell",     "isolation", "", 60),
        // Bodyweight strength: 80 slots
        FetchBatch("strength", "body only",    "",          "", 80),
        // Kettlebell: 40 slots
        FetchBatch("strength", "kettlebell",   "compound",  "", 30),
        FetchBatch("strength", "kettlebell",   "isolation", "", 10),
        // Conditioning: 50 slots
        FetchBatch("conditioning", "",         "",          "", 50),
        // Stretching: 30 slots
        FetchBatch("stretching", "body only",  "",          "", 30),
        // Punch bag: 20 slots (hidden until owned)
        FetchBatch("conditioning", "punching bag", "",      "", 20, requiresPunchBag = true)
    )

    val totalSlots: Int get() = batches.sumOf { it.slotLimit }
}
