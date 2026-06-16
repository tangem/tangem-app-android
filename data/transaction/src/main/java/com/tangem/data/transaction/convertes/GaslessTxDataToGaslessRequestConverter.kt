package com.tangem.data.transaction.convertes

import com.tangem.blockchain.extensions.formatHex
import com.tangem.datasource.api.gasless.models.FeeData
import com.tangem.datasource.api.gasless.models.TransactionData
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.toHexString
import com.tangem.datasource.api.gasless.models.GaslessTransactionData as GaslessTransactionDataDTO

/**
 * Converts domain GaslessTransactionData to DTO for API requests.
 * Note: This converter only handles the transaction data conversion.
 * Additional fields (signature, userAddress, chainId) must be added separately
 * to create complete GaslessTransactionRequest.
 *
 * @param shouldIncludeGasLimit when true (v2), serializes the per-call `gasLimit`; when false (v1), omits it so the
 *        request matches the legacy v1 service. Must stay in sync with the EIP-712 message that was signed.
 */
class GaslessTxDataToGaslessRequestConverter(
    private val shouldIncludeGasLimit: Boolean = true,
) : Converter<GaslessTransactionData, GaslessTransactionDataDTO> {

    override fun convert(value: GaslessTransactionData): GaslessTransactionDataDTO {
        return GaslessTransactionDataDTO(
            transaction = convertTransaction(value.transaction),
            fee = convertFee(value.fee),
            nonce = value.nonce.toString(),
        )
    }

    internal fun convertTransaction(transaction: GaslessTransactionData.Transaction): TransactionData {
        return TransactionData(
            to = transaction.to,
            value = transaction.value.toString(),
            gasLimit = transaction.gasLimit.toString().takeIf { shouldIncludeGasLimit },
            data = transaction.data.toHexString().formatHex(),
        )
    }

    internal fun convertFee(fee: GaslessTransactionData.Fee): FeeData {
        return FeeData(
            feeToken = fee.feeToken,
            maxTokenFee = fee.maxTokenFee.toString(),
            coinPriceInToken = fee.coinPriceInToken.toString(),
            feeTransferGasLimit = fee.feeTransferGasLimit.toString(),
            baseGas = fee.baseGas.toString(),
            feeReceiver = fee.feeReceiver,
        )
    }
}