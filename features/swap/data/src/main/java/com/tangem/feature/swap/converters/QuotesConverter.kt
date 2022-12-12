package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.BaseOneInchResponse
import com.tangem.datasource.api.oneinch.models.QuoteResponse
import com.tangem.feature.swap.domain.models.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.data.QuoteModel
import com.tangem.feature.swap.domain.models.data.mapErrors
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class QuotesConverter @Inject constructor() : Converter<BaseOneInchResponse<QuoteResponse>, AggregatedSwapDataModel<QuoteModel>> {

    override fun convert(value: BaseOneInchResponse<QuoteResponse>): AggregatedSwapDataModel<QuoteModel> {
        val body = value.body
        return if (body != null) {
            AggregatedSwapDataModel(
                QuoteModel(
                    fromTokenAmount = body.fromTokenAmount,
                    toTokenAmount = body.toTokenAmount,
                    estimatedGas = body.estimatedGas,
                ),
            )
        } else {
            AggregatedSwapDataModel(
                null,
                mapErrors(value.errorDto?.description),
            )
        }
    }
}