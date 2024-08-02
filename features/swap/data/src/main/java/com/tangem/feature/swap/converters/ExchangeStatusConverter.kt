package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.ExchangeStatusResponse
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.utils.converter.Converter

internal class ExchangeStatusConverter : Converter<ExchangeStatusResponse, ExchangeStatusModel> {
    override fun convert(value: ExchangeStatusResponse): ExchangeStatusModel {
        return ExchangeStatusModel(
            providerId = value.providerId,
            status = ExchangeStatus.values().firstOrNull {
                it.name.lowercase() == value.status.name.lowercase()
            },
            txId = value.externalTxId,
            txExternalUrl = value.externalTxUrl,
            txExternalId = value.externalTxId,
            refundNetwork = value.refundNetwork,
            refundContractAddress = value.refundContractAddress,
        )
    }
}