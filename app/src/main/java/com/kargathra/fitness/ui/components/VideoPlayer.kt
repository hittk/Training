package com.kargathra.fitness.ui.components

import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kargathra.fitness.ui.theme.NavyLine

/**
 * Plays a short looping demo clip bundled in res/raw, using the platform
 * VideoView (no extra dependency). Muted, autoplay, loops continuously —
 * suited to 4-second exercise demos. Renders nothing if the resource is absent.
 *
 * @param videoUrl the cached CDN url, e.g. ".../Barbell_Back_Squat.mp4".
 *                 We derive the local raw resource name from its filename.
 */
@Composable
fun ExerciseVideo(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Derive raw resource name from the URL filename:
    // "Barbell_Back_Squat.mp4" -> "vid_barbell_back_squat"
    val resName = remember(videoUrl) { rawResNameFor(videoUrl) }
    val resId = remember(resName) {
        if (resName.isEmpty()) 0
        else context.resources.getIdentifier(resName, "raw", context.packageName)
    }

    if (resId == 0) return  // no bundled video for this exercise

    val videoUri = remember(resId) {
        android.net.Uri.parse("android.resource://${context.packageName}/$resId")
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f),   // portrait clips (496x864)
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(videoUri)
                setOnPreparedListener { mp: MediaPlayer ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)   // muted
                    start()
                }
            }
        },
        onRelease = { view -> view.stopPlayback() }
    )
}

/**
 * Converts a CDN video URL into the sanitised raw-resource name we bundle under.
 * Android raw names must be lowercase, digits/underscore only.
 *   ".../Close-Grip_Barbell_Bench_Press.mp4" -> "vid_close_grip_barbell_bench_press"
 *   ""                                        -> ""
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
