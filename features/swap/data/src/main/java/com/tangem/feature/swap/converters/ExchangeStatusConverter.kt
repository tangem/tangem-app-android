package com.tangem.feature.swap.converters

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime
import com.tangem.datasource.api.express.models.response.ExchangeStatus as ResponseExchangeStatus

internal class ExchangeStatusConverter(moshi: Moshi) : Converter<ExchangeItemResponse, ExchangeStatusModel> {

    private val responseStatusAdapter = moshi.adapter(ResponseExchangeStatus::class.java)

    override fun convert(value: ExchangeItemResponse): ExchangeStatusModel {
        return ExchangeStatusModel(
            providerId = value.providerId,
            status = value.status.toExchangeStatus(),
            txId = value.externalTxId,
            txExternalUrl = value.externalTxUrl,
            txExternalId = value.externalTxId,
            refundNetwork = value.refundNetwork,
            refundContractAddress = value.refundContractAddress,
            createdAt = runCatching { DateTime.parse(value.createdAt) }.getOrNull(),
            averageDuration = value.averageDuration?.toInt(),
        )
    }

    private fun String.toExchangeStatus(): ExchangeStatus? {
        val responseStatus = responseStatusAdapter.fromJsonValue(this) ?: return null
        return ExchangeStatus.entries.firstOrNull { it.name == responseStatus.name }
    }
}