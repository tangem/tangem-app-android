package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.*
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat

internal class TxHistoryItemStateConverter(
    private val symbol: String,
    private val decimals: Int,
    private val clickIntents: WalletClickIntents,
) : Converter<TxInfo, TransactionState> {

    override fun convert(value: TxInfo): TransactionState {
        return createTransactionStateItem(item = value)
    }

    @Suppress("LongMethod")
    private fun createTransactionStateItem(item: TxInfo): TransactionState {
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

    private fun TxInfo.extractIcon(): Int = if (status == TransactionStatus.Failed) {
        R.drawable.ic_close_24
    } else {
        when (type) {
            is TransactionType.Approve -> R.drawable.ic_doc_24
            is TransactionType.Staking.Stake,
            is TransactionType.Staking.Vote,
            is TransactionType.Staking.Restake,
            -> R.drawable.ic_transaction_history_staking_24
            is TransactionType.Staking.ClaimRewards,
            -> R.drawable.ic_transaction_history_claim_rewards_24
            is TransactionType.Staking.Unstake,
            is TransactionType.Staking.Withdraw,
            -> R.drawable.ic_transaction_history_unstaking_24
            is TransactionType.Operation,
            is TransactionType.Swap,
            is TransactionType.Transfer,
            is TransactionType.UnknownOperation,
            -> if (isOutgoing) R.drawable.ic_arrow_up_24 else R.drawable.ic_arrow_down_24
        }
    }

    private fun TxInfo.extractTitle(): TextReference = when (val type = type) {
        is TransactionType.Approve -> resourceReference(R.string.common_approval)
        is TransactionType.Operation -> stringReference(type.name)
        is TransactionType.Swap -> resourceReference(R.string.common_swap)
        is TransactionType.Transfer -> resourceReference(R.string.common_transfer)
        is TransactionType.Staking.Stake -> resourceReference(R.string.common_stake)
        is TransactionType.Staking.Unstake -> resourceReference(R.string.common_unstake)
        is TransactionType.Staking.Vote -> resourceReference(R.string.staking_vote)
        is TransactionType.Staking.ClaimRewards -> resourceReference(R.string.common_claim_rewards)
        is TransactionType.Staking.Withdraw -> resourceReference(R.string.staking_withdraw)
        is TransactionType.Staking.Restake -> resourceReference(R.string.staking_restake)
        is TransactionType.UnknownOperation -> resourceReference(R.string.transaction_history_operation)
    }

    private fun TxInfo.extractSubtitle(): TextReference = when (val interactionAddress = interactionAddressType) {
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

    private fun TxInfo.extractDirection() =
        if (isOutgoing) TransactionState.Content.Direction.OUTGOING else TransactionState.Content.Direction.INCOMING

    private fun TransactionStatus.tiUiStatus() = when (this) {
        TransactionStatus.Confirmed -> TransactionState.Content.Status.Confirmed
        TransactionStatus.Failed -> TransactionState.Content.Status.Failed
        TransactionStatus.Unconfirmed -> TransactionState.Content.Status.Unconfirmed
    }

    private fun TxInfo.getAmount(): String {
        if (type is TransactionType.Staking.Vote ||
            type == TransactionType.Staking.ClaimRewards ||
            type == TransactionType.Staking.Withdraw
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