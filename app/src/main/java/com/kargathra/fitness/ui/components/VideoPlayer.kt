package com.kargathra.fitness.ui.components

import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Plays a short looping demo clip bundled in res/raw, using the platform
 * VideoView. Muted, autoplay, loops. The frame sizes itself to the video's
 * real aspect ratio once known, so portrait and landscape both fit with no
 * empty bands. Renders nothing if no matching resource exists.
 *
 * Resource lookup: prefer the name derived from [videoUrl]; if that's blank
 * (e.g. hand-authored punch bag exercises), fall back to vid_<fallbackId>.
 */
@Composable
fun ExerciseVideo(
    videoUrl: String,
    fallbackId: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val resName = remember(videoUrl, fallbackId) {
        val fromUrl = rawResNameFor(videoUrl)
        if (fromUrl.isNotEmpty()) fromUrl
        else if (fallbackId.isNotEmpty()) "vid_${sanitiseResName(fallbackId)}"
        else ""
    }
    val resId = remember(resName) {
        if (resName.isEmpty()) 0
        else context.resources.getIdentifier(resName, "raw", context.packageName)
    }

    if (resId == 0) return

    val videoUri = remember(resId) {
        android.net.Uri.parse("android.resource://${context.packageName}/$resId")
    }

    var aspect by remember(resId) { mutableFloatStateOf(9f / 16f) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect),
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(videoUri)
                setOnPreparedListener { mp: MediaPlayer ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    val w = mp.videoWidth
                    val h = mp.videoHeight
                    if (w > 0 && h > 0) aspect = w.toFloat() / h.toFloat()
                    start()
                }
            }
        },
        onRelease = { view -> view.stopPlayback() }
    )
}

/** CDN video URL -> sanitised raw-resource name (e.g. vid_barbell_back_squat). */
fun rawResNameFor(videoUrl: String): String {
    if (videoUrl.isBlank()) return ""
    val file = videoUrl.substringAfterLast('/').substringBeforeLast('.')
    if (file.isBlank()) return ""
    return "vid_${sanitiseResName(file)}"
}

/** Sanitise an arbitrary string into a valid raw-resource name fragment. */
fun sanitiseResName(s: String): String = s
    .lowercase()
    .replace("-", "_")
    .replace(Regex("[^a-z0-9_]"), "_")
    .replace(Regex("_+"), "_")
    .trim('_')
