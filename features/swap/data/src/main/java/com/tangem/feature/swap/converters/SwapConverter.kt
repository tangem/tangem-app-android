package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.models.SwapResponse
import com.tangem.feature.swap.domain.models.SwapTransactionModel
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class SwapConverter @Inject constructor() : Converter<SwapResponse, SwapTransactionModel> {

    override fun convert(value: SwapResponse): SwapTransactionModel {
        return SwapTransactionModel(
            fromTokenAddress = value.fromToken.address,
            toTokenAddress = value.toToken.address,
            toTokenAmount = value.fromTokenAmount,
            fromTokenAmount = value.toTokenAmount,
            fromWalletAddress = value.transaction.fromAddress,
            toWalletAddress = value.transaction.toAddress,
            data = value.transaction.data,
            value = value.transaction.value,
            gasPrice = value.transaction.gasPrice,
            gas = value.transaction.gas,
        )
    }
}