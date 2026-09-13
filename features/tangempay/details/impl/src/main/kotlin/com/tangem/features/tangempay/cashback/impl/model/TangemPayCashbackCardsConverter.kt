package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TangemPayCashbackCardsConverter : Converter<CashbackPromotions, List<CashbackCard>> {

    override fun convert(value: CashbackPromotions): List<CashbackCard> {
        return value.cards.map { card ->
            CashbackCard(
                cardType = card.cardType,
                title = card.title,
                rate = card.cashbackRate,
                minPurchase = card.minTransactionAmount?.formatUsd(),
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