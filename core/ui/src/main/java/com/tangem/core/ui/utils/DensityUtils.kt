package com.tangem.core.ui.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Stable
@Composable
fun Dp.toPx(): Float = toPx(density = LocalDensity.current.density)

@Stable
@Composable
fun convertPxToDp(px: Float): Dp = convertPxToDp(px, density = LocalDensity.current.density)

fun Dp.toPx(density: Float): Float = this.value * density

fun convertPxToDp(px: Float, density: Float): Dp = Dp(value = px / density)

fun Context.dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
fun Context.pxToDp(px: Float): Float = (px / resources.displayMetrics.density).roundToInt().toFloat()

@Composable
fun Painter.dpSize(): DpSize = DpSize(
    intrinsicSize.width.pxToDp().dp,
    intrinsicSize.height.pxToDp().dp,
)

@Composable
private fun Float.pxToDp(): Float = LocalContext.current.pxToDp(this)