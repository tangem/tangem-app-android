package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.models.QuoteResponse
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.utils.converter.Converter

class QuotesConverter : Converter<QuoteResponse, QuoteModel> {

    override fun convert(value: QuoteResponse): QuoteModel {
        return QuoteModel(
            toTokenAmount = createFromAmountWithOffset(value.toTokenAmount, value.toToken.decimals),
        )
    }
}