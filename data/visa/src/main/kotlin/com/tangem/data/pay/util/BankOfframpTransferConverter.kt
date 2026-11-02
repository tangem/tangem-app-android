package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankOfframpTransfer
import com.tangem.spend.datasource.pay.models.response.BankOfframpTransferResponse
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

internal object BankOfframpTransferConverter :
    Converter<BankOfframpTransferResponse.Result, BankOfframpTransfer?> {

    override fun convert(value: BankOfframpTransferResponse.Result): BankOfframpTransfer? {
        return BankOfframpTransfer(
            transferId = value.transferId?.takeIf(String::isNotBlank) ?: return null,
            depositAddress = value.depositAddress?.takeIf(String::isNotBlank) ?: return null,
            sourceAmount = value.sourceAmount ?: return null,
            destinationAmount = value.destinationAmount ?: return null,
            expiresAt = value.expiresAt?.let { runCatching { DateTime.parse(it) }.getOrNull() } ?: return null,
        )
    }
}