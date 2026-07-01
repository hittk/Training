package com.kargathra.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.kargathra.fitness.data.anatomy.MuscleMap
import com.kargathra.fitness.data.repo.MuscleVolume
import com.kargathra.fitness.ui.theme.Gold
import com.kargathra.fitness.ui.theme.NavyLine

/**
 * Horizontal bar chart showing weekly volume (kg) per muscle group.
 * Drawn entirely with Canvas — no chart library required.
 */
@Composable
fun MuscleVolumeChart(
    volumes: List<MuscleVolume>,
    modifier: Modifier = Modifier
) {
    if (volumes.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val surfaceVar   = MaterialTheme.colorScheme.onSurfaceVariant

    KCard(modifier) {
        SectionLabel("This week — muscle volume")
        Spacer(Modifier.height(14.dp))

        // Visual: shade the body by relative volume worked this week.
        val engagement = remember(volumes) {
            MuscleMap.engagementFromGroupVolumes(volumes.map { it.group to it.volumeKg })
        }
        if (engagement.isNotEmpty()) {
            MuscleMapView(
                engagement = engagement,
                showFront  = MuscleMap.needsFront(engagement.keys),
                showBack   = MuscleMap.needsBack(engagement.keys),
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
        }

        val barHeight    = 22.dp
        val rowSpacing   = 10.dp
        val labelWidth   = 96.dp
        val totalHeight  = (barHeight + rowSpacing) * volumes.size

        val maxVol = volumes.maxOf { it.volumeKg }.takeIf { it > 0.0 } ?: 1.0

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            val labelPx  = labelWidth.toPx()
            val valuePx  = 52.dp.toPx()
            val barAreaW = size.width - labelPx - valuePx - 8.dp.toPx()
            val rowH     = (barHeight + rowSpacing).toPx()
            val barH     = barHeight.toPx()

            volumes.forEachIndexed { i, mv ->
                val y      = i * rowH
                val barW   = (mv.volumeKg / maxVol * barAreaW).toFloat().coerceAtLeast(4f)
                val barY   = y + (barH * 0.1f)
                val barHActual = barH * 0.8f

                // Label
                val labelLayout = textMeasurer.measure(
                    text  = mv.group.display,
                    style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = surfaceVar)
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(0f, y + (barH - labelLayout.size.height) / 2f)
                )

                // Track
                drawRoundRect(
                    color        = NavyLine,
                    topLeft      = Offset(labelPx, barY),
                    size         = Size(barAreaW, barHActual),
                    cornerRadius = CornerRadius(barHActual / 2)
                )

                // Fill
                drawRoundRect(
                    color        = Gold,
                    topLeft      = Offset(labelPx, barY),
                    size         = Size(barW, barHActual),
                    cornerRadius = CornerRadius(barHActual / 2)
                )

                // Value label
                val volLabel = "${mv.volumeKg.toLong()} kg"
                val valLayout = textMeasurer.measure(
                    text  = volLabel,
                    style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = onSurface)
                )
                drawText(
                    textLayoutResult = valLayout,
                    topLeft = Offset(
                        x = labelPx + barAreaW + 8.dp.toPx(),
                        y = y + (barH - valLayout.size.height) / 2f
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Primary muscles full credit · secondary muscles 50%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
