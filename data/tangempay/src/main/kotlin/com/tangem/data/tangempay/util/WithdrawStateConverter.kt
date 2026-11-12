package com.tangem.data.tangempay.util

import com.tangem.data.tangempay.entity.WithdrawStoreData
import com.tangem.domain.tangempay.TangemPayWithdrawExchangeState
import com.tangem.domain.tangempay.TangemPayWithdrawState
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
        createdAtMillis = value.createdAtMillis,
    )
}