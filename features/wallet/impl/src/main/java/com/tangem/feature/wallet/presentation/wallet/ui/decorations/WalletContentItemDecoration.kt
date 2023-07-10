package com.tangem.feature.wallet.presentation.wallet.ui.decorations

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.res.TangemTheme

/**
[REDACTED_AUTHOR]
 */
internal fun Modifier.walletContentItemDecoration(currentIndex: Int, lastIndex: Int): Modifier = composed {
    val modifierWithHorizontalPadding = this.padding(horizontal = TangemTheme.dimens.spacing16)
    when (currentIndex) {
        0 -> {
            modifierWithHorizontalPadding
                .padding(top = TangemTheme.dimens.spacing14)
                .clip(
                    RoundedCornerShape(
                        topStart = TangemTheme.dimens.radius16,
                        topEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        lastIndex -> {
            modifierWithHorizontalPadding
                .clip(
                    RoundedCornerShape(
                        bottomStart = TangemTheme.dimens.radius16,
                        bottomEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        else -> modifierWithHorizontalPadding
    }
}