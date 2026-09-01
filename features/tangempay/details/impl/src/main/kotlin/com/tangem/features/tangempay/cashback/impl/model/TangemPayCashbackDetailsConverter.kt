package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackDetailsUM
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayCashbackDetailsConverter {

    fun convert(
        cards: List<CashbackCard>,
        payoutCurrency: String?,
        accountMonthlyCap: CashbackPromotions.MonthlyCap?,
    ): TangemPayCashbackDetailsUM {
        val rows = buildList {
            addAll(cards.mapNotNull(::cardRow))
            if (cards.isNotEmpty()) {
                add(resourceReference(R.string.tangempay_cashback_details_eu_excluded))
                payoutCurrency?.takeIf(String::isNotBlank)?.let { currency ->
                    add(
                        resourceReference(
                            id = R.string.tangempay_cashback_details_paid_in,
                            formatArgs = wrappedList(currency),
                        ),
                    )
                }
                if (accountMonthlyCap != null) {
                    add(
                        resourceReference(
                            id = R.string.tangempay_cashback_details_cap,
                            formatArgs = wrappedList(accountMonthlyCap.formatted()),
                        ),
                    )
                }
            }
        }
        return TangemPayCashbackDetailsUM(
            title = CashbackRateTitles(cards = cards).title,
            rows = rows.toImmutableList(),
        )
    }

    private fun cardRow(card: CashbackCard): TextReference? {
        val title = card.title ?: return null
        return resourceReference(
            id = R.string.tangempay_cashback_details_tier,
            formatArgs = wrappedList(card.rate.formatRate(), title, card.minPurchase.orEmpty()),
        )
    }

    private fun CashbackPromotions.MonthlyCap.formatted(): String {
        val javaCurrency = getJavaCurrencyByCode(currency ?: DEFAULT_CURRENCY_CODE)
        return amount.format { fiat(javaCurrency.currencyCode, javaCurrency.symbol).optionalDecimals() }
    }

    private companion object {
        const val DEFAULT_CURRENCY_CODE = "USD"
    }
}