package com.kargathra.fitness.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.ui.theme.Gold
import com.kargathra.fitness.ui.theme.NavyElevated
import kotlin.math.roundToInt

/** Available plate sizes (kg), heaviest first. Each is per-plate weight. */
private val PLATE_SIZES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

/**
 * Given a target total weight and the bar weight, work out how many of each
 * plate go on EACH side. Returns pairs of (plate, countPerSide) plus any
 * remainder that can't be matched exactly.
 */
data class PlateResult(
    val perSide: List<Pair<Double, Int>>,
    val leftover: Double  // kg per side that couldn't be matched
)

fun computePlates(target: Double, bar: Double, plates: List<Double>): PlateResult {
    val perSideTarget = (target - bar) / 2.0
    if (perSideTarget <= 0.0) return PlateResult(emptyList(), 0.0)
    var remaining = perSideTarget
    val out = ArrayList<Pair<Double, Int>>()
    for (p in plates.sortedDescending()) {
        val n = (remaining / p).toInt()
        if (n > 0) {
            out += p to n
            remaining -= n * p
        }
    }
    // round leftover to avoid float dust
    val leftover = (remaining * 100).roundToInt() / 100.0
    return PlateResult(out, leftover)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorCard(modifier: Modifier = Modifier) {
    var barText    by remember { mutableStateOf("10") }
    var targetText by remember { mutableStateOf("40") }

    val bar    = barText.toDoubleOrNull() ?: 0.0
    val target = targetText.toDoubleOrNull() ?: 0.0
    val result = remember(bar, target) { computePlates(target, bar, PLATE_SIZES) }

    KCard(modifier = modifier) {
        SectionLabel("Plate calculator")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Target (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = barText,
                onValueChange = { barText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Bar (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        when {
            target <= 0.0 || bar <= 0.0 -> {
                Text(
                    "Enter a target and bar weight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            target < bar -> {
                Text(
                    "Target is below the bar weight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            result.perSide.isEmpty() -> {
                Text(
                    "Just the bar — no plates needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Text(
                    "Per side:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRowPlates(result.perSide)
                if (result.leftover > 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "≈ ${fmtKg(result.leftover)} kg per side can't be matched with your plates.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(10.dp))
                val loaded = bar + result.perSide.sumOf { it.first * it.second } * 2
                Text(
                    "Loaded total: ${fmtKg(loaded)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowPlates(perSide: List<Pair<Double, Int>>) {
    // simple wrapping row of plate chips
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        perSide.forEach { (plate, count) ->
            Surface(
                color = NavyElevated,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "${count} × ${fmtKg(plate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun fmtKg(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
