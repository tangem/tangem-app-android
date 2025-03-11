package com.tangem.tap.common.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

/**
[REDACTED_AUTHOR]
 */
@Composable
fun Dp.toPx(): Float {
    val currentDp = this
    return with(LocalDensity.current) { currentDp.toPx() }
}

fun DpSize.halfHeight(): Dp = this.height / 2