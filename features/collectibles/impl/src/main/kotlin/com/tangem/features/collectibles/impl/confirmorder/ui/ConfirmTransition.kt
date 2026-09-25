package com.tangem.features.collectibles.impl.confirmorder.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The pack's own proportion on the confirm screen (Figma `1607:41492`, 230 x 332 on a 402-wide frame). */
internal const val PACK_WIDTH_FRACTION = 230f / 402f
internal const val PACK_ASPECT = 332f / 230f

/** The pack's centre above the screen's, on the 874-tall frame. */
internal val PACK_LIFT = 85.dp

/**
 * The pack dips down-left, then is thrown up-right, tilting and fading; the rest of the screen drops away.
 * Pure maths so the beat hand-over and the screen bounds are unit-tested.
 */
internal object PackLaunch {

    private val DIP_X = (-16).dp
    private val DIP_Y = 12.dp
    private const val DIP_SCALE = 0.94f

    // The largest round offsets that keep the pack (x 86..316, y 186..518 on the 402 x 874 frame) on screen
    private val LAUNCH_X = 44.dp
    private val LAUNCH_Y = (-72).dp
    private const val LAUNCH_SCALE = 1.06f

    private const val LAUNCH_ROTATION = 7f

    private const val FADE_GATE = 0.4f

    private val CONTENT_SHIFT = 120.dp

    /** How much later the price rows leave than the button, as a fraction of the exit. */
    private const val ROW_DELAY = 0.28f

    fun offsetX(progress: ConfirmProgress): Dp = lerp(
        start = lerp(0.dp, DIP_X, progress.dip),
        stop = LAUNCH_X,
        fraction = progress.launch,
    )

    fun offsetY(progress: ConfirmProgress): Dp = lerp(
        start = lerp(0.dp, DIP_Y, progress.dip),
        stop = LAUNCH_Y,
        fraction = progress.launch,
    )

    fun scale(progress: ConfirmProgress): Float = lerp(
        start = lerp(1f, DIP_SCALE, progress.dip),
        stop = LAUNCH_SCALE,
        fraction = progress.launch,
    )

    fun rotation(progress: ConfirmProgress): Float = LAUNCH_ROTATION * progress.launch

    /** Solid until [FADE_GATE] of the throw, fully transparent at its end. */
    fun alpha(progress: ConfirmProgress): Float {
        if (progress.launch <= FADE_GATE) return 1f

        return 1f - (progress.launch - FADE_GATE) / (1f - FADE_GATE)
    }

    /** [isDelayed] is the price rows, which lag the button by [ROW_DELAY]. */
    fun contentShift(exit: Float, isDelayed: Boolean): Dp = lerp(0.dp, CONTENT_SHIFT, contentProgress(exit, isDelayed))

    fun contentAlpha(exit: Float, isDelayed: Boolean): Float = 1f - contentProgress(exit, isDelayed)

    private fun contentProgress(exit: Float, isDelayed: Boolean): Float {
        if (!isDelayed) return exit.coerceIn(0f, 1f)

        return ((exit - ROW_DELAY) / (1f - ROW_DELAY)).coerceIn(0f, 1f)
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

    private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction
}

/** Three clocks because the beats overlap: [dip] then [launch] on the pack, [exit] alongside both. */
internal data class ConfirmProgress(
    val dip: Float,
    val launch: Float,
    val exit: Float,
) {

    companion object {

        val Idle = ConfirmProgress(dip = 0f, launch = 0f, exit = 0f)
    }
}

internal object ConfirmTiming {

    const val DIP_MS = 260
    const val LAUNCH_MS = 460
    const val EXIT_MS = 320
}