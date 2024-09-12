package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TransactionState.Content.Direction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat
import com.tangem.utils.toFormattedCurrencyString

internal class TokenDetailsTxHistoryTransactionStateConverter(
    private val symbol: String,
    private val decimals: Int,
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<TxHistoryItem, TransactionState> {

    override fun convert(value: TxHistoryItem): TransactionState {
        return createTransactionStateItem(item = value)
    }

    @Suppress("LongMethod")
    private fun createTransactionStateItem(item: TxHistoryItem): TransactionState {
        return TransactionState.Content(
            txHash = item.txHash,
            amount = item.getAmount(),
            time = item.timestampInMillis.toTimeFormat(),
            status = item.status.tiUiStatus(),
            direction = item.extractDirection(),
            iconRes = item.extractIcon(),
            title = item.extractTitle(),
            subtitle = item.extractSubtitle(),
            timestamp = item.timestampInMillis,
            onClick = { clickIntents.onTransactionClick(item.txHash) },
        )
    }

    private fun TxHistoryItem.extractIcon(): Int = if (status == TxHistoryItem.TransactionStatus.Failed) {
        R.drawable.ic_close_24
    } else {
        when (type) {
            is TxHistoryItem.TransactionType.Approve -> R.drawable.ic_doc_24
            is TxHistoryItem.TransactionType.TronStakingTransactionType.Stake,
            is TxHistoryItem.TransactionType.TronStakingTransactionType.Vote,
            -> R.drawable.ic_transaction_history_staking
            is TxHistoryItem.TransactionType.TronStakingTransactionType.Withdraw,
            is TxHistoryItem.TransactionType.TronStakingTransactionType.Unstake,
            -> R.drawable.ic_transaction_history_unstaking
            is TxHistoryItem.TransactionType.Operation,
            is TxHistoryItem.TransactionType.Swap,
            is TxHistoryItem.TransactionType.Transfer,
            is TxHistoryItem.TransactionType.UnknownOperation,
            -> if (isOutgoing) R.drawable.ic_arrow_up_24 else R.drawable.ic_arrow_down_24
        }
    }

    private fun TxHistoryItem.extractTitle(): TextReference = when (val type = type) {
        is TxHistoryItem.TransactionType.Approve -> resourceReference(R.string.common_approval)
        is TxHistoryItem.TransactionType.Operation -> stringReference(type.name)
        is TxHistoryItem.TransactionType.Swap -> resourceReference(R.string.common_swap)
        is TxHistoryItem.TransactionType.Transfer -> resourceReference(R.string.common_transfer)
        is TxHistoryItem.TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
        is TxHistoryItem.TransactionType.TronStakingTransactionType.Stake -> resourceReference(R.string.common_staking)
        is TxHistoryItem.TransactionType.TronStakingTransactionType.Unstake -> resourceReference(
            R.string.staking_unstaking,
        )
        is TxHistoryItem.TransactionType.TronStakingTransactionType.Vote -> stringReference("Vote")
        is TxHistoryItem.TransactionType.TronStakingTransactionType.Withdraw -> stringReference("Withdraw")
    }

    private fun TxHistoryItem.extractSubtitle(): TextReference =
        when (val interactionAddress = interactionAddressType) {
            is TxHistoryItem.InteractionAddressType.Contract -> resourceReference(
                id = R.string.transaction_history_contract_address,
                formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
            )
            is TxHistoryItem.InteractionAddressType.Multiple -> resourceReference(
                id = if (isOutgoing) {
                    R.string.transaction_history_transaction_to_address
                } else {
                    R.string.transaction_history_transaction_from_address
                },
                formatArgs = wrappedList(resourceReference(R.string.transaction_history_multiple_addresses)),
            )
            is TxHistoryItem.InteractionAddressType.User -> resourceReference(
                id = if (isOutgoing) {
                    R.string.transaction_history_transaction_to_address
                } else {
                    R.string.transaction_history_transaction_from_address
                },
                formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
            )
            is TxHistoryItem.InteractionAddressType.Staking -> resourceReference(
                id = R.string.common_staking,
            )
        }

    private fun TxHistoryItem.extractDirection() = if (isOutgoing) Direction.OUTGOING else Direction.INCOMING

    private fun TxHistoryItem.TransactionStatus.tiUiStatus() = when (this) {
        TxHistoryItem.TransactionStatus.Confirmed -> TransactionState.Content.Status.Confirmed
        TxHistoryItem.TransactionStatus.Failed -> TransactionState.Content.Status.Failed
        TxHistoryItem.TransactionStatus.Unconfirmed -> TransactionState.Content.Status.Unconfirmed
    }

    private fun TxHistoryItem.getAmount(): String {
        if (type == TxHistoryItem.TransactionType.TronStakingTransactionType.Vote ||
            type == TxHistoryItem.TransactionType.TronStakingTransactionType.Withdraw
        ) {
            return ""
        }
        val prefix = when {
            status == TxHistoryItem.TransactionStatus.Failed -> ""
            this.amount.isZero() -> ""
            else -> if (isOutgoing) MINUS else PLUS
        }
        return prefix + amount.toFormattedCurrencyString(currency = symbol, decimals = decimals)
    }
}
