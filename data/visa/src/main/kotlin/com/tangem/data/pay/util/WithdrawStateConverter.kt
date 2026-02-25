package com.tangem.data.pay.util

import com.tangem.data.pay.entity.WithdrawStoreData
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.utils.converter.Converter

class WithdrawStateConverter : Converter<WithdrawStoreData, TangemPayWithdrawState> {

    override fun convert(value: WithdrawStoreData): TangemPayWithdrawState = TangemPayWithdrawState(
        orderId = value.orderId,
        exchangeData = value.exchangeData?.let { exchangeData ->
            TangemPayWithdrawExchangeState(
                txId = exchangeData.txId,
                fromNetwork = exchangeData.fromNetwork,
                fromAddress = exchangeData.fromAddress,
                payInAddress = exchangeData.payInAddress,
                payInExtraId = exchangeData.payInExtraId,
            )
        },
        txHash = value.txHash,
    )
}