package com.tangem.core.ui.windowsize

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

/**
 * The preferred way to determine the window size is because it provides the actual size of the window as opposed to
 * [LocalConfiguration.current.screenHeightDp]
 *
 * @property width actual window width
 * @property height actual window height
 * @property widthSizeType window size type based on width
 * @property heightSizeType window size type based on height
 * @property smallestSize smallest size of width and height
 */
data class WindowSize(
    val width: Dp,
    val height: Dp,
) {
    val widthSizeType: WindowSizeType = getWidthWindowSizeType(width)
    val heightSizeType: WindowSizeType = getHeightWindowSizeType(height)
    val smallestSize: Dp = minOf(width, height)

    fun widthAtLeast(type: WindowSizeType): Boolean = this.widthSizeType.ordinal >= type.ordinal

    fun widthAtLeast(value: Dp): Boolean = this.width >= value

    fun heightAtLeast(type: WindowSizeType): Boolean = this.heightSizeType.ordinal >= type.ordinal

    fun heightAtLeast(value: Dp): Boolean = this.height >= value

    fun widthAtMost(type: WindowSizeType): Boolean = this.widthSizeType.ordinal <= type.ordinal

    fun widthAtMost(value: Dp): Boolean = this.width <= value

    fun heightAtMost(type: WindowSizeType): Boolean = this.heightSizeType.ordinal <= type.ordinal

    fun heightAtMost(value: Dp): Boolean = this.height <= value

    private fun getWidthWindowSizeType(windowDp: Dp): WindowSizeType = when {
        windowDp <= 320.dp -> WindowSizeType.ExtraSmall
        windowDp <= 360.dp -> WindowSizeType.Small
        windowDp <= 540.dp -> WindowSizeType.Normal
        windowDp <= 700.dp -> WindowSizeType.Large
        else -> WindowSizeType.ExtraLarge
    }

    private fun getHeightWindowSizeType(heightDp: Dp): WindowSizeType = when {
        heightDp <= 480.dp -> WindowSizeType.ExtraSmall
        heightDp <= 640.dp -> WindowSizeType.Small
        heightDp <= 860.dp -> WindowSizeType.Normal
        heightDp <= 1100.dp -> WindowSizeType.Large
        else -> WindowSizeType.ExtraLarge
    }
}

enum class WindowSizeType {
    ExtraSmall, Small, Normal, Large, ExtraLarge
}

@Composable
fun rememberWindowSize(activity: Activity): WindowSize {
    val windowBoundsSize = rememberWindowBoundsSize(activity)
    val windowDpSize = with(LocalDensity.current) {
        windowBoundsSize.toDpSize()
    }

    return WindowSize(
        width = windowDpSize.width,
        height = windowDpSize.height,
    )
}

@Composable
internal fun rememberWindowSizePreview(width: Dp, height: Dp): WindowSize {
    return remember(width, height) {
        WindowSize(
            width = width,
            height = height,
        )
    }
}

@Composable
private fun rememberWindowBoundsSize(activity: Activity): Size {
    val configuration = LocalConfiguration.current
    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    }
    return windowMetrics.bounds.toComposeRect().size
}