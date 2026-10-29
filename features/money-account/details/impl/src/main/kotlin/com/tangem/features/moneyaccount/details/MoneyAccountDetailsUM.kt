package com.tangem.features.moneyaccount.details

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class MoneyAccountDetailsUM(
    val title: TextReference,
    val subtitle: TextReference,
    val balance: MoneyAccountBalanceBlockState,
    val isBalanceHidden: Boolean,
    val menuItems: ImmutableList<MoneyAccountMenuItemUM>,
    val onBackClick: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onSendClick: () -> Unit,
    val txHistoryCurrency: CryptoCurrency.Token?,
    val pullToRefreshConfig: PullToRefreshConfig,
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