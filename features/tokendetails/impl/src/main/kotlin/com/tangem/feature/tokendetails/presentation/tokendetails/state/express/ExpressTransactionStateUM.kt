package com.tangem.feature.tokendetails.presentation.tokendetails.state.express

import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import kotlinx.collections.immutable.ImmutableList

internal sealed class ExpressTransactionStateUM {

    abstract val info: ExpressTransactionStateInfoUM

    data class ExchangeUM(
        override val info: ExpressTransactionStateInfoUM,
        val provider: SwapProvider,
        val activeStatus: ExchangeStatus?,
        val statuses: ImmutableList<ExchangeStatusState>,
        val notification: ExchangeStatusNotifications? = null,
        val showProviderLink: Boolean,
        val isRefundTerminalStatus: Boolean,
        val fromCryptoCurrency: CryptoCurrency,
        val toCryptoCurrency: CryptoCurrency,
    ) : ExpressTransactionStateUM()

    data class OnrampUM(
        override val info: ExpressTransactionStateInfoUM,
        val providerName: String, // todo onramp fix after SwapProvider moved to own module
        val providerImageUrl: String, // todo onramp fix after SwapProvider moved to own module
        val providerType: String, // todo onramp fix after SwapProvider moved to own module
        val activeStatus: OnrampStatus.Status,
        val fromCurrencyCode: String,
    ) : ExpressTransactionStateUM()
}

internal data class ExpressTransactionStateInfoUM(
    val title: TextReference,
    val status: ExpressStatusUM,
    val notification: NotificationUM?,
    val txId: String,
    val txExternalId: String?,
    val txExternalUrl: String?,
    val timestamp: Long,
    val timestampFormatted: TextReference,
    val onGoToProviderClick: (String) -> Unit,
    val onClick: () -> Unit,
    val iconState: ExpressTransactionStateIconUM,

    val toAmount: TextReference,
    val toFiatAmount: TextReference?,
    val toAmountSymbol: String,
    val toCurrencyIcon: CurrencyIconState,

    val fromAmount: TextReference,
    val fromFiatAmount: TextReference?,
    val fromAmountSymbol: String,
    val fromCurrencyIcon: CurrencyIconState,
)

internal enum class ExpressTransactionStateIconUM {
    Warning,
    Error,
    None,
}