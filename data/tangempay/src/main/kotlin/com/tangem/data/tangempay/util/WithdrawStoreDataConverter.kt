package com.tangem.data.tangempay.util

import com.tangem.data.tangempay.entity.ExchangeStoreData
import com.tangem.data.tangempay.entity.WithdrawStoreData
import com.tangem.domain.tangempay.TangemPayWithdrawState
import com.tangem.utils.converter.Converter

class WithdrawStoreDataConverter : Converter<TangemPayWithdrawState, WithdrawStoreData> {

    override fun convert(value: TangemPayWithdrawState): WithdrawStoreData = WithdrawStoreData(
        orderId = value.orderId,
        exchangeData = value.exchangeData?.let { exchangeData ->
            ExchangeStoreData(
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