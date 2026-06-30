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
 * VideoView (no extra dependency). Muted, autoplay, loops continuously.
 *
 * The frame sizes itself to the video's real aspect ratio once known, so
 * portrait clips stay tall and landscape clips stay wide — no empty bands.
 * Renders nothing if the resource is absent.
 */
@Composable
fun ExerciseVideo(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val resName = remember(videoUrl) { rawResNameFor(videoUrl) }
    val resId = remember(resName) {
        if (resName.isEmpty()) 0
        else context.resources.getIdentifier(resName, "raw", context.packageName)
    }

    if (resId == 0) return  // no bundled video for this exercise

    val videoUri = remember(resId) {
        android.net.Uri.parse("android.resource://${context.packageName}/$resId")
    }

    // Default to portrait until the real ratio is known; updated on prepare.
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
                    mp.setVolume(0f, 0f)   // muted
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

/**
 * Converts a CDN video URL into the sanitised raw-resource name we bundle under.
 *   ".../Close-Grip_Barbell_Bench_Press.mp4" -> "vid_close_grip_barbell_bench_press"
 */
fun rawResNameFor(videoUrl: String): String {
    if (videoUrl.isBlank()) return ""
    val file = videoUrl.substringAfterLast('/').substringBeforeLast('.')
    if (file.isBlank()) return ""
    val sanitised = file
        .lowercase()
        .replace("-", "_")
        .replace(Regex("[^a-z0-9_]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
    return "vid_$sanitised"
}
