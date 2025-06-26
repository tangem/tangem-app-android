package com.tangem.data.swap.converter

import com.tangem.datasource.api.express.models.response.ExchangeStatusResponse
import com.tangem.domain.swap.models.SwapStatus
import com.tangem.domain.swap.models.SwapStatusModel
import com.tangem.utils.converter.Converter

internal class SwapStatusConverter : Converter<ExchangeStatusResponse, SwapStatusModel> {
    override fun convert(value: ExchangeStatusResponse): SwapStatusModel {
        return SwapStatusModel(
            providerId = value.providerId,
            status = SwapStatus.entries.firstOrNull {
                it.name.lowercase() == value.status.name.lowercase()
            },
            txId = value.externalTxId,
            txExternalUrl = value.externalTxUrl,
            txExternalId = value.externalTxId,
            refundNetwork = value.refundNetwork,
            refundContractAddress = value.refundContractAddress,
            createdAt = value.createdAt,
            averageDuration = value.averageDuration,
        )
    }
}