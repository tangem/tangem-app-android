package com.tangem.features.storiesv2.impl.ui

import android.view.SurfaceView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.LongState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

/**
 * The parts of the player the screen has to reach imperatively: the surface the decoder draws into, and the frame
 * pulse that keeps progress in step with it.
 *
 * [slidePositionMs] is a Compose state read while drawing rather than a value in the UI model, so a bar moving at
 * display refresh rate never recomposes anything — sixty recompositions a second of a full-screen story is the kind
 * of load that makes the video itself stutter. A position and not a fraction because a Lottie slide needs the same
 * number in milliseconds.
 */
@Stable
internal interface StoryPlayerHandle {

    val slidePositionMs: LongState

    /** Whether any slide needs a video surface at all. */
    val hasVideo: Boolean

    /** Called once per displayed frame, with the Choreographer frame time. */
    fun onFrame(frameTimeNanos: Long)

    fun attachSurface(surfaceView: SurfaceView)

    fun detachSurface(surfaceView: SurfaceView)

    /**
     * A poster decoded once per story, off the main thread. `painterResource` re-decodes a full-screen bitmap inside
     * the very frame that switches the slide, which was most of the hitch a tap navigation used to have; null until
     * the prefetch lands, where the screen falls back to that synchronous decode.
     */
    fun posterPainter(@DrawableRes id: Int): Painter?
}