package com.tangem.data.onramp.converters

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.api.onramp.models.response.Status
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.utils.converter.Converter

internal class StatusConverter(moshi: Moshi) : Converter<OnrampItemResponse, OnrampStatus> {

    private val responseStatusAdapter = moshi.adapter(Status::class.java)

    override fun convert(value: OnrampItemResponse): OnrampStatus {
        return OnrampStatus(
            txId = value.txId,
            providerId = value.providerId,
            payoutAddress = value.payoutAddress,
            status = OnrampStatus.Status.valueOf(value.status.toResponseStatus().name),
            failReason = value.failReason,
            externalTxId = value.externalTxId,
            externalTxUrl = value.externalTxUrl,
            payoutHash = value.payoutHash,
            createdAt = value.createdAt,
            fromCurrencyCode = value.fromCurrencyCode,
            fromAmount = value.fromAmount,
            toContractAddress = value.toContractAddress,
            toNetwork = value.toNetwork,
            toDecimals = value.toDecimals.toString(),
            toAmount = value.toAmount,
            toActualAmount = value.toActualAmount,
            paymentMethod = value.paymentMethod,
            countryCode = value.countryCode,
        )
    }

    private fun String.toResponseStatus(): Status = responseStatusAdapter.fromJsonValue(this)
        ?: error("Unknown onramp status: $this")
}