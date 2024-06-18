package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.ExchangeDataResponse
import com.tangem.datasource.api.express.models.response.ExchangeDataResponseWithTxDetails
import com.tangem.datasource.api.express.models.response.TxDetails
import com.tangem.datasource.api.express.models.response.TxType
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.utils.converter.Converter

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
            ExpressTransactionModel.DEX(
                fromAmount = createFromAmountWithOffset(dataResponse.fromAmount, dataResponse.fromDecimals),
                toAmount = createFromAmountWithOffset(dataResponse.toAmount, dataResponse.toDecimals),
                txId = dataResponse.txId,
                txTo = transactionDto.txTo,
                txFrom = requireNotNull(transactionDto.txFrom),
                txData = requireNotNull(transactionDto.txData),
                txExtraId = transactionDto.txExtraId,
            )
        } else {
            ExpressTransactionModel.CEX(
                fromAmount = createFromAmountWithOffset(dataResponse.fromAmount, dataResponse.fromDecimals),
                toAmount = createFromAmountWithOffset(dataResponse.toAmount, dataResponse.toDecimals),
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