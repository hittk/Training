package com.kargathra.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.ui.theme.Gold
import com.kargathra.fitness.ui.theme.NavyLine

/**
 * Bespoke navy/gold line chart drawn with Canvas — no chart dependency.
 * Renders a trend across sessions with a soft gold fill, the line, dots,
 * and an emphasised latest point.
 */
@Composable
fun TrendChart(
    title: String,
    values: List<Float>,
    latestLabel: String,
    footer: String,
    modifier: Modifier = Modifier
) {
    KCard(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                SectionLabel(title)
                Text(
                    latestLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (values.isEmpty()) return@Canvas
            val maxV = values.max()
            val minV = values.min()
            val range = (maxV - minV).takeIf { it > 0f } ?: 1f
            val padTop = 12f
            val padBottom = 12f
            val usableH = size.height - padTop - padBottom
            val n = values.size
            val stepX = if (n > 1) size.width / (n - 1) else 0f

            fun pt(i: Int): Offset {
                val x = if (n > 1) stepX * i else size.width / 2f
                val norm = (values[i] - minV) / range
                val y = padTop + (1f - norm) * usableH
                return Offset(x, y)
            }

            // baseline
            drawLine(
                color = NavyLine,
                start = Offset(0f, size.height - padBottom),
                end = Offset(size.width, size.height - padBottom),
                strokeWidth = 2f
            )

            if (n == 1) {
                drawCircle(Gold, radius = 9f, center = pt(0))
                return@Canvas
            }

            // soft fill under the line
            val fill = Path().apply {
                moveTo(0f, size.height - padBottom)
                for (i in 0 until n) lineTo(pt(i).x, pt(i).y)
                lineTo(size.width, size.height - padBottom)
                close()
            }
            drawPath(fill, color = Gold.copy(alpha = 0.12f))

            // line
            val line = Path().apply {
                moveTo(pt(0).x, pt(0).y)
                for (i in 1 until n) lineTo(pt(i).x, pt(i).y)
            }
            drawPath(line, color = Gold, style = Stroke(width = 5f))

            // dots, last one emphasised
            for (i in 0 until n) {
                val emphasised = i == n - 1
                drawCircle(Gold, radius = if (emphasised) 9f else 5f, center = pt(i))
                if (emphasised) drawCircle(
                    Gold.copy(alpha = 0.25f), radius = 18f, center = pt(i)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            footer,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
