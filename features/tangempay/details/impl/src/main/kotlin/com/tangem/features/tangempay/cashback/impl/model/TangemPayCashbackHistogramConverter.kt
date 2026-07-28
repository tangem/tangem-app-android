package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM.Style
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class TangemPayCashbackHistogramConverter(
    private val dateFormatter: TangemPayCashbackDateFormatter = TangemPayCashbackDateFormatter(),
) : Converter<CashbackHistory, TangemPayCashbackHistogramUM> {

    // TODO([REDACTED_TASK_KEY]): move hardcoded strings to string resources
    override fun convert(value: CashbackHistory): TangemPayCashbackHistogramUM {
        val currency = getJavaCurrencyByCode(value.currency)
        val total = value.months.fold(BigDecimal.ZERO) { acc, month -> acc + month.confirmedAmount }
        val formattedTotal = total.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
        val lastIndex = value.months.lastIndex
        return TangemPayCashbackHistogramUM(
            title = stringReference("$formattedTotal earned in total"),
            bars = value.months.mapIndexed { index, month ->
                TangemPayCashbackHistogramUM.Bar(
                    month = stringReference(dateFormatter.formatShortMonth(month.year, month.month)),
                    amount = stringReference(
                        month.confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).defaultAmount() },
                    ),
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
}