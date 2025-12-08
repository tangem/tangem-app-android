package com.tangem.data.swap.converter.transaction

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.swap.models.SwapStatusDTO
import com.tangem.data.swap.models.SwapTransactionDTO
import com.tangem.data.swap.models.SwapTxTypeDTO
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapTransactionModel
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer
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
        status = value.status?.let(statusConverter::convert),
        swapTxType = SwapTxTypeDTO.entries.firstOrNull {
            it.name.lowercase() == value.swapTxType?.name?.lowercase()
        },
    )

    override fun convertBack(value: SwapTransactionDTO) = SwapTransactionModel(
        txId = value.txId,
        timestamp = value.timestamp,
        fromCryptoAmount = value.fromCryptoAmount,
        toCryptoAmount = value.toCryptoAmount,
        provider = value.provider,
        status = value.status?.let(statusConverter::convertBack),
        swapTxType = SwapTxType.entries.firstOrNull {
            it.name.lowercase() == value.swapTxType?.name?.lowercase()
        },
    )

    fun convertBack(
        value: SwapTransactionDTO,
        userWallet: UserWallet,
        txStatuses: Map<String, SwapStatusDTO>,
    ): SwapTransactionModel {
        val status = txStatuses[value.txId]
        val refundCurrency = status?.refundTokensResponse?.let { id ->
            val blockchain = Blockchain.fromNetworkId(id.networkId) ?: return@let null
            val derivationPath = id.derivationPath ?: return@let null

            val accountIndex = if (blockchain == Blockchain.Chia) {
                DerivationIndex.Main
            } else {
                val recognizer = AccountNodeRecognizer(blockchain = blockchain)
                val index = recognizer.recognize(derivationPathValue = derivationPath)?.toInt()
                    ?: return@let null

                DerivationIndex(index).getOrNull() ?: return@let null
            }

            responseCryptoCurrenciesFactory.createCurrency(
                responseToken = id,
                userWallet = userWallet,
                accountIndex = accountIndex,
            )
        }
        val statusWithRefundCurrency = status?.copy(refundCurrency = refundCurrency)

        return SwapTransactionModel(
            txId = value.txId,
            timestamp = value.timestamp,
            fromCryptoAmount = value.fromCryptoAmount,
            toCryptoAmount = value.toCryptoAmount,
            provider = value.provider,
            status = statusWithRefundCurrency?.let(statusConverter::convertBack),
            swapTxType = SwapTxType.entries.firstOrNull {
                it.name.lowercase() == value.swapTxType?.name?.lowercase()
            },
        )
    }
}