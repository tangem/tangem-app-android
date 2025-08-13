package com.tangem.data.swap.converter

import com.tangem.datasource.api.express.models.response.ExchangeDataResponse
import com.tangem.datasource.api.express.models.response.ExchangeDataResponseWithTxDetails
import com.tangem.datasource.api.express.models.response.TxDetails
import com.tangem.datasource.api.express.models.response.TxType
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapDataTransactionModel
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class SwapDataConverter : Converter<ExchangeDataResponseWithTxDetails, SwapDataModel> {

    override fun convert(value: ExchangeDataResponseWithTxDetails): SwapDataModel {
        val data = value.dataResponse
        return SwapDataModel(
            toTokenAmount = requireNotNull(data.toAmount.toBigDecimalOrNull()?.movePointLeft(data.toDecimals)),
            transaction = convertTransaction(value.txDetails, data),
        )
    }

    private fun convertTransaction(
        transactionDto: TxDetails,
        dataResponse: ExchangeDataResponse,
    ): SwapDataTransactionModel {
        val fromAmount = requireNotNull(
            dataResponse.fromAmount.toBigDecimalOrNull()?.movePointLeft(dataResponse.fromDecimals),
        )
        val toAmount = requireNotNull(
            dataResponse.toAmount.toBigDecimalOrNull()?.movePointLeft(dataResponse.toDecimals),
        )

        return if (transactionDto.txType == TxType.SWAP) {
            val otherNativeFeeWei = transactionDto.otherNativeFee?.let {
                if (it == "0") {
                    BigDecimal.ZERO
                } else {
                    requireNotNull(it.toBigDecimalOrNull()) { "wrong amount format, use only digits" }
                }
            }
            SwapDataTransactionModel.DEX(
                fromAmount = fromAmount,
                toAmount = toAmount,
                txValue = transactionDto.txValue,
                txId = dataResponse.txId,
                txTo = transactionDto.txTo,
                txFrom = requireNotNull(transactionDto.txFrom),
                txData = requireNotNull(transactionDto.txData),
                txExtraId = transactionDto.txExtraId,
                otherNativeFeeWei = otherNativeFeeWei,
                gas = transactionDto.gas?.toBigIntegerOrNull() ?: error("gas is empty"),
            )
        } else {
            SwapDataTransactionModel.CEX(
                fromAmount = fromAmount,
                toAmount = toAmount,
                txValue = transactionDto.txValue,
                txId = dataResponse.txId,
                txTo = transactionDto.txTo,
                externalTxId = requireNotNull(transactionDto.externalTxId),
                externalTxUrl = requireNotNull(transactionDto.externalTxUrl),
                txExtraIdName = transactionDto.txExtraIdName,
                txExtraId = transactionDto.txExtraId,
            )
        }
    }
}