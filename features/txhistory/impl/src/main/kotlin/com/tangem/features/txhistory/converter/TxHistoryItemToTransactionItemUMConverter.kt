package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.converter.TxHistoryStatusPillConverter.Input as PillInput
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.resolveOwner
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.toBriefAddressFormat

/**
 * Converts [TxInfo] to [TransactionItemUM] for transaction history.
 *
 * Single dispatch: each [TransactionType] is mapped exactly once in [convert] to either a [TransactionItemUM.Pill]
 * or a [TransactionItemUM.Content]. Per-type metadata (labels, icons, subtitles) lives in one branch — no parallel
 * `when`s to keep in sync.
 *
 * The high cyclomatic complexity of [convert] is structural — it mirrors the [TransactionType] sealed hierarchy.
 * Splitting it would re-introduce the parallel-`when`s problem; the suppression is intentional.
 */
internal class TxHistoryItemToTransactionItemUMConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
    private val lookupContext: TxHistoryLookupContext? = null,
) : Converter<TxInfo, TransactionItemUM> {

    private val pillConverter = TxHistoryStatusPillConverter(currency, txHistoryUiActions)

    @Suppress("CyclomaticComplexMethod")
    override fun convert(value: TxInfo): TransactionItemUM {
        val uiStatus = value.status.toUiStatus()
        return when (val type = value.type) {
            // region Pill
            is TransactionType.Approve -> pillConverter.convert(PillInput(value, uiStatus, ApproveSpec))
            is TransactionType.Staking.Stake -> pillConverter.convert(PillInput(value, uiStatus, StakeSpec))
            is TransactionType.Staking.Unstake -> pillConverter.convert(PillInput(value, uiStatus, UnstakeSpec))
            is TransactionType.Staking.Restake -> pillConverter.convert(PillInput(value, uiStatus, RestakeSpec))
            is TransactionType.Staking.Vote -> pillConverter.convert(PillInput(value, uiStatus, VoteSpec))
            is TransactionType.Staking.Withdraw -> pillConverter.convert(PillInput(value, uiStatus, WithdrawSpec))
            is TransactionType.YieldSupply.Enter -> pillConverter.convert(PillInput(value, uiStatus, YieldEnterSpec))
            is TransactionType.YieldSupply.Exit -> pillConverter.convert(PillInput(value, uiStatus, YieldExitSpec))
            // endregion

            // region Content
            is TransactionType.Operation -> operationContent(value, uiStatus, type)
            is TransactionType.Swap -> swapContent(value, uiStatus)
            is TransactionType.Transfer -> transferContent(value, uiStatus)
            is TransactionType.Staking.ClaimRewards -> claimRewardsContent(value, uiStatus)
            is TransactionType.YieldSupply.Topup -> yieldTopupContent(value, uiStatus, type)
            is TransactionType.YieldSupply.Send -> yieldSendContent(value, uiStatus, type)
            is TransactionType.YieldSupply.DeployContract -> yieldDeployContractContent(value, uiStatus, type)
            is TransactionType.YieldSupply.InitializeToken -> yieldInitializeTokenContent(value, uiStatus, type)
            is TransactionType.YieldSupply.ReactivateToken -> yieldReactivateTokenContent(value, uiStatus, type)
            is TransactionType.UnknownOperation -> unknownOperationContent(value, uiStatus)
            is TransactionType.GaslessFee -> gaslessFeeContent(value, uiStatus)
            // endregion
        }
    }

    private fun operationContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.Operation,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = stringReference(type.name),
        icon = tx.directionalIcon(),
        subtitle = tx.extractAddressSubtitle(),
    )

    private fun swapContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = tx.statusAwareTitle(R.string.common_swapping, R.string.common_swapped),
            icon = tx.directionalIcon(),
            subtitle = tx.extractAddressSubtitle(),
        )

    private fun transferContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content {
        val counterpartyAddress = (tx.interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        val direction = if (tx.isOutgoing) ContentSubtitle.Direction.TO else ContentSubtitle.Direction.FROM
        val ownSubtitle = counterpartyAddress?.let { address ->
            resolveOwnSubtitle(
                lookupContext = lookupContext,
                networkRawId = currency.network.id.rawId,
                address = address,
                direction = direction,
            )
        }

        val title = when {
            ownSubtitle != null -> tx.statusAwareTitle(R.string.common_transfer, R.string.common_transferred)
            tx.isOutgoing -> tx.statusAwareTitle(R.string.common_sending, R.string.common_sent)
            else -> tx.statusAwareTitle(R.string.common_receiving, R.string.common_received)
        }

        val subtitle = ownSubtitle ?: when {
            counterpartyAddress != null -> ContentSubtitle.ExternalAddress(
                direction = direction,
                rawAddress = counterpartyAddress,
                briefAddress = counterpartyAddress.toBriefAddressFormat(),
            )
            else -> tx.extractAddressSubtitle()
        }

        return buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = title,
            icon = tx.directionalIcon(),
            subtitle = subtitle,
        )
    }

    private fun claimRewardsContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = tx.statusAwareTitle(
                pending = R.string.transaction_history_claiming_reward,
                confirmed = R.string.transaction_history_staking_reward,
            ),
            icon = TxIcon.Res(R.drawable.ic_transaction_history_claim_rewards_24),
            subtitle = ContentSubtitle.Plain(resourceReference(R.string.transaction_history_earned_from_stake)),
        )

    private fun yieldTopupContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.Topup,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = resourceReference(R.string.yield_module_transaction_topup),
        icon = tx.directionalIcon(),
        subtitle = tx.yieldSupplySubtitle(currency, type),
    )

    private fun yieldDeployContractContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.DeployContract,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = resourceReference(R.string.yield_module_transaction_deploy_contract),
        icon = TxIcon.Vector(Icons.ic_document_20),
        subtitle = tx.yieldSupplySubtitle(currency, type),
    )

    private fun yieldInitializeTokenContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.InitializeToken,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = resourceReference(R.string.yield_module_transaction_initialize),
        icon = TxIcon.Res(R.drawable.ic_gear_24),
        subtitle = tx.yieldSupplySubtitle(currency, type),
    )

    private fun yieldReactivateTokenContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.ReactivateToken,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = resourceReference(R.string.yield_module_transaction_reactivate),
        icon = TxIcon.Vector(Icons.ic_arrow_refresh_20),
        subtitle = tx.yieldSupplySubtitle(currency, type),
    )

    private fun yieldSendContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.Send,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = if (type.isYieldSupplyWithdraw || tx.isOutgoing) {
            resourceReference(R.string.yield_module_transaction_withdraw)
        } else {
            resourceReference(R.string.common_transfer)
        },
        icon = tx.directionalIcon(),
        subtitle = tx.yieldSupplySubtitle(currency, type),
        hideAmount = currency is CryptoCurrency.Token && !tx.isOutgoing,
    )

    private fun unknownOperationContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = resourceReference(R.string.transaction_history_operation),
        icon = tx.directionalIcon(),
        subtitle = tx.extractAddressSubtitle(),
    )

    private fun gaslessFeeContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = resourceReference(R.string.gasless_transaction_fee),
            icon = tx.directionalIcon(),
            subtitle = tx.extractAddressSubtitle(),
        )

    private fun buildContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        title: TextReference,
        icon: TxIcon,
        subtitle: ContentSubtitle,
        hideAmount: Boolean = false,
    ): TransactionItemUM.Content = TransactionItemUM.Content(
        txHash = tx.txHash,
        amount = if (hideAmount) "" else tx.formatContentAmount(currency),
        currencySymbol = if (hideAmount) "" else currency.symbol,
        time = tx.timestampInMillis.toTimeFormat(),
        status = uiStatus,
        direction = tx.extractDirection(),
        icon = if (uiStatus is TransactionItemUM.Content.Status.Failed) TxIcon.Vector(Icons.ic_cross_20) else icon,
        title = title,
        subtitle = subtitle,
        timestamp = tx.timestampInMillis,
        onClick = { txHistoryUiActions.openTxInExplorer(tx.txHash) },
    )
}

// region Content building helpers

private fun TxInfo.formatContentAmount(currency: CryptoCurrency): String {
    val prefix = when {
        status is TxInfo.TransactionStatus.Failed -> ""
        amount.isZero() -> ""
        type is TransactionType.Staking.ClaimRewards -> ""
        else -> if (isOutgoing) StringsSigns.MINUS else StringsSigns.PLUS
    }
    return prefix + amount.format { crypto(symbol = "", decimals = currency.decimals) }.trim()
}

// endregion

// region Subtitles

private fun resolveOwnSubtitle(
    lookupContext: TxHistoryLookupContext?,
    networkRawId: Network.RawID,
    address: String,
    direction: ContentSubtitle.Direction,
): ContentSubtitle? {
    val ctx = lookupContext ?: return null
    return when (val resolved = ctx.resolveOwner(address, networkRawId)) {
        is ResolvedOwner.OwnAccount -> ContentSubtitle.OwnAccount(
            direction = direction,
            accountName = resolved.account.accountName.toUM().value,
            iconResId = resolved.account.icon.value.getResId(),
            iconBackgroundColor = resolved.account.icon.color.getUiColor(),
        )
        is ResolvedOwner.OwnWallet -> ContentSubtitle.OwnWallet(
            direction = direction,
            walletName = resolved.walletInfo.name,
            deviceIconUM = resolved.walletInfo.deviceIconUM,
        )
        // External counterparty: caller falls back to ContentSubtitle.ExternalAddress.
        is ResolvedOwner.External -> null
    }
}

private fun TxInfo.yieldSupplySubtitle(currency: CryptoCurrency, type: TransactionType.YieldSupply): ContentSubtitle {
    if (currency is CryptoCurrency.Coin) {
        return if (type is TransactionType.YieldSupply.Send) {
            extractAddressSubtitle()
        } else {
            val briefAddress = type.address?.toBriefAddressFormat()
            val text = resourceReference(
                R.string.transaction_history_transaction_for_address,
                wrappedList(briefAddress.orEmpty()),
            )
            if (briefAddress.isNullOrEmpty()) {
                ContentSubtitle.Plain(text)
            } else {
                ContentSubtitle.PlainAddress(text = text, highlight = briefAddress)
            }
        }
    }
    return when (type) {
        is TransactionType.YieldSupply.Enter ->
            amountSubtitle(currency, R.string.yield_module_transaction_enter_subtitle)
        TransactionType.YieldSupply.Topup ->
            amountSubtitle(currency, R.string.yield_module_transaction_topup_subtitle)
        is TransactionType.YieldSupply.Exit ->
            amountSubtitle(currency, R.string.yield_module_transaction_exit_subtitle)
        is TransactionType.YieldSupply.Send -> if (!isOutgoing && type.isYieldSupplyWithdraw) {
            amountSubtitle(currency, R.string.yield_module_transaction_exit_subtitle)
        } else {
            extractAddressSubtitle()
        }
        else -> extractAddressSubtitle()
    }
}

private fun TxInfo.amountSubtitle(currency: CryptoCurrency, @StringRes resId: Int): ContentSubtitle.Plain {
    val formatted = amount.format { crypto(symbol = currency.symbol, decimals = currency.decimals) }
    return ContentSubtitle.Plain(resourceReference(resId, wrappedList(formatted)))
}

private fun TxInfo.extractAddressSubtitle(): ContentSubtitle {
    val text = extractSubtitleByAddressType()
    val highlight = interactionAddressType.highlightableBriefAddress()
    return if (highlight == null) {
        ContentSubtitle.Plain(text)
    } else {
        ContentSubtitle.PlainAddress(text = text, highlight = highlight)
    }
}

private fun TxInfo.InteractionAddressType?.highlightableBriefAddress(): String? = when (this) {
    is TxInfo.InteractionAddressType.Contract -> address.toBriefAddressFormat()
    is TxInfo.InteractionAddressType.User -> address.toBriefAddressFormat()
    is TxInfo.InteractionAddressType.Validator -> address.toBriefAddressFormat()
    is TxInfo.InteractionAddressType.Multiple,
    null,
    -> null
}

private fun TxInfo.extractSubtitleByAddressType(): TextReference =
    when (val interactionAddress = interactionAddressType) {
        is TxInfo.InteractionAddressType.Contract -> resourceReference(
            id = R.string.transaction_history_contract_address,
            formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
        )
        is TxInfo.InteractionAddressType.Multiple -> resourceReference(
            id = directionalAddressRes(),
            formatArgs = wrappedList(resourceReference(R.string.transaction_history_multiple_addresses)),
        )
        is TxInfo.InteractionAddressType.User -> resourceReference(
            id = directionalAddressRes(),
            formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
        )
        is TxInfo.InteractionAddressType.Validator -> resourceReference(
            id = R.string.transaction_history_transaction_validator,
            formatArgs = wrappedList(interactionAddress.address.toBriefAddressFormat()),
        )
        null -> TextReference.EMPTY
    }

private fun TxInfo.directionalAddressRes(): Int = if (isOutgoing) {
    R.string.transaction_history_transaction_to_address
} else {
    R.string.transaction_history_transaction_from_address
}

// endregion

// region Misc

private fun TxInfo.directionalIcon(): TxIcon =
    TxIcon.Vector(if (isOutgoing) Icons.ic_arrow_up_20 else Icons.ic_arrow_down_20)

private fun TxInfo.extractDirection(): TransactionItemUM.Content.Direction = if (isOutgoing) {
    TransactionItemUM.Content.Direction.OUTGOING
} else {
    TransactionItemUM.Content.Direction.INCOMING
}

// endregion