package com.tangem.core.ui.decorations

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.TangemTheme

@Composable
fun Modifier.roundedShapeItemDecoration(
    currentIndex: Int,
    lastIndex: Int,
    addDefaultPadding: Boolean = true,
    radius: Dp = TangemTheme.dimens.radius16,
): Modifier = composed {
    val modifier = if (addDefaultPadding) this.padding(horizontal = TangemTheme.dimens.spacing16) else this
    val isSingleItem = currentIndex == 0 && lastIndex == 0
    when {
        isSingleItem -> {
            modifier
                .then(
                    if (addDefaultPadding) {
                        Modifier.padding(top = TangemTheme.dimens.spacing12)
                    } else {
                        Modifier
                    },
                )
                .clip(shape = RoundedCornerShape(radius))
        }
        currentIndex == 0 -> {
            modifier
                .then(
                    if (addDefaultPadding) {
                        Modifier.padding(top = TangemTheme.dimens.spacing12)
                    } else {
                        Modifier
                    },
                )
                .clip(
                    shape = RoundedCornerShape(
                        topStart = radius,
                        topEnd = radius,
                    ),
                )
        }
        currentIndex == lastIndex -> {
            modifier
                .clip(
                    shape = RoundedCornerShape(
                        bottomStart = radius,
                        bottomEnd = radius,
                    ),
                )
        }
        else -> modifier
    }
}