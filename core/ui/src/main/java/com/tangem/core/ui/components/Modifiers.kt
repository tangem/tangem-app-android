package com.tangem.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.res.TangemTheme

fun Modifier.walletContentItemDecoration(currentIndex: Int, lastIndex: Int): Modifier = composed {
    val modifierWithHorizontalPadding = this.padding(horizontal = TangemTheme.dimens.spacing16)
    val isSingleItem = currentIndex == 0 && lastIndex == 0
    when {
        isSingleItem -> {
            modifierWithHorizontalPadding
                .padding(top = TangemTheme.dimens.spacing14)
                .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
        }
        currentIndex == 0 -> {
            modifierWithHorizontalPadding
                .padding(top = TangemTheme.dimens.spacing14)
                .clip(
                    shape = RoundedCornerShape(
                        topStart = TangemTheme.dimens.radius16,
                        topEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        currentIndex == lastIndex -> {
            modifierWithHorizontalPadding
                .clip(
                    shape = RoundedCornerShape(
                        bottomStart = TangemTheme.dimens.radius16,
                        bottomEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        else -> modifierWithHorizontalPadding
    }
}