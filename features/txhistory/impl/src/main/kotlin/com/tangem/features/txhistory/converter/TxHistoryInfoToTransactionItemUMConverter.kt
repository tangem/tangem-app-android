package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.converter.Converter

/**
 * Converts a merged [TxHistoryInfo] row to [TransactionItemUM].
 *
 * On-chain rows render via the plain [TxHistoryItemToTransactionItemUMConverter] (which knows only `TxInfo`); since
 * that converter cannot reference the merged row, the row click is bound here to the **incoming** [OnChainTx] so the
 * model resolves it in the live list by [TxHistoryInfo.txId]. Express rows carry their own [TxHistoryInfo] and wire
 * the click themselves in [ExpressTxToTransactionItemUMConverter].
 */
internal class TxHistoryInfoToTransactionItemUMConverter(
    private val txInfoConverter: TxHistoryItemToTransactionItemUMConverter,
    private val expressConverter: ExpressTxToTransactionItemUMConverter,
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<TxHistoryInfo, TransactionItemUM> {

    override fun convert(value: TxHistoryInfo): TransactionItemUM = when (value) {
        is OnChainTx -> convertOnChain(value)
        is ExpressTx -> expressConverter.convert(value)
    }

    private fun convertOnChain(value: OnChainTx): TransactionItemUM = when (value) {
        is OnChainTx.BSDK -> when (val um = txInfoConverter.convert(value.txInfo)) {
            // Content rows (transfer/swap/…) route through the details/explorer decision; pills stay on the explorer.
            is TransactionItemUM.Content -> um.copy(onClick = { txHistoryUiActions.onTransactionClick(value) })
            else -> um
        }
    }
}