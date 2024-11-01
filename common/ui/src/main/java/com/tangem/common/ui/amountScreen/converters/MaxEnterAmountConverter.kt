package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.amountScreen.models.MaxEnterAmount
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

/**
 * Converts [CryptoCurrencyStatus] to [MaxEnterAmount]
 */
class MaxEnterAmountConverter : Converter<CryptoCurrencyStatus, MaxEnterAmount> {

    override fun convert(value: CryptoCurrencyStatus): MaxEnterAmount {
        return MaxEnterAmount(
            amount = value.value.amount,
            fiatAmount = value.value.fiatAmount,
            fiatRate = value.value.fiatRate,
        )
    }
}
