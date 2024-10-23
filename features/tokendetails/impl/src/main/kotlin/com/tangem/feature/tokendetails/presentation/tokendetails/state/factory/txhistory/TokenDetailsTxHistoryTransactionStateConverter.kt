package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TransactionState.Content.Direction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryItem.*
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat

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

    private fun TxHistoryItem.extractIcon(): Int = if (status == TransactionStatus.Failed) {
        R.drawable.ic_close_24
    } else {
        when (type) {
            is TransactionType.Approve -> R.drawable.ic_doc_24
            is TransactionType.TronStakingTransactionType.Stake,
            is TransactionType.TronStakingTransactionType.Vote,
            -> R.drawable.ic_transaction_history_staking_24
            is TransactionType.TronStakingTransactionType.ClaimRewards,
            -> R.drawable.ic_transaction_history_claim_rewards_24
            is TransactionType.TronStakingTransactionType.Unstake,
            is TransactionType.TronStakingTransactionType.Withdraw,
            -> R.drawable.ic_transaction_history_unstaking_24
            is TransactionType.Operation,
            is TransactionType.Swap,
            is TransactionType.Transfer,
            is TransactionType.UnknownOperation,
            -> if (isOutgoing) R.drawable.ic_arrow_up_24 else R.drawable.ic_arrow_down_24
        }
    }

    private fun TxHistoryItem.extractTitle(): TextReference = when (val type = type) {
        is TransactionType.Approve -> resourceReference(R.string.common_approval)
        is TransactionType.Operation -> stringReference(type.name)
        is TransactionType.Swap -> resourceReference(R.string.common_swap)
        is TransactionType.Transfer -> resourceReference(R.string.common_transfer)
        is TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
        is TransactionType.TronStakingTransactionType.Stake -> resourceReference(R.string.common_stake)
        is TransactionType.TronStakingTransactionType.Unstake -> resourceReference(R.string.common_unstake)
        is TransactionType.TronStakingTransactionType.Vote -> resourceReference(R.string.staking_vote)
        is TransactionType.TronStakingTransactionType.ClaimRewards -> resourceReference(R.string.common_claim_rewards)
        is TransactionType.TronStakingTransactionType.Withdraw -> resourceReference(R.string.staking_withdraw)
    }

    private fun TxHistoryItem.extractSubtitle(): TextReference =
        when (val interactionAddress = interactionAddressType) {
            is InteractionAddressType.Contract -> resourceReference(
                id = R.string.transaction_history_contract_address,
                formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
            )
            is InteractionAddressType.Multiple -> resourceReference(
                id = if (isOutgoing) {
                    R.string.transaction_history_transaction_to_address
                } else {
                    R.string.transaction_history_transaction_from_address
                },
                formatArgs = wrappedList(resourceReference(R.string.transaction_history_multiple_addresses)),
            )
            is InteractionAddressType.User -> resourceReference(
                id = if (isOutgoing) {
                    R.string.transaction_history_transaction_to_address
                } else {
                    R.string.transaction_history_transaction_from_address
                },
                formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
            )
            is InteractionAddressType.Validator -> resourceReference(
                id = R.string.transaction_history_transaction_validator,
                formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
            )
            null -> {
                TextReference.EMPTY
            }
        }

    private fun TxHistoryItem.extractDirection() = if (isOutgoing) Direction.OUTGOING else Direction.INCOMING

    private fun TransactionStatus.tiUiStatus() = when (this) {
        TransactionStatus.Confirmed -> TransactionState.Content.Status.Confirmed
        TransactionStatus.Failed -> TransactionState.Content.Status.Failed
        TransactionStatus.Unconfirmed -> TransactionState.Content.Status.Unconfirmed
    }

    private fun TxHistoryItem.getAmount(): String {
        if (type is TransactionType.TronStakingTransactionType.Vote ||
            type == TransactionType.TronStakingTransactionType.ClaimRewards ||
            type == TransactionType.TronStakingTransactionType.Withdraw
        ) {
            return ""
        }
        val prefix = when {
            status == TransactionStatus.Failed -> ""
            this.amount.isZero() -> ""
            else -> if (isOutgoing) MINUS else PLUS
        }
        return prefix + amount.format { crypto(symbol = symbol, decimals = decimals) }
    }
}
