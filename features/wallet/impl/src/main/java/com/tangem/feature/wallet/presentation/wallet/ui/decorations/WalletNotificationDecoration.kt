package com.tangem.feature.wallet.presentation.wallet.ui.decorations

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.tangem.core.ui.res.TangemTheme

/**
 * Wallet notification decoration
 *
 * @param currentIndex current index
 * @param lastIndex    last index
 *
[REDACTED_AUTHOR]
 */
internal fun Modifier.walletNotificationDecoration(currentIndex: Int, lastIndex: Int): Modifier = composed {
    val modifierWithHorizontalPadding = this.padding(horizontal = TangemTheme.dimens.spacing16)

    if (currentIndex != lastIndex) {
        modifierWithHorizontalPadding.padding(bottom = TangemTheme.dimens.spacing14)
    } else {
        modifierWithHorizontalPadding
    }
}