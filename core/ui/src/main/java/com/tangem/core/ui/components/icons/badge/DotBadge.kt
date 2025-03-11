package com.tangem.core.ui.components.icons.badge

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemColorPalette

fun DrawScope.drawBadge(containerColor: Color, offset: Dp = 2.dp) {
    val width = size.width
    drawCircle(
        color = containerColor,
        center = Offset(x = width - offset.toPx(), y = offset.toPx()),
        radius = 5.dp.toPx(),
    )
    drawCircle(
        color = TangemColorPalette.Azure,
        center = Offset(x = width - offset.toPx(), y = offset.toPx()),
        radius = 3.dp.toPx(),
    )
}