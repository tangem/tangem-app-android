package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.utils.converter.Converter

internal object CollectibleActiveTxConverter : Converter<CollectibleTxResponse, CollectibleActiveTx> {

    override fun convert(value: CollectibleTxResponse): CollectibleActiveTx = CollectibleActiveTx(
        txId = value.txId,
        status = CollectibleTxStatusConverter.convert(value),
        machineId = value.machine?.externalId,
        isTurboModeEnabled = value.isTurboModeEnabled,
    )
}