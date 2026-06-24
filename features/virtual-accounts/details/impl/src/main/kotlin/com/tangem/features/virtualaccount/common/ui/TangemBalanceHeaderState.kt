package com.tangem.features.virtualaccount.common.ui

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface TangemBalanceHeaderState {

    data object Loading : TangemBalanceHeaderState

    data class Content(
        val balance: TextReference,
        val isBalanceHidden: Boolean,
        val isFlickering: Boolean = false,
    ) : TangemBalanceHeaderState

    data object Error : TangemBalanceHeaderState
}