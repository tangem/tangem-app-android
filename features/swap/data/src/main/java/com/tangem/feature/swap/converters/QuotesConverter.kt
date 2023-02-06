package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.models.QuoteResponse
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class QuotesConverter @Inject constructor() : Converter<QuoteResponse, QuoteModel> {

    override fun convert(value: QuoteResponse): QuoteModel {
        return QuoteModel(
            fromTokenAmount = createFromAmountWithOffset(value.fromTokenAmount, value.fromToken.decimals),
            toTokenAmount = createFromAmountWithOffset(value.toTokenAmount, value.toToken.decimals),
            fromTokenAddress = value.fromToken.address,
            toTokenAddress = value.toToken.address,
            estimatedGas = value.estimatedGas,
        )
    }
}
