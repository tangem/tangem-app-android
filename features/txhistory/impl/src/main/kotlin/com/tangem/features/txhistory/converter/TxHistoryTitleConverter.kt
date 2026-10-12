package com.tangem.features.txhistory.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.impl.R

/**
 * Single source of truth for an on-chain [TxInfo]'s human-readable title, dispatched on [TransactionType].
 *
 * Shared by the history list (row content title and status-pill label) and the transaction-details header, so the two
 * never drift apart. Pill types (Approve / Staking.* / YieldSupply Enter|Exit) resolve to the status-aware pill label
 * text only — the same wording shown in the list pill, without its trailing amount (e.g. "Staking", "Restaking",
 * "Yield mode"); Approve is the exception, see its branch. Content types reproduce the per-type row titles.
 */
internal class TxHistoryTitleConverter(private val currency: CryptoCurrency) {

    /**
     * @param tx            transaction whose title to resolve.
     * @param isOwnTransfer for a plain transfer only ([TransactionType.Transfer] and the non-withdraw
     *                      [TransactionType.YieldSupply.Send]) — whether the counterparty is one of the user's own
     *                      accounts/wallets, which selects the "Transfer" title over "Send" / "Receive".
     */
    fun convert(tx: TxInfo, isOwnTransfer: Boolean = false): TextReference {
        // An unrecognized call that moved no coin is titled by what it really was — the fee it cost.
        if (tx.isFeeOnlyOperation(currency)) return resourceReference(R.string.common_network_fee_title)
        return convertByType(tx, isOwnTransfer)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun convertByType(tx: TxInfo, isOwnTransfer: Boolean): TextReference = when (val type = tx.type) {
        // region Pills — the label text only (no amount), matching what the list pill shows
        is TransactionType.Approve -> tx.statusAwareTitle(
            pending = R.string.common_approving,
            confirmed = R.string.common_approved,
            failed = R.string.transaction_history_status_approve_failed,
        )
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
        is TransactionType.Swap -> tx.statusAwareTitle(
            pending = R.string.common_swapping,
            confirmed = R.string.common_swapped,
            failed = R.string.transaction_history_status_swap_failed,
        )
        is TransactionType.Transfer -> tx.transferTitle(isOwnTransfer)
        is TransactionType.Staking.ClaimRewards -> tx.statusAwareTitle(
            pending = R.string.transaction_history_status_claiming_rewards,
            confirmed = R.string.transaction_history_status_rewards_claimed,
            failed = R.string.transaction_history_status_rewards_claim_failed,
        )
        // A topup is the mirror of a withdraw — funds into the Aave position — and reads as the same kind of
        // status-aware verb.
        is TransactionType.YieldSupply.Topup -> tx.statusAwareTitle(
            pending = R.string.common_supplying,
            confirmed = R.string.transaction_history_supplied,
            failed = R.string.transaction_history_status_supply_failed,
        )
        // A withdraw takes the funds out of Aave → Withdrawn. Any other Send is a plain transfer the yield module
        // made from the user's own address, so its counterparty can be another portfolio of theirs — it reads exactly
        // like Transfer.
        is TransactionType.YieldSupply.Send -> if (type.isYieldSupplyWithdraw) {
            tx.statusAwareTitle(
                pending = R.string.common_withdrawing,
                confirmed = R.string.transaction_history_withdrawn,
                failed = R.string.transaction_history_status_withdraw_failed,
            )
        } else {
            tx.transferTitle(isOwnTransfer)
        }
        is TransactionType.YieldSupply.DeployContract ->
            resourceReference(R.string.yield_module_transaction_deploy_contract)
        is TransactionType.YieldSupply.InitializeToken ->
            resourceReference(R.string.yield_module_transaction_initialize)
        is TransactionType.YieldSupply.ReactivateToken ->
            resourceReference(R.string.yield_module_transaction_reactivate)
        is TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
        is TransactionType.GaslessFee,
        is TransactionType.NetworkFee,
        -> resourceReference(R.string.common_network_fee_title)
        // endregion
    }

    private fun TxInfo.transferTitle(isOwnTransfer: Boolean): TextReference = when {
        isOwnTransfer -> statusAwareTitle(
            pending = R.string.transaction_history_status_transferring,
            confirmed = R.string.common_transferred,
            failed = R.string.transaction_history_status_transfer_failed,
        )
        else -> sendReceiveTitle()
    }

    private fun TxInfo.sendReceiveTitle(): TextReference = if (isOutgoing) {
        statusAwareTitle(
            pending = R.string.common_sending,
            confirmed = R.string.common_sent,
            failed = R.string.transaction_history_status_send_failed,
        )
    } else {
        statusAwareTitle(
            pending = R.string.common_receiving,
            confirmed = R.string.common_received,
            failed = R.string.transaction_history_status_receive_failed,
        )
    }

    private fun TxInfo.pillTitle(spec: PillSpec): TextReference = spec.labels.resolve(status.toUiStatus())
}