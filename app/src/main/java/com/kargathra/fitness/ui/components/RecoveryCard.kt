package com.kargathra.fitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.anatomy.GroupRecovery
import com.kargathra.fitness.data.anatomy.MuscleMap
import com.kargathra.fitness.ui.theme.Gold

/**
 * "Recovery" card: whole-body heat map plus the muscles that most need rest.
 * Fresh = rest navy, working = gold, fatigued = ember.
 */
@Composable
fun RecoveryCard(
    recovery: List<GroupRecovery>,
    modifier: Modifier = Modifier
) {
    SectionLabel("Recovery")
    KCard {
        // Fan enum-level recovery out to SVG data-groups for the figure.
        val bySvg = remember(recovery) {
            val m = HashMap<String, Float>()
            recovery.forEach { r ->
                MuscleMap.svgGroupsFor(r.group).forEach { g -> m[g] = r.recovered }
            }
            m
        }

        RecoveryMapView(
            recoveryBySvg = bySvg,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            LegendDot(recoveryColor(0f), "Fresh")
            LegendDot(recoveryColor(0.5f), "Working")
            LegendDot(recoveryColor(1f), "Fatigued")
        }

        // The groups most in need of rest (skip anything essentially fresh).
        val tired = recovery.filter { it.recovered < 0.9f }.take(4)
        if (tired.isEmpty()) {
            Text(
                "All muscle groups recovered — everything is ready to train.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            tired.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        r.group.display,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(r.recovered * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = Gold
                    )
                    Text(
                        if (r.daysToFull <= 0) "" else
                            "  ·  ready in ${r.daysToFull}d",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
