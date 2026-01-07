package com.tangem.data.transaction.convertes

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
 */
class GaslessTxDataToGaslessRequestConverter : Converter<GaslessTransactionData, GaslessTransactionDataDTO> {

    override fun convert(value: GaslessTransactionData): GaslessTransactionDataDTO {
        return GaslessTransactionDataDTO(
            transaction = convertTransaction(value.transaction),
            fee = convertFee(value.fee),
            nonce = value.nonce.toString(),
        )
    }

    private fun convertTransaction(transaction: GaslessTransactionData.Transaction): TransactionData {
        return TransactionData(
            to = transaction.to,
            value = transaction.value.toString(),
            data = transaction.data.toHexString(),
        )
    }

    private fun convertFee(fee: GaslessTransactionData.Fee): FeeData {
        return FeeData(
            feeToken = fee.feeToken,
            maxTokenFee = fee.maxTokenFee.toString(),
            coinPriceInToken = fee.coinPriceInToken.toString(),
            feeTransferGasLimit = fee.feeTransferGasLimit.toString(),
            baseGas = fee.baseGas.toString(),
        )
    }
}