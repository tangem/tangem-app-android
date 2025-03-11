package com.tangem.tap.common.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.sdk.extensions.pxToDp

/**
[REDACTED_AUTHOR]
 */
@Composable
fun Painter.dpSize(): DpSize = DpSize(
    intrinsicSize.width.pxToDp().dp,
    intrinsicSize.height.pxToDp().dp,
)

@Composable
private fun Float.pxToDp(): Float = LocalContext.current.pxToDp(this)