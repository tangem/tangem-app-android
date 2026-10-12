package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
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
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_percent_20
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.portfolioIcon
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.converter.TxHistoryStatusPillConverter.Input as PillInput
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.counterpartyAddress
import com.tangem.features.txhistory.model.resolveCounterparty
import com.tangem.features.txhistory.model.resolveOwner
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero

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
    private val networkTokens: Set<CryptoCurrency.Token> = emptySet(),
) : Converter<TxInfo, TransactionItemUM> {

    private val titleConverter = TxHistoryTitleConverter(currency)
    private val pillConverter = TxHistoryStatusPillConverter(currency, txHistoryUiActions)
    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    @Suppress("CyclomaticComplexMethod")
    override fun convert(value: TxInfo): TransactionItemUM {
        val uiStatus = value.status.toUiStatus()
        if (value.isFeeOnlyOperation(currency)) return feeOnlyOperationContent(value, uiStatus)
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
            is TransactionType.YieldSupply.Topup -> pillConverter.convert(PillInput(value, uiStatus, YieldSupplySpec))
            // endregion

            // region Content
            is TransactionType.Operation -> operationContent(value, uiStatus)
            is TransactionType.Swap -> swapContent(value, uiStatus)
            is TransactionType.Transfer -> transferContent(value, uiStatus)
            is TransactionType.Staking.ClaimRewards -> claimRewardsContent(value, uiStatus)
            is TransactionType.YieldSupply.Send -> yieldSend(value, uiStatus, type)
            is TransactionType.YieldSupply.DeployContract -> yieldDeployContractContent(value, uiStatus, type)
            is TransactionType.YieldSupply.InitializeToken -> yieldInitializeTokenContent(value, uiStatus, type)
            is TransactionType.YieldSupply.ReactivateToken -> yieldReactivateTokenContent(value, uiStatus, type)
            is TransactionType.UnknownOperation -> unknownOperationContent(value, uiStatus)
            is TransactionType.GaslessFee,
            is TransactionType.NetworkFee,
            -> feeContent(value, uiStatus)
            // endregion
        }
    }

    /**
     * An unrecognized contract call whose direction-correct counterparty resolves to one of the user's own portfolios
     * is really a transfer between the user's own accounts/wallets, so it is rendered as a Transfer (own-transfer title,
     * directional icon, "to / from MY account/wallet" subtitle). `null` when the counterparty is external or cannot be
     * resolved (no lookup context, multi-address side), leaving the caller's raw-operation rendering.
     */
    private fun ownTransferContentOrNull(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
    ): TransactionItemUM.Content? {
        if (tx !is TxInfo.Single) return null
        val address = tx.counterpartyAddress() ?: return null
        val direction = if (tx.isOutgoing) ContentSubtitle.Direction.TO else ContentSubtitle.Direction.FROM
        val ownSubtitle = resolveOwnSubtitle(
            lookupContext = lookupContext,
            networkRawId = currency.network.id.rawId,
            address = address,
            direction = direction,
        ) ?: return null

        return buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = titleConverter.convert(tx.copy(type = TransactionType.Transfer), isOwnTransfer = true),
            icon = tx.directionalIcon(),
            subtitle = ownSubtitle,
        )
    }

    private fun operationContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        ownTransferContentOrNull(tx, uiStatus) ?: buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = titleConverter.convert(tx),
            icon = TxIcon.Vector(Icons.ic_document_20),
            subtitle = tx.operationCounterpartySubtitle(),
        )

    private fun swapContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = titleConverter.convert(tx),
            icon = tx.directionalIcon(),
            subtitle = tx.extractAddressSubtitle(),
        )

    private fun transferContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content {
        val counterpartyAddress = (tx.interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        val direction = if (tx.isOutgoing) ContentSubtitle.Direction.TO else ContentSubtitle.Direction.FROM
        val counterparty = counterpartyAddress?.let(::resolveCounterparty)

        val subtitle = when {
            counterparty != null -> counterparty.toSubtitle(direction)
            counterpartyAddress != null -> externalAddressSubtitle(counterpartyAddress, direction)
            else -> tx.extractAddressSubtitle()
        }

        return buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = titleConverter.convert(tx, isOwnTransfer = counterparty.isOwnPortfolio()),
            icon = tx.directionalIcon(),
            subtitle = subtitle,
        )
    }

    private fun claimRewardsContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = titleConverter.convert(tx),
            icon = TxIcon.Res(R.drawable.ic_transaction_history_claim_rewards_24),
            subtitle = ContentSubtitle.Plain(resourceReference(R.string.transaction_history_earned_from_stake)),
        )

    private fun yieldDeployContractContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.DeployContract,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = titleConverter.convert(tx),
        icon = TxIcon.Vector(Icons.ic_document_20),
        subtitle = tx.yieldSupplySubtitle(type),
    )

    private fun yieldInitializeTokenContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.InitializeToken,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = titleConverter.convert(tx),
        icon = TxIcon.Res(R.drawable.ic_gear_24),
        subtitle = tx.yieldSupplySubtitle(type),
    )

    private fun yieldReactivateTokenContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.ReactivateToken,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = titleConverter.convert(tx),
        icon = TxIcon.Vector(Icons.ic_arrow_refresh_20),
        subtitle = tx.yieldSupplySubtitle(type),
    )

    /**
     * A withdraw Send takes the funds out of the yield position, so it reads as the yield pill its topup counterpart
     * does. A non-withdraw Send is an ordinary transfer — resolve the counterparty against the user's own
     * accounts/wallets and render it exactly like Transfer, so the list matches the details screen.
     */
    private fun yieldSend(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        type: TransactionType.YieldSupply.Send,
    ): TransactionItemUM = if (type.isYieldSupplyWithdraw) {
        pillConverter.convert(PillInput(tx, uiStatus, YieldWithdrawSpec))
    } else {
        transferContent(tx, uiStatus)
    }

    private fun unknownOperationContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
    ): TransactionItemUM.Content = ownTransferContentOrNull(tx, uiStatus) ?: buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = titleConverter.convert(tx),
        icon = tx.directionalIcon(),
        subtitle = tx.operationCounterpartySubtitle(),
    )

    /**
     * A row for an unrecognized call that moved no coin: the same fee row as [feeContent], but the call was never
     * decoded, so the subtitle names the operation itself instead of the asset the fee was paid for.
     */
    private fun feeOnlyOperationContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
    ): TransactionItemUM.Content = buildContent(
        tx = tx,
        uiStatus = uiStatus,
        title = titleConverter.convert(tx),
        icon = TxIcon.Vector(Icons.ic_percent_20),
        subtitle = tx.feeOnlyOperationSubtitle(),
    )

    /**
     * What the fee was paid for: the localized name of the protocol call the operation gave up its type for; else the
     * contract method the history named — raw and unlocalized, the same way the [TransactionType.Operation] row title
     * shows it — or a bare "operation" when the method has no name at all.
     */
    private fun TxInfo.feeOnlyOperationSubtitle(): ContentSubtitle {
        val operation = type as? TransactionType.Operation
            ?: return ContentSubtitle.Plain(resourceReference(R.string.transaction_history_fee_for_operation))

        operation.recognized?.feeSubtitleResId()
            ?.let { return ContentSubtitle.Plain(resourceReference(it)) }

        val methodName = operation.name.ifBlank { null }
            ?: return ContentSubtitle.Plain(resourceReference(R.string.transaction_history_fee_for_operation))

        return ContentSubtitle.Plain(
            resourceReference(R.string.transaction_history_fee_for_operation_name, wrappedList(methodName)),
        )
    }

    /** A row that is nothing but a network fee — the gasless fee leg, or a token transfer in the coin history. */
    private fun feeContent(tx: TxInfo, uiStatus: TransactionItemUM.Content.Status): TransactionItemUM.Content =
        buildContent(
            tx = tx,
            uiStatus = uiStatus,
            title = titleConverter.convert(tx),
            icon = TxIcon.Vector(Icons.ic_percent_20),
            subtitle = tx.feeSubtitle(),
        )

    /** What the fee was paid for; a gasless leg or an untracked token has nothing to name and says so instead. */
    private fun TxInfo.feeSubtitle(): ContentSubtitle {
        val networkFee = type as? TransactionType.NetworkFee
        val symbol = networkFee?.amount?.currencySymbol?.ifBlank { null }
            ?: return ContentSubtitle.Plain(resourceReference(R.string.transaction_history_fee_for_sending))

        return ContentSubtitle.FeeFor(
            symbol = symbol,
            icon = networkFee.amount.resolveToken(networkTokens)?.let(iconStateConverter::convert),
        )
    }

    /**
     * The counterparty [address] resolved for display — own account → own wallet → saved contact → the raw address —
     * or `null` without a [lookupContext], which leaves the caller on the raw address.
     */
    private fun resolveCounterparty(address: String): ResolvedOwner? =
        lookupContext?.resolveCounterparty(address = address, networkRawId = currency.network.id.rawId)

    /**
     * The subtitle naming [address] as one of the user's own portfolios or a saved contact, or `null` when it is none
     * of them — the caller then keeps whatever wording it shows a raw address with. Applied wherever a row names a
     * counterparty, independently of the transaction type and of the direction the address sits on.
     */
    private fun namedCounterpartySubtitle(address: String, direction: ContentSubtitle.Direction): ContentSubtitle? =
        resolveCounterparty(address)
            ?.takeIf { it !is ResolvedOwner.External }
            ?.toSubtitle(direction)

    /**
     * Yield supply is a token product, and a coin only ever paid the fee for it — such a row never reaches here, it is
     * a [network fee][isFeeOnlyOperation] by then. So the subtitle speaks of the token that moved.
     */
    private fun TxInfo.yieldSupplySubtitle(type: TransactionType.YieldSupply): ContentSubtitle {
        return when (type) {
            is TransactionType.YieldSupply.Enter ->
                amountSubtitle(currency, R.string.yield_module_transaction_enter_subtitle)
            is TransactionType.YieldSupply.Exit ->
                amountSubtitle(currency, R.string.yield_module_transaction_exit_subtitle)
            else -> extractAddressSubtitle()
        }
    }

    /**
     * Counterparty subtitle for an unrecognized contract call (Operation / UnknownOperation). For an **incoming** call
     * [TxInfo.interactionAddressType] is the destination — the viewed wallet itself — so the sender is read from
     * [TxInfo.sourceType] instead: a single source is named like any other counterparty, multiple sources render
     * "from multiple addresses" (reading [TxInfo.interactionAddressType] here would wrongly show the own destination).
     * An **outgoing** call keeps its (correct) destination-based subtitle.
     */
    private fun TxInfo.operationCounterpartySubtitle(): ContentSubtitle {
        if (isOutgoing) return extractAddressSubtitle()
        return when (val source = sourceType) {
            is TxInfo.SourceType.Single -> namedCounterpartySubtitle(
                address = source.address,
                direction = ContentSubtitle.Direction.FROM,
            ) ?: ContentSubtitle.ExternalAddress(
                direction = ContentSubtitle.Direction.FROM,
                rawAddress = source.address,
            )
            is TxInfo.SourceType.Multiple -> ContentSubtitle.Plain(
                resourceReference(
                    R.string.transaction_history_transaction_from_address,
                    wrappedList(resourceReference(R.string.transaction_history_multiple_addresses)),
                ),
            )
        }
    }

    /**
     * The subtitle naming the address this row interacted with. A `User` address is a counterparty, so it is named
     * after the portfolio or contact it resolves to; a contract or validator address talks to no one and keeps its own
     * wording, as does an address that resolves to nothing.
     */
    private fun TxInfo.extractAddressSubtitle(): ContentSubtitle {
        val userAddress = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        if (userAddress != null) {
            val direction = if (isOutgoing) ContentSubtitle.Direction.TO else ContentSubtitle.Direction.FROM
            namedCounterpartySubtitle(address = userAddress, direction = direction)?.let { return it }
        }

        val text = extractSubtitleByAddressType()
        val address = interactionAddressType.displayedAddress()
        return if (address == null) {
            ContentSubtitle.Plain(text)
        } else {
            ContentSubtitle.PlainAddress(text = text, address = address)
        }
    }

    private fun buildContent(
        tx: TxInfo,
        uiStatus: TransactionItemUM.Content.Status,
        title: TextReference,
        icon: TxIcon,
        subtitle: ContentSubtitle,
    ): TransactionItemUM.Content = TransactionItemUM.Content(
        txHash = tx.txHash,
        amount = tx.formatContentAmount(currency),
        currencySymbol = currency.symbol,
        time = tx.timestampInMillis.toTimeFormat(),
        status = uiStatus,
        direction = tx.extractDirection(),
        icon = icon,
        title = title,
        subtitle = subtitle,
        timestamp = tx.timestampInMillis,
        onClick = { txHistoryUiActions.openTxInExplorer(tx.txHash) },
    )
}

// region Content building helpers

private fun TxInfo.formatContentAmount(currency: CryptoCurrency): String {
    val value = amountOrFee(currency)
    val prefix = when {
        status is TxInfo.TransactionStatus.Failed -> ""
        value.isZero() -> ""
        type is TransactionType.Staking.ClaimRewards -> ""
        else -> if (isOutgoing) StringsSigns.MINUS else StringsSigns.PLUS
    }
    return prefix + value.format { crypto(symbol = "", decimals = currency.displayDecimals) }.trim()
}

// endregion

// region Subtitles

/**
 * The own-portfolio subtitle for [address], or `null` when it resolves to anything else — a saved contact or an
 * external address. Used by the paths that only reclassify a row when the counterparty is the user's own.
 */
private fun resolveOwnSubtitle(
    lookupContext: TxHistoryLookupContext?,
    networkRawId: Network.RawID,
    address: String,
    direction: ContentSubtitle.Direction,
): ContentSubtitle? {
    val owner = lookupContext?.resolveOwner(address, networkRawId) ?: return null
    return if (owner.isOwnPortfolio()) owner.toSubtitle(direction) else null
}

/** One of the user's own portfolios — a saved contact or an external address is somebody else's. */
private fun ResolvedOwner?.isOwnPortfolio(): Boolean = when (this) {
    is ResolvedOwner.OwnAccount, is ResolvedOwner.OwnPaymentAccount, is ResolvedOwner.OwnWallet -> true
    is ResolvedOwner.Foreign, null -> false
}

private fun ResolvedOwner.toSubtitle(direction: ContentSubtitle.Direction): ContentSubtitle = when (this) {
    is ResolvedOwner.OwnAccount -> ContentSubtitle.OwnAccount(
        direction = direction,
        accountName = account.accountName.toUM().value,
        avatar = account.icon.toPortfolioAvatar(name = account.accountName.toUM().value),
    )
    is ResolvedOwner.OwnPaymentAccount -> ContentSubtitle.OwnPaymentAccount(
        direction = direction,
        accountName = account.accountName.toUM().value,
    )
    is ResolvedOwner.OwnWallet -> ContentSubtitle.OwnWallet(
        direction = direction,
        walletName = walletInfo.name,
        deviceIconUM = walletInfo.deviceIconUM,
    )
    is ResolvedOwner.SavedContact -> ContentSubtitle.Contact(
        direction = direction,
        contactName = contact.name.value,
        avatar = contact.toContactAvatar(),
    )
    is ResolvedOwner.External -> externalAddressSubtitle(address, direction)
}

private fun externalAddressSubtitle(address: String, direction: ContentSubtitle.Direction) =
    ContentSubtitle.ExternalAddress(direction = direction, rawAddress = address)

/**
 * A portfolio still on the default `Letter` icon renders the monogram of [name]; an icon picked in the account or
 * contact editor renders as its own drawable. The same rule the design-system `AccountIcon` applies wherever a
 * portfolio avatar is drawn full-size — here for the inline row avatar, which has its own container.
 *
 * The letter itself is left to the composition: [name] may be a string resource (the main account's title), so its
 * first character is only known once the reference resolves.
 */
internal fun CryptoPortfolioIcon.toPortfolioAvatar(name: TextReference): ContentSubtitle.PortfolioAvatar =
    if (value == CryptoPortfolioIcon.Icon.Letter) {
        ContentSubtitle.PortfolioAvatar.Monogram(name = name, color = color.getUiColor())
    } else {
        ContentSubtitle.PortfolioAvatar.Resource(resId = value.getResId(), color = color.getUiColor())
    }

/**
 * A contact's name is a plain string, so an empty one is caught here rather than rendering a blank circle: it falls
 * back to the letter drawable, as it did before the monogram existed.
 */
internal fun Contact.toContactAvatar(): ContentSubtitle.PortfolioAvatar {
    val icon = portfolioIcon()

    return if (name.value.isEmpty()) {
        ContentSubtitle.PortfolioAvatar.Resource(resId = icon.value.getResId(), color = icon.color.getUiColor())
    } else {
        icon.toPortfolioAvatar(name = stringReference(name.value))
    }
}

/**
 * The network-fee subtitle naming the protocol call a coin's history only paid the gas of. `null` for a call the row
 * has no wording for — a yield [Send][TransactionType.YieldSupply.Send] changed no mode, it is a transfer the module
 * made — which leaves the subtitle on the raw method name.
 */
@StringRes
private fun TransactionType.feeSubtitleResId(): Int? = when (this) {
    is TransactionType.YieldSupply.Enter -> R.string.transaction_history_fee_for_yield_mode_activation
    is TransactionType.YieldSupply.Topup -> R.string.transaction_history_fee_for_yield_mode_top_up
    is TransactionType.YieldSupply.Exit -> R.string.transaction_history_fee_for_yield_mode_deactivation
    is TransactionType.YieldSupply.DeployContract -> R.string.transaction_history_fee_for_yield_mode_deployment
    is TransactionType.YieldSupply.InitializeToken -> R.string.transaction_history_fee_for_yield_mode_initialization
    is TransactionType.YieldSupply.ReactivateToken -> R.string.transaction_history_fee_for_yield_mode_reactivation
    is TransactionType.Approve,
    TransactionType.GaslessFee,
    is TransactionType.NetworkFee,
    is TransactionType.Operation,
    TransactionType.Staking.ClaimRewards,
    TransactionType.Staking.Restake,
    TransactionType.Staking.Stake,
    TransactionType.Staking.Unstake,
    is TransactionType.Staking.Vote,
    TransactionType.Staking.Withdraw,
    TransactionType.Swap,
    TransactionType.Transfer,
    TransactionType.UnknownOperation,
    is TransactionType.YieldSupply.Send,
    -> null
}

private fun TxInfo.amountSubtitle(currency: CryptoCurrency, @StringRes resId: Int): ContentSubtitle.Plain {
    val formatted = amount.format { crypto(currency) }
    return ContentSubtitle.Plain(resourceReference(resId, wrappedList(formatted)))
}

private fun TxInfo.InteractionAddressType?.displayedAddress(): String? = when (this) {
    is TxInfo.InteractionAddressType.Contract -> address
    is TxInfo.InteractionAddressType.User -> address
    is TxInfo.InteractionAddressType.Validator -> address
    is TxInfo.InteractionAddressType.Multiple,
    null,
    -> null
}

private fun TxInfo.extractSubtitleByAddressType(): TextReference =
    when (val interactionAddress = interactionAddressType) {
        is TxInfo.InteractionAddressType.Contract -> resourceReference(
            id = R.string.transaction_history_contract_address,
            formatArgs = wrappedList(interactionAddress.address),
        )
        is TxInfo.InteractionAddressType.Multiple -> resourceReference(
            id = directionalAddressRes(),
            formatArgs = wrappedList(resourceReference(R.string.transaction_history_multiple_addresses)),
        )
        is TxInfo.InteractionAddressType.User -> resourceReference(
            id = directionalAddressRes(),
            formatArgs = wrappedList(interactionAddress.address),
        )
        is TxInfo.InteractionAddressType.Validator -> resourceReference(
            id = R.string.transaction_history_transaction_validator,
            formatArgs = wrappedList(interactionAddress.address),
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