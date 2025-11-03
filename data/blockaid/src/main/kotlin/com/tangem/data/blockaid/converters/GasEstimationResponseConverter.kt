package com.tangem.data.blockaid.converters

import com.domain.blockaid.models.transaction.GasEstimationResult
import com.tangem.blockchain.extensions.hexToBigInteger
import com.tangem.datasource.api.common.blockaid.models.response.GasEstimationResponse
import com.tangem.utils.converter.Converter

internal object GasEstimationResponseConverter : Converter<List<GasEstimationResponse>, GasEstimationResult> {
    override fun convert(value: List<GasEstimationResponse>): GasEstimationResult {
        return GasEstimationResult(
            estimatedGasList = value.map { it.gasEstimation.estimate.hexToBigInteger() },
        )
    }
}