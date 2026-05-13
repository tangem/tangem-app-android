package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.PillKind
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat

internal class TxHistoryStatusPillConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<TxHistoryStatusPillConverter.Input, TransactionItemUM.Pill> {

    override fun convert(value: Input): TransactionItemUM.Pill {
        val tx = value.tx
        val uiStatus = value.uiStatus
        val spec = value.spec
        val hasAmount = spec.amount.show(uiStatus)
        return TransactionItemUM.Pill(
            txHash = tx.txHash,
            kind = spec.kind,
            status = uiStatus,
            label = spec.labels.resolve(uiStatus),
            amount = if (hasAmount) {
                tx.amount.format { crypto(symbol = "", decimals = currency.decimals) }.trim()
            } else {
                null
            },
            currencySymbol = if (hasAmount) currency.symbol else null,
            subtitle = tx.buildPillSubtitle(uiStatus),
            timestamp = tx.timestampInMillis,
            onClick = { txHistoryUiActions.openTxInExplorer(tx.txHash) },
        )
    }

    data class Input(
        val tx: TxInfo,
        val uiStatus: TransactionItemUM.Content.Status,
        val spec: PillSpec,
    )
}

internal data class PillSpec(
    val kind: PillKind,
    val labels: PillLabels,
    val amount: PillAmount,
)

internal data class PillLabels(
    @StringRes val confirmed: Int,
    @StringRes val pending: Int,
    @StringRes val failedBase: Int = pending,
    val hasFailedTemplate: Boolean = true,
)

internal enum class PillAmount {
    ALWAYS, NEVER, IF_NOT_FAILED;

    fun show(status: TransactionItemUM.Content.Status): Boolean = when (this) {
        ALWAYS -> true
        NEVER -> false
        IF_NOT_FAILED -> status !is TransactionItemUM.Content.Status.Failed
    }
}

internal val ApproveSpec = PillSpec(
    kind = PillKind.APPROVE,
    labels = PillLabels(
        confirmed = R.string.common_approved,
        pending = R.string.common_approving,
        hasFailedTemplate = false,
    ),
    amount = PillAmount.ALWAYS,
)
internal val StakeSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(R.string.common_staked, R.string.common_staking),
    amount = PillAmount.IF_NOT_FAILED,
)
internal val UnstakeSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(R.string.staking_unstaked, R.string.staking_unstaking),
    amount = PillAmount.IF_NOT_FAILED,
)
internal val RestakeSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(
        confirmed = R.string.transaction_history_rewards_restaked,
        pending = R.string.transaction_history_rewards_restaking,
    ),
    amount = PillAmount.IF_NOT_FAILED,
)
internal val VoteSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(
        confirmed = R.string.staking_vote,
        pending = R.string.common_voting,
        failedBase = R.string.staking_vote,
    ),
    amount = PillAmount.NEVER,
)
internal val WithdrawSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(
        confirmed = R.string.staking_withdraw,
        pending = R.string.common_withdrawing,
        failedBase = R.string.staking_withdraw,
    ),
    amount = PillAmount.NEVER,
)
internal val YieldEnterSpec = PillSpec(
    kind = PillKind.YIELD_MODE,
    labels = PillLabels(
        confirmed = R.string.yield_module_transaction_enter,
        pending = R.string.yield_module_token_details_earn_notification_processing,
        failedBase = R.string.common_yield_mode,
    ),
    amount = PillAmount.NEVER,
)
internal val YieldExitSpec = PillSpec(
    kind = PillKind.YIELD_MODE,
    labels = PillLabels(
        confirmed = R.string.yield_module_transaction_exit,
        pending = R.string.transaction_history_disabling_yield_mode,
    ),
    amount = PillAmount.NEVER,
)

private fun TxInfo.buildPillSubtitle(status: TransactionItemUM.Content.Status): TransactionItemUM.PillSubtitle? {
    if (type !is TransactionType.Approve) return null
    if (status is TransactionItemUM.Content.Status.Failed) return null
    val address = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address ?: return null
    return TransactionItemUM.PillSubtitle.Address(
        rawAddress = address,
        briefAddress = address.toBriefAddressFormat(),
    )
}

private fun PillLabels.resolve(status: TransactionItemUM.Content.Status): TextReference = when (status) {
    is TransactionItemUM.Content.Status.Confirmed -> resourceReference(confirmed)
    is TransactionItemUM.Content.Status.Unconfirmed -> resourceReference(pending)
    is TransactionItemUM.Content.Status.Failed -> if (hasFailedTemplate) {
        resourceReference(R.string.common_action_failed, wrappedList(resourceReference(failedBase)))
    } else {
        resourceReference(failedBase)
    }
}