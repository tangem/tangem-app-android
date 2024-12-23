package com.tangem.common.ui.expressStatus.state

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampStatus

interface ExpressTransactionStateUM {

    val info: ExpressTransactionStateInfoUM

    data class OnrampUM(
        override val info: ExpressTransactionStateInfoUM,
        val providerName: String, // todo onramp fix after SwapProvider moved to own module
        val providerImageUrl: String, // todo onramp fix after SwapProvider moved to own module
        val providerType: String, // todo onramp fix after SwapProvider moved to own module
        val activeStatus: OnrampStatus.Status,
        val fromCurrencyCode: String,
    ) : ExpressTransactionStateUM
}

data class ExpressTransactionStateInfoUM(
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

enum class ExpressTransactionStateIconUM {
    Warning,
    Error,
    None,
}