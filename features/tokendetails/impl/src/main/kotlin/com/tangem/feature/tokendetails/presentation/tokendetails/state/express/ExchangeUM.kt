package com.tangem.feature.tokendetails.presentation.tokendetails.state.express

import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateInfoUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import kotlinx.collections.immutable.ImmutableList

internal data class ExchangeUM(
    override val info: ExpressTransactionStateInfoUM,
    val provider: SwapProvider,
    val activeStatus: ExchangeStatus?,
    val statuses: ImmutableList<ExchangeStatusState>,
    val notification: ExchangeStatusNotifications? = null,
    val showProviderLink: Boolean,
    val fromCryptoCurrency: CryptoCurrency,
    val toCryptoCurrency: CryptoCurrency,
) : ExpressTransactionStateUM