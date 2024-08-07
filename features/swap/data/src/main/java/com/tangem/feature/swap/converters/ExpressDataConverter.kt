package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.ExchangeDataResponse
import com.tangem.datasource.api.express.models.response.ExchangeDataResponseWithTxDetails
import com.tangem.datasource.api.express.models.response.TxDetails
import com.tangem.datasource.api.express.models.response.TxType
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class ExpressDataConverter : Converter<ExchangeDataResponseWithTxDetails, SwapDataModel> {

    override fun convert(value: ExchangeDataResponseWithTxDetails): SwapDataModel {
        val data = value.dataResponse
        return SwapDataModel(
            toTokenAmount = createFromAmountWithOffset(data.toAmount, data.toDecimals),
            transaction = convertTransaction(value.txDetails, data),
        )
    }

    private fun convertTransaction(
        transactionDto: TxDetails,
        dataResponse: ExchangeDataResponse,
    ): ExpressTransactionModel {
        return if (transactionDto.txType == TxType.SWAP) {
            val otherNativeFeeWei = transactionDto.otherNativeFee?.let {
                if (it == "0") {
                    BigDecimal.ZERO
                } else {
                    requireNotNull(it.toBigDecimalOrNull()) { "wrong amount format, use only digits" }
                }
            }
            ExpressTransactionModel.DEX(
                fromAmount = createFromAmountWithOffset(dataResponse.fromAmount, dataResponse.fromDecimals),
                toAmount = createFromAmountWithOffset(dataResponse.toAmount, dataResponse.toDecimals),
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
            ExpressTransactionModel.CEX(
                fromAmount = createFromAmountWithOffset(dataResponse.fromAmount, dataResponse.fromDecimals),
                toAmount = createFromAmountWithOffset(dataResponse.toAmount, dataResponse.toDecimals),
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
