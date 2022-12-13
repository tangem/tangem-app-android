package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.BaseOneInchResponse
import com.tangem.datasource.api.oneinch.models.SwapResponse
import com.tangem.datasource.api.oneinch.models.TransactionDto
import com.tangem.feature.swap.domain.models.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.data.SwapDataModel
import com.tangem.feature.swap.domain.models.data.TransactionModel
import com.tangem.feature.swap.domain.models.data.mapErrors
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class SwapConverter @Inject constructor() : Converter<BaseOneInchResponse<SwapResponse>, AggregatedSwapDataModel<SwapDataModel>> {

    override fun convert(value: BaseOneInchResponse<SwapResponse>): AggregatedSwapDataModel<SwapDataModel> {
        val body = value.body
        return if (body != null) {
            AggregatedSwapDataModel(
                SwapDataModel(
                    fromTokenAddress = body.fromToken.address,
                    toTokenAddress = body.toToken.address,
                    toTokenAmount = body.fromTokenAmount,
                    fromTokenAmount = body.toTokenAmount,
                    transaction = convertTransaction(body.transaction),
                ),
            )
        } else {
            AggregatedSwapDataModel(
                null,
                mapErrors(value.errorDto?.description),
            )
        }
    }

    private fun convertTransaction(transactionDto: TransactionDto): TransactionModel {
        return TransactionModel(
            fromWalletAddress = transactionDto.fromAddress,
            toWalletAddress = transactionDto.toAddress,
            data = transactionDto.data,
            value = transactionDto.value,
            gasPrice = transactionDto.gasPrice,
            gas = transactionDto.gas,
        )
    }
}