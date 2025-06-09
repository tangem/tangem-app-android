package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.toBriefAddressFormat

internal class TxHistoryItemToTransactionStateConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<TxInfo, TransactionState> {
    override fun convert(value: TxInfo): TransactionState {
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

    private fun TxInfo.extractIcon(): Int = if (status == TxInfo.TransactionStatus.Failed) {
        R.drawable.ic_close_24
    } else {
        when (type) {
            is TxInfo.TransactionType.Approve -> R.drawable.ic_doc_24
            is TxInfo.TransactionType.Staking.Stake,
            is TxInfo.TransactionType.Staking.Vote,
            is TxInfo.TransactionType.Staking.Restake,
            -> R.drawable.ic_transaction_history_staking_24
            is TxInfo.TransactionType.Staking.ClaimRewards,
            -> R.drawable.ic_transaction_history_claim_rewards_24
            is TxInfo.TransactionType.Staking.Unstake,
            is TxInfo.TransactionType.Staking.Withdraw,
            -> R.drawable.ic_transaction_history_unstaking_24
            is TxInfo.TransactionType.Operation,
            is TxInfo.TransactionType.Swap,
            is TxInfo.TransactionType.Transfer,
            is TxInfo.TransactionType.UnknownOperation,
            -> if (isOutgoing) R.drawable.ic_arrow_up_24 else R.drawable.ic_arrow_down_24
        }
    }

    private fun TxInfo.extractTitle(): TextReference = when (val type = type) {
        is TxInfo.TransactionType.Approve -> resourceReference(R.string.common_approval)
        is TxInfo.TransactionType.Operation -> stringReference(type.name)
        is TxInfo.TransactionType.Swap -> resourceReference(R.string.common_swap)
        is TxInfo.TransactionType.Transfer -> resourceReference(R.string.common_transfer)
        is TxInfo.TransactionType.Staking.Stake -> resourceReference(R.string.common_stake)
        is TxInfo.TransactionType.Staking.Unstake -> resourceReference(R.string.common_unstake)
        is TxInfo.TransactionType.Staking.Vote -> resourceReference(R.string.staking_vote)
        is TxInfo.TransactionType.Staking.ClaimRewards -> resourceReference(R.string.common_claim_rewards)
        is TxInfo.TransactionType.Staking.Withdraw -> resourceReference(R.string.staking_withdraw)
        is TxInfo.TransactionType.Staking.Restake -> resourceReference(R.string.staking_restake)
        is TxInfo.TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
    }

    private fun TxInfo.extractSubtitle(): TextReference = when (val interactionAddress = interactionAddressType) {
        is TxInfo.InteractionAddressType.Contract -> resourceReference(
            id = R.string.transaction_history_contract_address,
            formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
        )
        is TxInfo.InteractionAddressType.Multiple -> resourceReference(
            id = if (isOutgoing) {
                R.string.transaction_history_transaction_to_address
            } else {
                R.string.transaction_history_transaction_from_address
            },
            formatArgs = wrappedList(resourceReference(R.string.transaction_history_multiple_addresses)),
        )
        is TxInfo.InteractionAddressType.User -> resourceReference(
            id = if (isOutgoing) {
                R.string.transaction_history_transaction_to_address
            } else {
                R.string.transaction_history_transaction_from_address
            },
            formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
        )
        is TxInfo.InteractionAddressType.Validator -> resourceReference(
            id = R.string.transaction_history_transaction_validator,
            formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
        )
        null -> {
            TextReference.EMPTY
        }
    }

    private fun TxInfo.extractDirection() =
        if (isOutgoing) TransactionState.Content.Direction.OUTGOING else TransactionState.Content.Direction.INCOMING

    private fun TxInfo.getAmount(): String {
        if (type is TxInfo.TransactionType.Staking.Vote ||
            type == TxInfo.TransactionType.Staking.ClaimRewards ||
            type == TxInfo.TransactionType.Staking.Withdraw
        ) {
            return ""
        }
        val prefix = when {
            status == TxInfo.TransactionStatus.Failed -> ""
            this.amount.isZero() -> ""
            else -> if (isOutgoing) StringsSigns.MINUS else StringsSigns.PLUS
        }
        return prefix + amount.format { crypto(symbol = currency.symbol, decimals = currency.decimals) }
    }

    private fun TxInfo.TransactionStatus.tiUiStatus() = when (this) {
        TxInfo.TransactionStatus.Confirmed -> TransactionState.Content.Status.Confirmed
        TxInfo.TransactionStatus.Failed -> TransactionState.Content.Status.Failed
        TxInfo.TransactionStatus.Unconfirmed -> TransactionState.Content.Status.Unconfirmed
    }
}