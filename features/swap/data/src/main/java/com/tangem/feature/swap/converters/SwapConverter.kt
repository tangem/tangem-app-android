package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.models.SwapResponse
import com.tangem.datasource.api.oneinch.models.TransactionDto
import com.tangem.feature.swap.domain.models.data.SwapDataModel
import com.tangem.feature.swap.domain.models.data.TransactionModel
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class SwapConverter @Inject constructor() : Converter<SwapResponse, SwapDataModel> {

    override fun convert(value: SwapResponse): SwapDataModel {
        return SwapDataModel(
            fromTokenAddress = value.fromToken.address,
            toTokenAddress = value.toToken.address,
            toTokenAmount = value.fromTokenAmount,
            fromTokenAmount = value.toTokenAmount,
            transaction = convertTransaction(value.transaction),
        )
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
