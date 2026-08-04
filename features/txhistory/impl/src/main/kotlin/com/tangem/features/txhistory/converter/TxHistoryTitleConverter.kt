package com.tangem.features.txhistory.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.impl.R

/**
 * Single source of truth for an on-chain [TxInfo]'s human-readable title, dispatched on [TransactionType].
 *
 * Shared by the history list (row content title and status-pill label) and the transaction-details header, so the two
 * never drift apart. Pill types (Approve / Staking.* / YieldSupply Enter|Exit) resolve to the status-aware pill label
 * text only — the same wording shown in the list pill, without its trailing amount (e.g. "Staking", "Restaking",
 * "Yield mode"). Content types reproduce the per-type row titles.
 */
internal class TxHistoryTitleConverter {

    /**
     * @param tx            transaction whose title to resolve.
     * @param isOwnTransfer for a [TransactionType.Transfer] only — whether the counterparty is one of the user's own
     *                      accounts/wallets, which selects the "Transfer" title over "Send" / "Receive".
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun convert(tx: TxInfo, isOwnTransfer: Boolean = false): TextReference = when (val type = tx.type) {
        // region Pills — the label text only (no amount), matching what the list pill shows
        is TransactionType.Approve -> tx.pillTitle(ApproveSpec)
        is TransactionType.Staking.Stake -> tx.pillTitle(StakeSpec)
        is TransactionType.Staking.Unstake -> tx.pillTitle(UnstakeSpec)
        is TransactionType.Staking.Restake -> tx.pillTitle(RestakeSpec)
        is TransactionType.Staking.Vote -> tx.pillTitle(VoteSpec)
        is TransactionType.Staking.Withdraw -> tx.pillTitle(WithdrawSpec)
        is TransactionType.YieldSupply.Enter -> tx.pillTitle(YieldEnterSpec)
        is TransactionType.YieldSupply.Exit -> tx.pillTitle(YieldExitSpec)
        // endregion

        // region Content
        is TransactionType.Operation -> stringReference(type.name)
        is TransactionType.Swap -> tx.statusAwareTitle(R.string.common_swapping, R.string.common_swapped)
        is TransactionType.Transfer -> tx.transferTitle(isOwnTransfer)
        is TransactionType.Staking.ClaimRewards -> tx.statusAwareTitle(
            pending = R.string.transaction_history_claiming_reward,
            confirmed = R.string.transaction_history_staking_reward,
        )
        is TransactionType.YieldSupply.Topup -> resourceReference(R.string.yield_module_transaction_topup)
        is TransactionType.YieldSupply.Send -> if (type.isYieldSupplyWithdraw) {
            resourceReference(R.string.yield_module_transaction_withdraw)
        } else {
            resourceReference(R.string.common_transfer)
        }
        is TransactionType.YieldSupply.DeployContract ->
            resourceReference(R.string.yield_module_transaction_deploy_contract)
        is TransactionType.YieldSupply.InitializeToken ->
            resourceReference(R.string.yield_module_transaction_initialize)
        is TransactionType.YieldSupply.ReactivateToken ->
            resourceReference(R.string.yield_module_transaction_reactivate)
        is TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
        is TransactionType.GaslessFee -> resourceReference(R.string.gasless_transaction_fee)
        // endregion
    }

    private fun TxInfo.transferTitle(isOwnTransfer: Boolean): TextReference = when {
        isOwnTransfer -> statusAwareTitle(R.string.common_transfer, R.string.common_transferred)
        isOutgoing -> statusAwareTitle(R.string.common_sending, R.string.common_sent)
        else -> statusAwareTitle(R.string.common_receiving, R.string.common_received)
    }

    private fun TxInfo.pillTitle(spec: PillSpec): TextReference = spec.labels.resolve(status.toUiStatus())
}