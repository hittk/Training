package com.kargathra.fitness.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    WORKOUT("workout", "Workout", Icons.Outlined.PlayArrow),
    PROGRAMS("programs", "Programs", Icons.Outlined.GridView),
    EXERCISES("exercises", "Exercises", Icons.Outlined.FitnessCenter),
    PROGRESS("progress", "Progress", Icons.Outlined.BarChart);

    companion object {
        val bottomBar = listOf(WORKOUT, PROGRAMS, EXERCISES, PROGRESS)
    }
}

const val SETTINGS_ROUTE = "settings"
