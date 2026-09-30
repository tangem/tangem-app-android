package com.tangem.features.storiesv2.impl.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf

/**
 * The position inside the current slide, keyed by the play token of the slide it belongs to.
 *
 * The frame loop and the screen hear of a slide change by different routes: the position is a snapshot state the very
 * next draw sees, while the slide reaches the screen through the UI model a frame later. Read by the token of the slide
 * the screen is drawing, a slide the player has already left keeps the position it ended at for that frame instead of
 * dropping to zero — which showed as a finished segment, or a finished Lottie, blinking empty at the hand-over.
 */
@Stable
internal class SlidePosition {

    private val playToken = mutableIntStateOf(NO_TOKEN)
    private val positionMs = mutableLongStateOf(0L)
    private var leftSlidePositionMs = 0L

    /** The position of the slide entered last, whatever slide the screen is drawing. */
    val currentMs: Long get() = positionMs.longValue

    fun update(positionMs: Long) {
        this.positionMs.longValue = positionMs
    }

    fun enter(playToken: Int) {
        leftSlidePositionMs = positionMs.longValue
        positionMs.longValue = 0L
        this.playToken.intValue = playToken
    }

    fun of(playToken: Int): Long {
        return if (playToken == this.playToken.intValue) positionMs.longValue else leftSlidePositionMs
    }

    private companion object {
        const val NO_TOKEN = -1
    }
}