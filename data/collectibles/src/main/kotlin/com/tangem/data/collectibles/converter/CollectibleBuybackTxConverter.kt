package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleBuybackTx
import com.tangem.grow.datasource.collectibles.models.response.GenerateBuybackDataResponse
import com.tangem.utils.converter.Converter

internal object CollectibleBuybackTxConverter : Converter<GenerateBuybackDataResponse, CollectibleBuybackTx> {

    override fun convert(value: GenerateBuybackDataResponse): CollectibleBuybackTx = with(value.txData) {
        CollectibleBuybackTx(
            buybackId = value.buybackId,
            memo = memo,
            amount = toAmount.toBigDecimal().movePointLeft(toDecimals),
            transaction = tx,
        )
    }
}