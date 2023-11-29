package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import kotlinx.collections.immutable.PersistentList

internal data class SwapTransactionsState(
    val txId: String,
    val provider: SwapProvider,
    val txUrl: String? = null,
    val timestamp: Long,
    val statuses: PersistentList<ExchangeStatusState>,
    val activeStatus: ExchangeStatus?,
    val fiatSymbol: String,
    val notification: ExchangeStatusNotifications? = null,
    val toCryptoAmount: String,
    val toCryptoSymbol: String,
    val toFiatAmount: String,
    val toCurrencyIcon: TokenIconState,
    val fromCryptoAmount: String,
    val fromCryptoSymbol: String,
    val fromFiatAmount: String,
    val fromCurrencyIcon: TokenIconState,
    val onClick: () -> Unit,
    val onGoToProviderClick: () -> Unit,
)

internal class ExchangeStatusState(
    val status: ExchangeStatus,
    val isActive: Boolean,
    val isDone: Boolean,
)
