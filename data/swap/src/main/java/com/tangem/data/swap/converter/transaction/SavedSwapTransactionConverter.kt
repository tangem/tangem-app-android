package com.tangem.data.swap.converter.transaction

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.swap.models.SwapStatusDTO
import com.tangem.data.swap.models.SwapTransactionDTO
import com.tangem.domain.swap.models.SwapTransactionModel
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.converter.TwoWayConverter

internal class SavedSwapTransactionConverter(
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : TwoWayConverter<SwapTransactionModel, SwapTransactionDTO> {

    private val statusConverter by lazy(LazyThreadSafetyMode.NONE) {
        SavedSwapStatusConverter()
    }

    override fun convert(value: SwapTransactionModel) = SwapTransactionDTO(
        txId = value.txId,
        timestamp = value.timestamp,
        fromCryptoAmount = value.fromCryptoAmount,
        toCryptoAmount = value.toCryptoAmount,
        provider = value.provider,
        status = value.status,
    )

    override fun convertBack(value: SwapTransactionDTO) = SwapTransactionModel(
        txId = value.txId,
        timestamp = value.timestamp,
        fromCryptoAmount = value.fromCryptoAmount,
        toCryptoAmount = value.toCryptoAmount,
        provider = value.provider,
        status = value.status,
    )

    fun convertBack(
        value: SwapTransactionDTO,
        userWallet: UserWallet,
        txStatuses: Map<String, SwapStatusDTO>,
    ): SwapTransactionModel {
        val status = txStatuses[value.txId]
        val refundCurrency = status?.refundTokensResponse?.let { id ->
            responseCryptoCurrenciesFactory.createCurrency(
                responseToken = id,
                userWallet = userWallet,
            )
        }
        val statusWithRefundCurrency = status?.copy(refundCurrency = refundCurrency)

        return SwapTransactionModel(
            txId = value.txId,
            timestamp = value.timestamp,
            fromCryptoAmount = value.fromCryptoAmount,
            toCryptoAmount = value.toCryptoAmount,
            provider = value.provider,
            status = statusWithRefundCurrency?.let(statusConverter::convert),
        )
    }
}