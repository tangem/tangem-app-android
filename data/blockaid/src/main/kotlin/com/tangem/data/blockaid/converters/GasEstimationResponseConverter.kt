package com.tangem.data.blockaid.converters

import com.domain.blockaid.models.transaction.GasEstimationResult
import com.tangem.blockchain.extensions.hexToBigInteger
import com.tangem.store.datasource.blockaid.models.response.GasEstimationResponse
import com.tangem.utils.converter.Converter

internal object GasEstimationResponseConverter : Converter<List<GasEstimationResponse>, GasEstimationResult> {

    private const val ERROR_STATUS = "Error"

    override fun convert(value: List<GasEstimationResponse>): GasEstimationResult {
        val estimations = value.map(GasEstimationResponse::gasEstimation)
        if (estimations.any { it.status == ERROR_STATUS }) return GasEstimationResult.Reverted

        val gasLimits = estimations.map { it.estimate ?: return GasEstimationResult.Unavailable }
        if (gasLimits.isEmpty()) return GasEstimationResult.Unavailable

        return GasEstimationResult.Estimated(gasLimits = gasLimits.map(String::hexToBigInteger))
    }
}