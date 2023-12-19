package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.ExchangeDataResponse
import com.tangem.datasource.api.express.models.response.TxType
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.utils.converter.Converter

class ExpressDataConverter : Converter<ExchangeDataResponse, SwapDataModel> {

    override fun convert(value: ExchangeDataResponse): SwapDataModel {
        return SwapDataModel(
            toTokenAmount = createFromAmountWithOffset(value.toAmount, value.toDecimals),
            transaction = convertTransaction(value),
        )
    }

    private fun convertTransaction(transactionDto: ExchangeDataResponse): ExpressTransactionModel {
        return if (transactionDto.txType == TxType.SWAP) {
            ExpressTransactionModel.DEX(
                fromAmount = createFromAmountWithOffset(transactionDto.fromAmount, transactionDto.fromDecimals),
                toAmount = createFromAmountWithOffset(transactionDto.toAmount, transactionDto.toDecimals),
                txId = transactionDto.txId,
                txTo = transactionDto.txTo,
                txFrom = requireNotNull(transactionDto.txFrom),
                txData = requireNotNull(transactionDto.txData),
            )
        } else {
            ExpressTransactionModel.CEX(
                fromAmount = createFromAmountWithOffset(transactionDto.fromAmount, transactionDto.fromDecimals),
                toAmount = createFromAmountWithOffset(transactionDto.toAmount, transactionDto.toDecimals),
                txId = transactionDto.txId,
                txTo = transactionDto.txTo,
                externalTxId = requireNotNull(transactionDto.externalTxId),
                externalTxUrl = requireNotNull(transactionDto.externalTxUrl),
                txExtraIdName = transactionDto.txExtraIdName,
                txExtraId = transactionDto.txExtraId,
            )
        }
    }
}