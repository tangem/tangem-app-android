package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM.Style
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class TangemPayCashbackHistogramConverter(
    private val dateFormatter: TangemPayCashbackDateFormatter = TangemPayCashbackDateFormatter(),
) {
    fun convert(
        history: CashbackHistory,
        totalEarnedAmount: BigDecimal?,
        totalCurrency: String?,
    ): TangemPayCashbackHistogramUM {
        val total = totalEarnedAmount
            ?: history.months.fold(BigDecimal.ZERO) { acc, month -> acc + month.confirmedAmount }
        val totalCurrencyCode = totalCurrency
            ?: history.months.firstOrNull()?.currency
            ?: DEFAULT_CURRENCY_CODE
        val lastIndex = history.months.lastIndex
        return TangemPayCashbackHistogramUM(
            title = resourceReference(
                id = R.string.tangempay_cashback_total_earned,
                formatArgs = wrappedList(total.formatTotal(totalCurrencyCode)),
            ),
            bars = history.months.mapIndexed { index, month ->
                TangemPayCashbackHistogramUM.Bar(
                    month = stringReference(dateFormatter.formatShortMonth(month.year, month.month)),
                    amount = stringReference(month.formatBar()),
                    amountValue = month.confirmedAmount.toFloat(),
                    style = when {
                        index != lastIndex -> Style.Regular
                        month.confirmedAmount.signum() < 0 -> Style.HighlightedNegative
                        else -> Style.Highlighted
                    },
                )
            }.toImmutableList(),
        )
    }

    private fun BigDecimal.formatTotal(currencyCode: String): String {
        val currency = getJavaCurrencyByCode(currencyCode)
        return format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
    }

    private fun CashbackHistory.MonthlyCashback.formatBar(): String {
        val currency = getJavaCurrencyByCode(this.currency)
        return confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).defaultAmount() }
    }

    private companion object {
        const val DEFAULT_CURRENCY_CODE = "USD"
    }
}