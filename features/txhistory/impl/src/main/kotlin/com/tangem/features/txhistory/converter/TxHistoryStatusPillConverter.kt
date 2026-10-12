package com.tangem.features.txhistory.converter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.PillKind
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.converter.Converter

internal class TxHistoryStatusPillConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<TxHistoryStatusPillConverter.Input, TransactionItemUM.Pill> {

    override fun convert(value: Input): TransactionItemUM.Pill {
        val tx = value.tx
        val uiStatus = value.uiStatus
        val spec = value.spec
        // An Approve's amount/address are parameters of the granted allowance (TransactionType.Approve), not the
        // tx's own amount/interaction address, and they are denominated in the approved token — which is not the
        // viewed currency when the approval shows up in the coin history. A null allowance value means an unlimited
        // approval, shown as "Unlimited" in place of the number (mirrors the details screen's "Unlimited <symbol>"
        // + risk banner treatment).
        val allowance = (tx.type as? TransactionType.Approve)?.amount
        val allowanceValue = allowance?.value
        val hasAmount = spec.amount.show(uiStatus)
        val split = spec.confirmedSplit?.takeIf { uiStatus.isSettled() }
        return TransactionItemUM.Pill(
            txHash = tx.txHash,
            kind = spec.kind,
            status = uiStatus,
            label = split?.let { resourceReference(it.label) } ?: spec.labels.resolve(uiStatus),
            amount = when {
                split != null -> resourceReference(split.value)
                !hasAmount -> null
                allowance == null ->
                    stringReference(
                        tx.amount.format { crypto(symbol = "", decimals = currency.displayDecimals) }.trim(),
                    )
                allowanceValue == null -> resourceReference(R.string.transaction_history_unlimited)
                else -> stringReference(
                    allowanceValue.format { crypto(symbol = "", decimals = allowance.decimals) }.trim(),
                )
            },
            currencySymbol = if (hasAmount) allowance?.currencySymbol ?: currency.symbol else null,
            subtitle = tx.buildPillSubtitle(uiStatus, spec),
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
    /** Two-tone text of the settled pill; `null` renders [PillLabels.confirmed] in a single tone. */
    val confirmedSplit: PillConfirmedSplit? = null,
    /** DeFi provider named after the label ("Supplied **to: Aave**"); `null` for pills that name no provider. */
    val provider: PillProvider? = null,
)

/**
 * The provider tail of a yield pill: a constant of the operation, not of the transaction — the yield-supply product is
 * a single hard-wired integration (Aave), so the spec names it once instead of resolving it per tx.
 */
internal data class PillProvider(
    val direction: TransactionItemUM.ContentSubtitle.Direction,
    @StringRes val name: Int,
    @DrawableRes val iconResId: Int,
)

/**
 * Settled pill rendered in two tones: [label] in the muted label color and [value] in the primary one — the split the
 * pill already draws for an approve's allowance, reused to emphasize the state a yield-mode transaction ended in
 * ("Yield mode **Enabled**"). It replaces [PillLabels.confirmed], which stays the single-tone title of the details
 * card, and applies only to the settled statuses [PillLabels.resolve] answers [PillLabels.confirmed] for.
 */
internal data class PillConfirmedSplit(
    @StringRes val label: Int,
    @StringRes val value: Int,
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
    labels = PillLabels(
        confirmed = R.string.common_staked,
        pending = R.string.common_staking,
        failedBase = R.string.transaction_history_status_stake_failed,
        hasFailedTemplate = false,
    ),
    amount = PillAmount.IF_NOT_FAILED,
)
internal val UnstakeSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(
        confirmed = R.string.staking_unstaked,
        pending = R.string.staking_unstaking,
        failedBase = R.string.transaction_history_status_unstake_failed,
        hasFailedTemplate = false,
    ),
    amount = PillAmount.IF_NOT_FAILED,
)
internal val RestakeSpec = PillSpec(
    kind = PillKind.STAKING,
    labels = PillLabels(
        confirmed = R.string.transaction_history_rewards_restaked,
        pending = R.string.transaction_history_status_restaking_rewards,
        failedBase = R.string.transaction_history_status_rewards_restake_failed,
        hasFailedTemplate = false,
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
internal val YieldSupplySpec = PillSpec(
    kind = PillKind.YIELD_SUPPLY,
    labels = PillLabels(
        confirmed = R.string.yield_module_transaction_supplied,
        pending = R.string.common_supplying,
        failedBase = R.string.yield_module_supply,
    ),
    amount = PillAmount.NEVER,
    provider = PillProvider(
        direction = TransactionItemUM.ContentSubtitle.Direction.TO,
        name = R.string.yield_module_provider,
        iconResId = R.drawable.img_aave_22,
    ),
)
internal val YieldWithdrawSpec = PillSpec(
    kind = PillKind.YIELD_SUPPLY,
    labels = PillLabels(
        confirmed = R.string.transaction_history_withdrawn,
        pending = R.string.common_withdrawing,
        failedBase = R.string.common_withdrawal,
    ),
    amount = PillAmount.NEVER,
    provider = PillProvider(
        direction = TransactionItemUM.ContentSubtitle.Direction.FROM,
        name = R.string.yield_module_provider,
        iconResId = R.drawable.img_aave_22,
    ),
)
internal val YieldEnterSpec = PillSpec(
    kind = PillKind.YIELD_MODE,
    labels = PillLabels(
        confirmed = R.string.yield_module_transaction_enter,
        pending = R.string.transaction_history_enabling_yield_mode,
        failedBase = R.string.common_yield_mode_inline,
    ),
    amount = PillAmount.NEVER,
    confirmedSplit = PillConfirmedSplit(
        label = R.string.common_yield_mode_inline,
        value = R.string.common_enabled,
    ),
)
internal val YieldExitSpec = PillSpec(
    kind = PillKind.YIELD_MODE,
    labels = PillLabels(
        confirmed = R.string.yield_module_transaction_exit,
        pending = R.string.transaction_history_disabling_yield_mode,
    ),
    amount = PillAmount.NEVER,
    confirmedSplit = PillConfirmedSplit(
        label = R.string.common_yield_mode_inline,
        value = R.string.common_disabled,
    ),
)

/** The counterparty tail: a spec-wide provider, or the spender an approval granted its allowance to. */
private fun TxInfo.buildPillSubtitle(
    status: TransactionItemUM.Content.Status,
    spec: PillSpec,
): TransactionItemUM.PillSubtitle? {
    if (status is TransactionItemUM.Content.Status.Failed) return null
    spec.provider?.let { provider ->
        return TransactionItemUM.PillSubtitle.Provider(
            direction = provider.direction,
            name = resourceReference(provider.name),
            iconResId = provider.iconResId,
        )
    }
    val approve = type as? TransactionType.Approve ?: return null
    val address = approve.address.takeIf { it.isNotBlank() } ?: return null
    return TransactionItemUM.PillSubtitle.Address(rawAddress = address)
}

/** The statuses [resolve] answers [PillLabels.confirmed] for — the same ones [PillSpec.confirmedSplit] replaces. */
private fun TransactionItemUM.Content.Status.isSettled(): Boolean =
    this is TransactionItemUM.Content.Status.Confirmed || this is TransactionItemUM.Content.Status.PendingSignature

internal fun PillLabels.resolve(status: TransactionItemUM.Content.Status): TextReference = when (status) {
    is TransactionItemUM.Content.Status.Confirmed,
    is TransactionItemUM.Content.Status.PendingSignature,
    -> resourceReference(confirmed)
    is TransactionItemUM.Content.Status.Unconfirmed -> resourceReference(pending)
    is TransactionItemUM.Content.Status.Failed -> if (hasFailedTemplate) {
        resourceReference(R.string.common_action_failed, wrappedList(resourceReference(failedBase)))
    } else {
        resourceReference(failedBase)
    }
}