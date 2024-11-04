package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

/**
 * Converts [CryptoCurrencyStatus] to [EnterAmountBoundary]
 */
class MaxEnterAmountConverter : Converter<CryptoCurrencyStatus, EnterAmountBoundary> {

    override fun convert(value: CryptoCurrencyStatus): EnterAmountBoundary {
        return EnterAmountBoundary(
            amount = value.value.amount,
            fiatAmount = value.value.fiatAmount,
            fiatRate = value.value.fiatRate,
        )
    }
}