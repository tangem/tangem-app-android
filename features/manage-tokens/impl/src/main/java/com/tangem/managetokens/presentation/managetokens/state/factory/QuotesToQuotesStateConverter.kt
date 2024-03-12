package com.tangem.managetokens.presentation.managetokens.state.factory

import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.tokens.model.Quote
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
            ),
            changeType = priceChange.getPriceChangeType(),
            chartData = // TODO (in [REDACTED_TASK_KEY] when endpoint is ready)
            when (priceChange.getPriceChangeType()) {
                PriceChangeType.UP -> persistentListOf(0f, 5f, 10f, 30f)
                PriceChangeType.DOWN -> persistentListOf(15f, 12f, 13f, 18f, 10f, 3f)
                PriceChangeType.NEUTRAL -> persistentListOf(0f, 0f, 0f, 0f)
            },
        )
    }

    private fun BigDecimal.getPriceChangeType(): PriceChangeType {
        return PriceChangeConverter.fromBigDecimal(value = this)
    }
}