package com.tangem.data.onramp.converters

import com.tangem.data.onramp.models.OnrampTransactionDTO
import com.tangem.datasource.api.onramp.models.response.Status
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.utils.converter.TwoWayConverter

internal class TransactionConverter : TwoWayConverter<OnrampTransactionDTO, OnrampTransaction> {

    private val currencyConverter = CurrencyConverter()

    override fun convert(value: OnrampTransactionDTO): OnrampTransaction {
        return OnrampTransaction(
            txId = value.txId,
            userWalletId = value.userWalletId,
            fromAmount = value.fromAmount,
            fromCurrency = currencyConverter.convert(value.fromCurrency),
            toAmount = value.toAmount,
            toCurrencyId = value.toCurrencyId,
            status = OnrampStatus.Status.valueOf(value.status.name),
            externalTxUrl = value.externalTxUrl,
            externalTxId = value.externalTxId,
            timestamp = value.timestamp,
            providerName = value.providerName,
            providerImageUrl = value.providerImageUrl,
            providerType = value.providerType,
            redirectUrl = "", // not necessary field
            paymentMethod = value.paymentMethod,
            residency = value.residency,
        )
    }

    override fun convertBack(value: OnrampTransaction): OnrampTransactionDTO {
        return OnrampTransactionDTO(
            txId = value.txId,
            userWalletId = value.userWalletId,
            fromAmount = value.fromAmount,
            fromCurrency = currencyConverter.convertBack(value.fromCurrency),
            toAmount = value.toAmount,
            toCurrencyId = value.toCurrencyId,
            status = Status.valueOf(value.status.name),
            externalTxUrl = value.externalTxUrl,
            externalTxId = value.externalTxId,
            timestamp = value.timestamp,
            providerName = value.providerName,
            providerImageUrl = value.providerImageUrl,
            providerType = value.providerType,
            paymentMethod = value.paymentMethod,
            residency = value.residency,
        )
    }
}