package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.toBriefAddressFormat

internal class TxHistoryItemToTransactionStateConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<TxHistoryItem, TransactionState> {
    override fun convert(value: TxHistoryItem): TransactionState {
        return TransactionState.Content(
            txHash = value.txHash,
            amount = value.getAmount(),
            time = value.timestampInMillis.toTimeFormat(),
            status = value.status.tiUiStatus(),
            direction = value.extractDirection(),
            iconRes = value.extractIcon(),
            title = value.extractTitle(),
            subtitle = value.extractSubtitle(),
            timestamp = value.timestampInMillis,
            onClick = { txHistoryUiActions.openTxInExplorer(value.txHash) },
        )
    }

    private fun TxHistoryItem.extractIcon(): Int = if (status == TxHistoryItem.TransactionStatus.Failed) {
        R.drawable.ic_close_24
    } else {
        when (type) {
            is TxHistoryItem.TransactionType.Approve -> R.drawable.ic_doc_24
            is TxHistoryItem.TransactionType.Staking.Stake,
            is TxHistoryItem.TransactionType.Staking.Vote,
            is TxHistoryItem.TransactionType.Staking.Restake,
            -> R.drawable.ic_transaction_history_staking_24
            is TxHistoryItem.TransactionType.Staking.ClaimRewards,
            -> R.drawable.ic_transaction_history_claim_rewards_24
            is TxHistoryItem.TransactionType.Staking.Unstake,
            is TxHistoryItem.TransactionType.Staking.Withdraw,
            -> R.drawable.ic_transaction_history_unstaking_24
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
        is TxHistoryItem.TransactionType.Staking.Stake -> resourceReference(R.string.common_stake)
        is TxHistoryItem.TransactionType.Staking.Unstake -> resourceReference(R.string.common_unstake)
        is TxHistoryItem.TransactionType.Staking.Vote -> resourceReference(R.string.staking_vote)
        is TxHistoryItem.TransactionType.Staking.ClaimRewards -> resourceReference(R.string.common_claim_rewards)
        is TxHistoryItem.TransactionType.Staking.Withdraw -> resourceReference(R.string.staking_withdraw)
        is TxHistoryItem.TransactionType.Staking.Restake -> resourceReference(R.string.staking_restake)
        is TxHistoryItem.TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
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
            is TxHistoryItem.InteractionAddressType.Validator -> resourceReference(
                id = R.string.transaction_history_transaction_validator,
                formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
            )
            null -> {
                TextReference.EMPTY
            }
        }

    private fun TxHistoryItem.extractDirection() =
        if (isOutgoing) TransactionState.Content.Direction.OUTGOING else TransactionState.Content.Direction.INCOMING

    private fun TxHistoryItem.getAmount(): String {
        if (type is TxHistoryItem.TransactionType.Staking.Vote ||
            type == TxHistoryItem.TransactionType.Staking.ClaimRewards ||
            type == TxHistoryItem.TransactionType.Staking.Withdraw
        ) {
            return ""
        }
        val prefix = when {
            status == TxHistoryItem.TransactionStatus.Failed -> ""
            this.amount.isZero() -> ""
            else -> if (isOutgoing) StringsSigns.MINUS else StringsSigns.PLUS
        }
        return prefix + amount.format { crypto(symbol = currency.symbol, decimals = currency.decimals) }
    }

    private fun TxHistoryItem.TransactionStatus.tiUiStatus() = when (this) {
        TxHistoryItem.TransactionStatus.Confirmed -> TransactionState.Content.Status.Confirmed
        TxHistoryItem.TransactionStatus.Failed -> TransactionState.Content.Status.Failed
        TxHistoryItem.TransactionStatus.Unconfirmed -> TransactionState.Content.Status.Unconfirmed
    }
}