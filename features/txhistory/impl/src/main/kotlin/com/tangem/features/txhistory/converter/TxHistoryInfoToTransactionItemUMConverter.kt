package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.utils.converter.Converter

/**
 * Converts a merged [TxHistoryInfo] row to [TransactionItemUM]: on-chain rows convert their `TxInfo` via
 * [TxHistoryItemToTransactionItemUMConverter]; express rows map directly via [ExpressTxToTransactionItemUMConverter].
 */
internal class TxHistoryInfoToTransactionItemUMConverter(
    private val txInfoConverter: TxHistoryItemToTransactionItemUMConverter,
    private val expressConverter: ExpressTxToTransactionItemUMConverter,
) : Converter<TxHistoryInfo, TransactionItemUM> {

    override fun convert(value: TxHistoryInfo): TransactionItemUM = when (value) {
        is OnChainTx -> convertOnChain(value)
        is ExpressTx -> expressConverter.convert(value)
    }

    private fun convertOnChain(value: OnChainTx): TransactionItemUM = when (value) {
        is OnChainTx.BSDK -> txInfoConverter.convert(value.txInfo)
    }
}