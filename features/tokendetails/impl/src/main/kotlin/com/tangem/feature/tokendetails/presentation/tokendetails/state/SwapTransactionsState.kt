package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import kotlinx.coroutines.flow.MutableStateFlow

internal data class SwapTransactionsState(
    val txId: String,
    val provider: SwapProvider,
    val txUrl: String? = null,
    val timestamp: TextReference,
    val fiatSymbol: String,
    val activeStatus: MutableStateFlow<ExchangeStatus?>,
    val statuses: MutableStateFlow<List<ExchangeStatusState>>,
    val notification: MutableStateFlow<ExchangeStatusNotifications?> = MutableStateFlow(null),
    val toCryptoCurrencyId: CryptoCurrency.ID,
    val toCryptoAmount: String,
    val toCryptoSymbol: String,
    val toFiatAmount: String,
    val toCurrencyIcon: TokenIconState,
    val fromCryptoCurrencyId: CryptoCurrency.ID,
    val fromCryptoAmount: String,
    val fromCryptoSymbol: String,
    val fromFiatAmount: String,
    val fromCurrencyIcon: TokenIconState,
    val onClick: () -> Unit,
    val onGoToProviderClick: () -> Unit,
)

internal class ExchangeStatusState(
    val status: ExchangeStatus,
    val text: TextReference,
    val isActive: Boolean,
    val isDone: Boolean,
)