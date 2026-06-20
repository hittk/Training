package com.kargathra.fitness.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Outlined.Today),
    ROUTINES("routines", "Train", Icons.Outlined.FitnessCenter),
    EXERCISES("exercises", "Exercises", Icons.Outlined.GridView),
    PROGRESS("progress", "Progress", Icons.Outlined.BarChart);

    companion object {
        val bottomBar = listOf(TODAY, ROUTINES, EXERCISES, PROGRESS)
    }
}

const val SETTINGS_ROUTE = "settings"
