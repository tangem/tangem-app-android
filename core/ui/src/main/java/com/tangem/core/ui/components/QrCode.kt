package com.tangem.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.extensions.toQrCode
import com.tangem.core.ui.res.TangemTheme

@Composable
fun rememberQrPainters(
    content: List<String>,
    size: Dp = TangemTheme.dimens.size248,
    padding: Dp = TangemTheme.dimens.spacing0,
): List<BitmapPainter> {
    val density = LocalDensity.current
    return remember(content) {
        content.map { code ->
            BitmapPainter(
                code.toQrCode(
                    sizePx = with(density) { size.roundToPx() },
                    paddingPx = with(density) { padding.roundToPx() },
                ).asImageBitmap(),
            )
        }
    }
}