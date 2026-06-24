package com.tangem.features.virtualaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class VirtualAccountMainUM(
    val title: TextReference,
    val subtitle: TextReference,
    val balance: VirtualAccountBalanceBlockState,
    val isBalanceHidden: Boolean,
    val onBackClick: () -> Unit,
    val onMenuClick: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onSendClick: () -> Unit,
)

@Immutable
internal sealed class VirtualAccountBalanceBlockState {

    data object Loading : VirtualAccountBalanceBlockState()

    data class Content(
        val fiatBalance: TextReference,
        val isBalanceFlickering: Boolean,
    ) : VirtualAccountBalanceBlockState()

    data object Error : VirtualAccountBalanceBlockState()
}