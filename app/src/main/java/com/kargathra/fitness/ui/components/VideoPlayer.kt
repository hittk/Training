package com.kargathra.fitness.ui.components

import android.media.MediaPlayer
import android.net.Uri
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
import com.kargathra.fitness.data.video.UserVideoStore

/**
 * Plays a short looping demo clip, muted, auto-sizing to the video's real
 * aspect ratio. Resolution order:
 *   1. user-added video in internal storage (vid uploaded in-app)
 *   2. resource derived from [videoUrl]
 *   3. bundled vid_<fallbackId>
 * Renders nothing if none exist.
 *
 * [refreshKey] can be changed by the caller to force re-resolution after a
 * user uploads a new video.
 */
@Composable
fun ExerciseVideo(
    videoUrl: String,
    fallbackId: String = "",
    refreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1. user video?
    val userUri: Uri? = remember(fallbackId, refreshKey) {
        if (fallbackId.isNotEmpty() && UserVideoStore.has(context, fallbackId))
            Uri.fromFile(UserVideoStore.fileFor(context, fallbackId))
        else null
    }

    // 2/3. bundled resource fallback
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

    val videoUri: Uri? = remember(userUri, resId) {
        userUri ?: if (resId != 0)
            Uri.parse("android.resource://${context.packageName}/$resId")
        else null
    }

    if (videoUri == null) return

    var aspect by remember(videoUri) { mutableFloatStateOf(9f / 16f) }

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
        update = { view ->
            // Only re-point if the source changed (avoids restart on every recompose)
            if (view.tag != videoUri) {
                view.tag = videoUri
                view.setVideoURI(videoUri)
                view.start()
            }
        },
        onRelease = { view -> view.stopPlayback() }
    )
}

/** True if any video exists for this exercise (user, url, or bundled). */
@Composable
fun hasAnyVideo(videoUrl: String, exerciseId: String, refreshKey: Int = 0): Boolean {
    val context = LocalContext.current
    return remember(videoUrl, exerciseId, refreshKey) {
        if (exerciseId.isNotEmpty() && UserVideoStore.has(context, exerciseId)) return@remember true
        val resName = rawResNameFor(videoUrl).ifEmpty {
            if (exerciseId.isNotEmpty()) "vid_${sanitiseResName(exerciseId)}" else ""
        }
        if (resName.isEmpty()) false
        else context.resources.getIdentifier(resName, "raw", context.packageName) != 0
    }
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
