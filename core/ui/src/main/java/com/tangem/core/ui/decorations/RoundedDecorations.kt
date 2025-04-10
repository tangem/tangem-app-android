package com.tangem.core.ui.decorations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.TangemTheme

@Composable
fun Modifier.roundedShapeItemDecoration(
    currentIndex: Int,
    lastIndex: Int,
    addDefaultPadding: Boolean = true,
    radius: Dp = TangemTheme.dimens.radius16,
    backgroundColor: Color? = null,
): Modifier = composed {
    val modifier = if (addDefaultPadding) this.padding(horizontal = TangemTheme.dimens.spacing16) else this

    val applyTopPadding: @Composable Modifier.() -> Modifier = {
        if (addDefaultPadding) {
            padding(top = TangemTheme.dimens.spacing12)
        } else {
            this
        }
    }

    val applyShape: Modifier.(shape: RoundedCornerShape?) -> Modifier = { shape ->
        if (backgroundColor != null) {
            if (shape != null) {
                clip(shape).background(color = backgroundColor, shape = shape)
            } else {
                background(color = backgroundColor)
            }
        } else {
            if (shape != null) {
                clip(shape = shape)
            } else {
                this
            }
        }
    }

    val isSingleItem = currentIndex == 0 && lastIndex == 0
    when {
        isSingleItem -> {
            modifier
                .applyTopPadding()
                .applyShape(RoundedCornerShape(radius))
        }
        currentIndex == 0 -> {
            modifier
                .applyTopPadding()
                .applyShape(RoundedCornerShape(topStart = radius, topEnd = radius))
        }
        currentIndex == lastIndex -> {
            modifier.applyShape(RoundedCornerShape(bottomStart = radius, bottomEnd = radius))
        }
        else -> modifier.applyShape(null)
    }
}