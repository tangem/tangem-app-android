package com.tangem.feature.wallet.presentation.common.state

import androidx.compose.runtime.Composable
import com.tangem.common.Strings
import com.tangem.core.ui.extensions.resolveReference

@Composable
internal fun TokenItemState.DraggableItemInfo.resolve(): String {
    return when (this) {
        is TokenItemState.DraggableItemInfo.Balance -> {
            if (isBalanceHidden) Strings.STARS else balance.resolveReference()
        }
        is TokenItemState.DraggableItemInfo.Status -> {
            info.resolveReference()
        }
    }
}
