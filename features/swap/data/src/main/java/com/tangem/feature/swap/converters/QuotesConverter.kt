package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.models.QuoteResponse
import com.tangem.feature.swap.domain.models.data.SwapState.QuoteModel
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class QuotesConverter @Inject constructor() : Converter<QuoteResponse, QuoteModel> {

    override fun convert(value: QuoteResponse): QuoteModel {
        return QuoteModel(
            fromTokenAmount = value.fromTokenAmount,
            toTokenAmount = value.toTokenAmount,
            estimatedGas = value.estimatedGas,
        )
    }
}
