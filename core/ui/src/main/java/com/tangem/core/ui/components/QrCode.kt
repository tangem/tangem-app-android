package com.tangem.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.toQrCode

@Composable
fun rememberQrPainter(content: String, size: Dp = 248.dp, padding: Dp = 0.dp): BitmapPainter {
    val density = LocalDensity.current
    return remember(content) {
        BitmapPainter(
            content.toQrCode(
                sizePx = with(density) { size.roundToPx() },
                paddingPx = with(density) { padding.roundToPx() },
            ).asImageBitmap(),
        )
    }
}