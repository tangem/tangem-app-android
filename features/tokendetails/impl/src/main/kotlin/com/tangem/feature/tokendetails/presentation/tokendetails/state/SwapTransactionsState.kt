package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import kotlinx.collections.immutable.PersistentList
import java.math.BigDecimal

internal data class SwapTransactionsState(
    val txId: String,
    val providerId: Int,
    val txUrl: String? = null,
    val rate: BigDecimal,
    val timestamp: Long,
    val status: PersistentList<ExchangeStatusState>,
    val activeStatus: ExchangeStatus?,
    val toCryptoAmount: String,
    val toFiatAmount: String,
    val toCurrencyIcon: TokenIconState,
    val fromCryptoAmount: String,
    val fromFiatAmount: String,
    val fromCurrencyIcon: TokenIconState,
    val onClick: () -> Unit,
    val onGoToProviderClick: () -> Unit,
)

internal class ExchangeStatusState(
    val status: ExchangeStatus,
    val text: String,
    val isActive: Boolean,
    val isDone: Boolean,
)