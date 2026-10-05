package com.tangem.features.tangempay.withdraw.bank.amount.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayBankWithdrawAmountUM(
    val header: BankWithdrawHeaderUM,
    val amount: AmountFieldUM,
    val source: SourceRowUM,
    val route: RouteRowUM,
    val isContinueEnabled: Boolean,
    val sheet: SheetUM,
    val onBackClick: () -> Unit,
    val onContinueClick: () -> Unit,
) {

    @Immutable
    data class AmountFieldUM(
        val value: String,
        val decimals: Int,
        val currencyCode: String,
        val currencySymbol: String,
        val error: TextReference?,
        val onValueChange: (String) -> Unit,
    )

    @Immutable
    data class SourceRowUM(
        val icon: CurrencyIconState,
        val symbol: String,
        val subtitle: TextReference,
        val onMaxClick: () -> Unit,
        val onClick: () -> Unit,
    )

    /** [onClick] is null when the backend offers a single rail: the row is then static and shows no chevron. */
    @Immutable
    data class RouteRowUM(
        val rail: BankRail,
        val title: TextReference,
        val eta: TextReference,
        val fee: String,
        val onClick: (() -> Unit)?,
    )

    @Immutable
    sealed interface SheetUM {
        val onDismiss: () -> Unit

        data object Hidden : SheetUM {
            override val onDismiss: () -> Unit = {}
        }

        data class Tokens(val items: ImmutableList<TokenItemUM>, override val onDismiss: () -> Unit) : SheetUM

        data class Routes(val items: ImmutableList<RouteItemUM>, override val onDismiss: () -> Unit) : SheetUM
    }

    @Immutable
    data class TokenItemUM(
        val id: String,
        val icon: CurrencyIconState,
        val symbol: String,
        val subtitle: TextReference,
        val isAvailable: Boolean,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    )

    @Immutable
    data class RouteItemUM(
        val rail: BankRail,
        val title: TextReference,
        val eta: TextReference,
        val fee: String,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    )
}