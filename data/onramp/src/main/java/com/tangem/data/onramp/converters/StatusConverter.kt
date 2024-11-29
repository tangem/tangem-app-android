package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.OnrampStatusResponse
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.utils.converter.Converter

internal class StatusConverter : Converter<OnrampStatusResponse, OnrampStatus> {
    override fun convert(value: OnrampStatusResponse): OnrampStatus {
        return OnrampStatus(
            txId = value.txId,
            providerId = value.providerId,
            payoutAddress = value.payoutAddress,
            status = OnrampStatus.Status.valueOf(value.status.name),
            failReason = value.failReason,
            externalTxId = value.externalTxId,
            externalTxUrl = value.externalTxUrl,
            payoutHash = value.payoutHash,
            createdAt = value.createdAt,
            fromCurrencyCode = value.fromCurrencyCode,
            fromAmount = value.fromAmount,
            toContractAddress = value.toContractAddress,
            toNetwork = value.toNetwork,
            toDecimals = value.toDecimals,
            toAmount = value.toAmount,
            toActualAmount = value.toActualAmount,
            paymentMethod = value.paymentMethod,
            countryCode = value.countryCode,
        )
    }
}