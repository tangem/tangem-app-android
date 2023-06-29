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
internal fun Modifier.walletContentItemDecoration(currentIndex: Int, lastItemIndex: Int): Modifier = composed {
    when (currentIndex) {
        0 -> {
            this
                .padding(top = TangemTheme.dimens.spacing14)
                .clip(
                    RoundedCornerShape(
                        topStart = TangemTheme.dimens.radius16,
                        topEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        lastItemIndex -> {
            this
                .clip(
                    RoundedCornerShape(
                        bottomStart = TangemTheme.dimens.radius16,
                        bottomEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        else -> this
    }
}