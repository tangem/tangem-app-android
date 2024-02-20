package com.tangem.core.ui.decorations

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.res.TangemTheme

fun Modifier.roundedShapeItemDecoration(
    currentIndex: Int,
    lastIndex: Int,
    addDefaultPadding: Boolean = true,
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
                .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
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
                        topStart = TangemTheme.dimens.radius16,
                        topEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        currentIndex == lastIndex -> {
            modifier
                .clip(
                    shape = RoundedCornerShape(
                        bottomStart = TangemTheme.dimens.radius16,
                        bottomEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        else -> modifier
    }
}