package com.tangem.domain.transaction.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus

data class AvailableFeeTokens(
    val tokens: List<CryptoCurrencyStatus>,
    val notEnoughForFeeIds: Set<CryptoCurrency.ID> = emptySet(),
)