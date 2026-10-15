package com.tangem.features.moneyaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class MoneyAccountMainUM(
    val title: TextReference,
    val subtitle: TextReference,
    val balance: MoneyAccountBalanceBlockState,
    val isBalanceHidden: Boolean,
    val menuItems: ImmutableList<MoneyAccountMenuItemUM>,
    val onBackClick: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onSendClick: () -> Unit,
)

@Immutable
internal data class MoneyAccountMenuItemUM(
    val title: TextReference,
    val icon: TangemIconUM,
    val onClick: () -> Unit,
)

@Immutable
internal sealed class MoneyAccountBalanceBlockState {

    data object Loading : MoneyAccountBalanceBlockState()

    data class Content(
        val fiatBalance: TextReference,
        val isBalanceFlickering: Boolean,
    ) : MoneyAccountBalanceBlockState()

    data object Unavailable : MoneyAccountBalanceBlockState()
}