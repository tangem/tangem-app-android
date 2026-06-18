package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.utils.toSyntheticTxInfo
import com.tangem.utils.converter.Converter

/**
 * Converts a merged [TxHistoryInfo] row to [TransactionItemUM], delegating to the on-chain
 * [TxHistoryItemToTransactionItemUMConverter]: on-chain rows convert their `TxInfo` directly, express
 * rows convert a synthesized `TxInfo` view (see [toSyntheticTxInfo]).
 */
internal class TxHistoryInfoToTransactionItemUMConverter(
    private val txInfoConverter: TxHistoryItemToTransactionItemUMConverter,
) : Converter<TxHistoryInfo, TransactionItemUM> {

    override fun convert(value: TxHistoryInfo): TransactionItemUM = when (value) {
        is OnChainTx -> convertOnChain(value)
        is ExpressTx -> txInfoConverter.convert(value.toSyntheticTxInfo())
    }

    private fun convertOnChain(value: OnChainTx): TransactionItemUM = when (value) {
        is OnChainTx.BSDK -> txInfoConverter.convert(value.txInfo)
    }
}