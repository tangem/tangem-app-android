package com.tangem.managetokens.presentation.managetokens.state.factory

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.tokens.model.Quote
import com.tangem.managetokens.presentation.managetokens.state.PriceChangeType
import com.tangem.managetokens.presentation.managetokens.state.QuotesState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

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
            chartData = value.values?.toPersistentList() ?: persistentListOf(),
        )
    }

    private fun BigDecimal.getPriceChangeType(): PriceChangeType {
        return if (this >= BigDecimal.ZERO) PriceChangeType.UP else PriceChangeType.DOWN
    }
}
