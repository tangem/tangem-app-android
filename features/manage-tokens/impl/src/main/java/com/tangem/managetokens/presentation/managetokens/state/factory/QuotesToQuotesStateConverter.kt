package com.tangem.managetokens.presentation.managetokens.state.factory

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.tokens.model.Quote
import com.tangem.managetokens.presentation.managetokens.state.PriceChangeType
import com.tangem.managetokens.presentation.managetokens.state.QuotesState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Suppress("MagicNumber") // TODO: remove when chart data is added (in [REDACTED_TASK_KEY] when endpoint is ready)
internal class QuotesToQuotesStateConverter : Converter<Quote, QuotesState> {
    override fun convert(value: Quote): QuotesState {
        val priceChange = value.priceChange
        return QuotesState.Content(
            priceChange = BigDecimalFormatter.formatPercent(
                percent = priceChange.movePointLeft(2),
                useAbsoluteValue = true,
                maxFractionDigits = 1,
                minFractionDigits = 1,
            ),
            changeType = priceChange.getPriceChangeType(),
            chartData = // TODO (in [REDACTED_TASK_KEY] when endpoint is ready)
            when (priceChange.getPriceChangeType()) {
                PriceChangeType.UP -> persistentListOf(0f, 5f, 10f, 30f)
                PriceChangeType.DOWN -> persistentListOf(15f, 12f, 13f, 18f, 10f, 3f)
            },
        )
    }

    private fun BigDecimal.getPriceChangeType(): PriceChangeType {
        return if (this >= BigDecimal.ZERO) PriceChangeType.UP else PriceChangeType.DOWN
    }
}