package com.tangem.features.txhistory.converter

import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.utils.converter.Converter

/**
 * Converts a [TxInfo] to a [TxHistoryDetailsUM] for the in-app transaction details card.
 *
 * Single dispatch on [TxInfo.type] picks the layout family — mirroring the same `when(type)` used by
 * [TxHistoryItemToTransactionItemUMConverter]:
 * - [TransactionType.Swap] (and onramp once it lands in `TxInfo`) -> [TxHistoryDetailsUM.TwoAssets]
 * - everything else -> [TxHistoryDetailsUM.SingleAsset]
 */
internal class TxInfoToTxHistoryDetailsUMConverter : Converter<TxInfo, TxHistoryDetailsUM> {

    override fun convert(value: TxInfo): TxHistoryDetailsUM = when (value.type) {
        is TransactionType.Swap -> twoAssets(value)
        else -> singleAsset(value)
    }

    private fun singleAsset(tx: TxInfo): TxHistoryDetailsUM.SingleAsset = TxHistoryDetailsUM.SingleAsset(
        title = tx.type.toString(),
    )

    private fun twoAssets(tx: TxInfo): TxHistoryDetailsUM.TwoAssets = TxHistoryDetailsUM.TwoAssets(
        title = tx.type.toString(),
    )
}