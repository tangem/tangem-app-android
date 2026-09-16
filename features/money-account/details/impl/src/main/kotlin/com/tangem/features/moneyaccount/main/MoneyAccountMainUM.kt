package com.tangem.features.moneyaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class MoneyAccountMainUM(
    val title: TextReference,
    val subtitle: TextReference,
    val balance: MoneyAccountBalanceBlockState,
    val isBalanceHidden: Boolean,
    val onBackClick: () -> Unit,
    val onMenuClick: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onSendClick: () -> Unit,
)

@Immutable
internal sealed class MoneyAccountBalanceBlockState {

    data object Loading : MoneyAccountBalanceBlockState()

    data class Content(
        val fiatBalance: TextReference,
        val isBalanceFlickering: Boolean,
    ) : MoneyAccountBalanceBlockState()

    data object Error : MoneyAccountBalanceBlockState()
}