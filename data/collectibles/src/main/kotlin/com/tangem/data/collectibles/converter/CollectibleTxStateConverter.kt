package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleTxState
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.utils.converter.Converter

internal object CollectibleTxStateConverter : Converter<CollectibleTxResponse, CollectibleTxState> {

    override fun convert(value: CollectibleTxResponse): CollectibleTxState = CollectibleTxState(
        status = CollectibleTxStatusConverter.convert(value),
        pack = value.nftWon?.let { nft ->
            OpenedPackConverter.convert(OpenedPackConverter.Input(txId = value.txId, nft = nft))
        },
        autoBuybackAmount = value.autoBuyback?.let { it.toAmount.toBigDecimalOrNull()?.movePointLeft(it.toDecimals) },
    )
}