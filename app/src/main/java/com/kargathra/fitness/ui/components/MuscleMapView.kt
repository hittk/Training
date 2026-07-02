package com.kargathra.fitness.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.R
import com.kargathra.fitness.data.anatomy.Engagement
import com.kargathra.fitness.ui.theme.Gold
import org.json.JSONObject

/** A parsed muscle path: which view + group it belongs to, plus its Compose Path. */
private data class MusclePath(val view: String, val group: String, val path: Path)

private data class MuscleData(
    val vbWidth: Float,
    val vbHeight: Float,
    val frontMinX: Float,
    val frontMaxX: Float,
    val backMinX: Float,
    val backMaxX: Float,
    val paths: List<MusclePath>
)

private const val SPLIT_X = 810f  // x below = front figure, above = back figure

@Composable
fun MuscleMapView(
    engagement: Map<String, Engagement>,   // data-group -> engagement
    showFront: Boolean,
    showBack: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val data = remember { loadMuscleData(context) }

    val primaryColor   = Gold
    val secondaryColor = Gold.copy(alpha = 0.45f)
    val restColor      = Color(0xFF26405A)
    val outlineColor   = Color(0xFF16283A)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        val fillFor: (String) -> Color = { group ->
            when (engagement[group] ?: Engagement.NONE) {
                Engagement.PRIMARY   -> primaryColor
                Engagement.SECONDARY -> secondaryColor
                Engagement.NONE      -> restColor
            }
        }
        if (showFront) {
            MuscleFigure(
                data, "front", data.frontMinX, data.frontMaxX,
                fillFor, outlineColor, Modifier.weight(1f)
            )
        }
        if (showBack) {
            MuscleFigure(
                data, "back", data.backMinX, data.backMaxX,
                fillFor, outlineColor, Modifier.weight(1f)
            )
        }
    }
}

/**
 * Whole-body recovery heat map. [recoveryBySvg] maps SVG data-group ->
 * recovered fraction (0 = fully fatigued, 1 = fresh). Groups absent from the
 * map render as rest (never trained recently = fresh).
 *
 * Colour ramp stays on brand: fresh muscles sit at the rest navy, working
 * fatigue warms through gold, heavy fatigue glows ember red.
 */
@Composable
fun RecoveryMapView(
    recoveryBySvg: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val data = remember { loadMuscleData(context) }

    val outlineColor = Color(0xFF16283A)
    val fillFor: (String) -> Color = { group ->
        recoveryColor(1f - (recoveryBySvg[group] ?: 1f))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        MuscleFigure(
            data, "front", data.frontMinX, data.frontMaxX,
            fillFor, outlineColor, Modifier.weight(1f)
        )
        MuscleFigure(
            data, "back", data.backMinX, data.backMaxX,
            fillFor, outlineColor, Modifier.weight(1f)
        )
    }
}

private val RestNavy  = Color(0xFF26405A)
private val EmberRed  = Color(0xFFC24141)

/** fatigue 0 -> rest navy, 0.5 -> gold, 1 -> ember red. */
fun recoveryColor(fatigue: Float): Color {
    val f = fatigue.coerceIn(0f, 1f)
    return if (f <= 0.5f) lerpColor(RestNavy, Gold, f / 0.5f)
    else lerpColor(Gold, EmberRed, (f - 0.5f) / 0.5f)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = 1f
)

@Composable
private fun MuscleFigure(
    data: MuscleData,
    view: String,
    minX: Float,
    maxX: Float,
    fillFor: (String) -> Color,
    outline: Color,
    modifier: Modifier = Modifier
) {
    val figW = maxX - minX
    val figH = data.vbHeight
    val aspect = figW / figH

    Canvas(modifier = modifier.aspectRatio(aspect)) {
        val sx = size.width / figW
        val sy = size.height / figH
        val s = minOf(sx, sy)
        // centre
        val offX = (size.width - figW * s) / 2f - minX * s
        val offY = (size.height - figH * s) / 2f

        translate(offX, offY) {
            scale(s, s, pivot = Offset.Zero) {
                for (mp in data.paths) {
                    if (mp.view != view) continue
                    drawPath(mp.path, color = fillFor(mp.group))
                    drawPath(mp.path, color = outline, style = Stroke(width = 1.2f / s))
                }
            }
        }
    }
}

// ── Loading & parsing ──────────────────────────────────────────────────────

private fun loadMuscleData(context: Context): MuscleData {
    val raw = context.resources.openRawResource(R.raw.muscle_paths)
        .bufferedReader().use { it.readText() }
    val root = JSONObject(raw)
    val vb = root.getString("viewBox").trim().split(Regex("\\s+")).map { it.toFloat() }
    val arr = root.getJSONArray("paths")

    val paths = ArrayList<MusclePath>(arr.length())
    var frontMinX = Float.MAX_VALUE; var frontMaxX = Float.MIN_VALUE
    var backMinX  = Float.MAX_VALUE; var backMaxX  = Float.MIN_VALUE

    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val view = o.getString("view")
        val group = o.getString("group")
        val d = o.getString("d")
        val path = parseSvgPath(d)
        paths += MusclePath(view, group, path)

        // track per-view x bounds from the path data
        val bounds = pathBounds(d)
        if (view == "front") {
            frontMinX = minOf(frontMinX, bounds.first); frontMaxX = maxOf(frontMaxX, bounds.second)
        } else {
            backMinX = minOf(backMinX, bounds.first); backMaxX = maxOf(backMaxX, bounds.second)
        }
    }

    return MuscleData(
        vbWidth = vb[2], vbHeight = vb[3],
        frontMinX = frontMinX, frontMaxX = frontMaxX,
        backMinX = backMinX, backMaxX = backMaxX,
        paths = paths
    )
}

private fun pathBounds(d: String): Pair<Float, Float> {
    val nums = Regex("-?[\\d.]+").findAll(d).map { it.value.toFloat() }.toList()
    var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
    var idx = 0
    while (idx + 1 < nums.size) {
        val x = nums[idx]
        minX = minOf(minX, x); maxX = maxOf(maxX, x)
        idx += 2
    }
    return minX to maxX
}

/**
 * Minimal SVG path parser supporting the commands present in this asset:
 * M/m (move), L/l (line), Z/z (close). The asset uses absolute M/L and Z only.
 */
private fun parseSvgPath(d: String): Path {
    val path = Path()
    val tokens = Regex("[MmLlZz]|-?[\\d.]+").findAll(d).map { it.value }.toList()
    var i = 0
    var cmd = ""
    var curX = 0f; var curY = 0f
    fun num(): Float = tokens[i++].toFloat()
    while (i < tokens.size) {
        val t = tokens[i]
        if (t.matches(Regex("[MmLlZz]"))) { cmd = t; i++ }
        when (cmd) {
            "M" -> { curX = num(); curY = num(); path.moveTo(curX, curY); cmd = "L" }
            "m" -> { curX += num(); curY += num(); path.moveTo(curX, curY); cmd = "l" }
            "L" -> { curX = num(); curY = num(); path.lineTo(curX, curY) }
            "l" -> { curX += num(); curY += num(); path.lineTo(curX, curY) }
            "Z", "z" -> { path.close(); i++ }
            else -> i++
        }
    }
    return path
}
