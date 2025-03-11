package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Stable
@Composable
fun Dp.toPx(): Float = toPx(density = LocalDensity.current.density)

@Stable
@Composable
fun convertPxToDp(px: Float): Dp = convertPxToDp(px, density = LocalDensity.current.density)

fun Dp.toPx(density: Float): Float = this.value * density

fun convertPxToDp(px: Float, density: Float): Dp = Dp(value = px / density)