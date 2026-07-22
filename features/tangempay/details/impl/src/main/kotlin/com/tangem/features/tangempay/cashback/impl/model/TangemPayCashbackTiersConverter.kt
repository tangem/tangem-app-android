package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TangemPayCashbackTiersConverter : Converter<CashbackPromotions, List<CashbackTier>> {

    override fun convert(value: CashbackPromotions): List<CashbackTier> {
        return value.cardTiers.map { tier ->
            CashbackTier(
                tierId = tier.tier,
                rate = CashbackRates.forTier(tier.tier),
                label = tier.label,
                scope = tier.scope,
                minPurchase = tier.minTransactionAmount?.formatUsd(),
                monthlyCap = tier.monthlyCapAmount?.formatUsd(),
            )
        }
    }

    private fun BigDecimal.formatUsd(): String {
        val currency = getJavaCurrencyByCode(AMOUNT_CURRENCY_CODE)
        return format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
    }

    private companion object {
        const val AMOUNT_CURRENCY_CODE = "USD"
    }
}