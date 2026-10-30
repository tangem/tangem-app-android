package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst

internal object PackLaunchMotion {

    private val DIP_X = (-5).dp
    private val DIP_Y = 17.dp
    private const val DIP_SCALE = 1.01f

    /** The top leans toward the viewer during the dip. */
    private const val DIP_ROTATION_X = -4f

    private val LAUNCH_X = 45.dp
    private val LAUNCH_Y = (-45).dp
    private const val LAUNCH_SCALE = 0.84f

    /** Top and right edge go away from the viewer; most of the visible tilt is perspective. */
    private const val LAUNCH_ROTATION_X = 35f
    private const val LAUNCH_ROTATION_Y = 16f
    private const val LAUNCH_ROTATION_Z = 3f

    /** Perspective the demo angles were measured against, in pack widths. */
    private const val PERSPECTIVE_OF_PACK_WIDTH = 3.36f

    private val CONTENT_SHIFT = 36.dp

    /** The button is gone within this share of the exit; the rows start at [ROW_DELAY]. */
    private const val BUTTON_SPAN = 0.25f
    private const val ROW_DELAY = 0.23f

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

    fun rotationX(progress: ConfirmProgress): Float = lerp(
        start = lerp(0f, DIP_ROTATION_X, progress.dip),
        stop = LAUNCH_ROTATION_X,
        fraction = progress.launch,
    )

    fun rotationY(progress: ConfirmProgress): Float = LAUNCH_ROTATION_Y * progress.launch

    fun rotationZ(progress: ConfirmProgress): Float = LAUNCH_ROTATION_Z * progress.launch

    fun alpha(progress: ConfirmProgress): Float = if (progress.launch >= 1f) 0f else 1f

    fun cameraDistance(packWidthPx: Float): Float =
        packWidthPx * PERSPECTIVE_OF_PACK_WIDTH / PackOpeningConst.CAMERA_PX_PER_INCH

    fun contentShift(exit: Float, isDelayed: Boolean): Dp = lerp(0.dp, CONTENT_SHIFT, contentProgress(exit, isDelayed))

    fun contentAlpha(exit: Float, isDelayed: Boolean): Float = 1f - contentProgress(exit, isDelayed)

    private fun contentProgress(exit: Float, isDelayed: Boolean): Float {
        if (!isDelayed) return (exit / BUTTON_SPAN).coerceIn(0f, 1f)

        return ((exit - ROW_DELAY) / (1f - ROW_DELAY)).coerceIn(0f, 1f)
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

    private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction
}

internal data class ConfirmProgress(
    val dip: Float,
    val launch: Float,
    val exit: Float,
) {

    companion object {

        val Idle = ConfirmProgress(dip = 0f, launch = 0f, exit = 0f)
    }
}