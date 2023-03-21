package com.tangem.tap.common.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.sdk.extensions.dpToPx
import com.tangem.sdk.extensions.pxToDp

/**
 * Created by Anton Zhilenkov on 08/06/2022.
 */
@Composable
fun Painter.dpSize(): DpSize = DpSize(
    intrinsicSize.width.pxToDp().dp,
    intrinsicSize.height.pxToDp().dp,
)

@Composable
private fun Float.dpToPx(): Float = LocalContext.current.dpToPx(this)

@Composable
private fun Float.pxToDp(): Float = LocalContext.current.pxToDp(this)
