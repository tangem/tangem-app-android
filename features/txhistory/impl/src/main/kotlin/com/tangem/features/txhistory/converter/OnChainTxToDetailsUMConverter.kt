package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_chart_line_vertical_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_stack_20
import com.tangem.core.ui.res.generated.icons.ic_success_20
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.reclassifyOwnOperationAsTransfer
import com.tangem.features.txhistory.model.resolveOwner
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isZero
import com.tangem.utils.toBriefAddressFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Website the yield-supply protocol row links to — the hard-wired Aave integration's home page. */
private const val AAVE_WEBSITE = "https://aave.com/"

/**
 * Converts an on-chain [TxInfo] to the [TxHistoryDetailsUM.SingleAsset] details card (Receive / Send / Transfer /
 * staking / yield-supply). A two-asset swap always surfaces as an [com.tangem.domain.txhistory.model.ExpressTx.Swap]
 * (handled by [ExpressTxToDetailsUMConverter]); an on-chain `TxInfo` of type `Swap` (e.g. a DEX swap with no express
 * record) carries no legs, so it falls back to the single amount it does have rather than an empty two-asset card.
 */
internal class OnChainTxToDetailsUMConverter(
    private val currency: CryptoCurrency,
    private val onCopyAddress: (String) -> Unit,
    private val menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM>,
    /** Staking validators of the viewed currency keyed by on-chain address; resolves the validator row's name/link. */
    private val validatorsByAddress: Map<String, Yield.Validator>,
    private val onOpenValidator: (String) -> Unit,
    /** Resolves a transfer counterparty to one of the user's own accounts/wallets — the same lookup the list uses. */
    private val lookup: TxHistoryLookupContext,
) {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val titleConverter = TxHistoryTitleConverter()

    fun convert(value: TxInfo): TxHistoryDetailsUM.SingleAsset {
        // An unrecognized contract call between the user's own portfolios reads as a Transfer, not a raw operation —
        // mirrors the history list so the two never disagree.
        val tx = value.reclassifyOwnOperationAsTransfer(lookup, currency.network.id.rawId)
        return TxHistoryDetailsUM.SingleAsset(
            header = tx.toHeaderUM(),
            amountBlock = tx.toAmountBlockUM(),
            counterparty = tx.toCounterpartyUM(),
            // Validator (staking) / protocol (yield-supply), then the network fee from the tx; rate is not surfaced.
            rows = buildList {
                tx.validatorRow()?.let(::add)
                tx.protocolRow()?.let(::add)
                // A received plain transfer's fee was paid by the sender, not the user — omit it here. Non-Transfer
                // ops the user initiated (Approve, staking) keep their fee even when incoming.
                if (!tx.isReceivedTransfer()) addAll(tx.toInfoRows())
            }.toImmutableList(),
        )
    }

    /**
     * Protocol row of a yield-supply tx: the DeFi protocol the funds are supplied to. The yield-supply product is a
     * single hard-wired integration across the app (Aave — the [yield_module_provider][R.string.yield_module_provider]
     * name), so the value is that constant provider rather than a per-tx resolved name, and the tap opens the
     * constant [AAVE_WEBSITE]. Mutually exclusive with [validatorRow] — a tx is either staking or yield-supply,
     * never both.
     */
    private fun TxInfo.protocolRow(): TxHistoryDetailsUM.InfoRowUM? {
        if (type !is TransactionType.YieldSupply) return null
        return TxHistoryDetailsUM.InfoRowUM(
            label = resourceReference(R.string.staking_validator),
            value = resourceReference(R.string.yield_module_provider),
            trailingIconRes = R.drawable.ic_arrow_top_right_24,
            onClick = { onOpenValidator(AAVE_WEBSITE) },
        )
    }

    /**
     * Validator row of a staking tx: its display [name][Yield.Validator.name] and a link to the validator page. Present
     * only when the tx carries a validator address ([validatorAddress]) that resolves in [validatorsByAddress]; otherwise
     * `null` (non-staking tx, an address the current yield doesn't list, or a chain that omits the validator address).
     * The trailing arrow-link and tap are offered only when the validator has a [website][Yield.Validator.website].
     */
    private fun TxInfo.validatorRow(): TxHistoryDetailsUM.InfoRowUM? {
        val validator = validatorAddress()?.let(validatorsByAddress::get) ?: return null
        val website = validator.website?.ifBlank { null }
        return TxHistoryDetailsUM.InfoRowUM(
            label = resourceReference(R.string.staking_validator),
            value = stringReference(validator.name),
            trailingIconRes = website?.let { R.drawable.ic_arrow_top_right_24 },
            onClick = website?.let { url -> { onOpenValidator(url) } },
        )
    }

    /**
     * On-chain address of the validator a staking tx interacts with, or `null` for a non-staking tx or a staking tx that
     * does not surface it. A `Vote` carries it in the type itself; other staking types expose it through the interaction
     * or destination address typed as `Validator`.
     */
    private fun TxInfo.validatorAddress(): String? = when (val txType = type) {
        is TransactionType.Staking.Vote -> txType.validatorAddress
        is TransactionType.Staking -> interactionValidatorAddress() ?: destinationValidatorAddress()
        else -> null
    }

    private fun TxInfo.interactionValidatorAddress(): String? =
        (interactionAddressType as? TxInfo.InteractionAddressType.Validator)?.address

    private fun TxInfo.destinationValidatorAddress(): String? = when (val destination = destinationType) {
        is TxInfo.DestinationType.Single -> (destination.addressType as? TxInfo.AddressType.Validator)?.address
        is TxInfo.DestinationType.Multiple ->
            destination.addressTypes.filterIsInstance<TxInfo.AddressType.Validator>().firstOrNull()?.address
    }

    private fun TxInfo.toHeaderUM(): TxHistoryDetailsUM.HeaderUM = TxHistoryDetailsUM.HeaderUM(
        icon = headerIcon(),
        status = status.toUiStatus(),
        title = titleConverter.convert(this, isOwnTransfer = isOwnTransfer()),
        subtitle = headerSubtitle(timestampInMillis),
        menu = menu,
    )

    /** A transfer whose counterparty resolves to one of the user's own accounts/wallets reads "Transfer". */
    private fun TxInfo.isOwnTransfer(): Boolean = when (resolvedCounterparty()) {
        is ResolvedOwner.OwnAccount, is ResolvedOwner.OwnPaymentAccount, is ResolvedOwner.OwnWallet -> true
        is ResolvedOwner.External, null -> false
    }

    /** A received plain transfer — its on-chain fee belongs to the sender, so the details omit the fee row. */
    private fun TxInfo.isReceivedTransfer(): Boolean = type is TxInfo.TransactionType.Transfer && !isOutgoing

    /**
     * The counterparty resolved against the user's portfolios on the viewed currency's network, so the title and the
     * counterparty card never disagree with the list. [interactionAddressType] already carries the correct counterparty
     * for every type — including an incoming [TxInfo.TransactionType.Transfer], whose interaction address the mapper
     * takes from the sender. The one exception is an **incoming** unrecognized call ([TxInfo.TransactionType.Operation]
     * / [TxInfo.TransactionType.UnknownOperation]): there [interactionAddressType] is the destination — the viewed
     * wallet itself — so the real sender ([TxInfo.sourceType]) is resolved instead. `null` when there is no single
     * plain counterparty address.
     */
    private fun TxInfo.resolvedCounterparty(): ResolvedOwner? {
        val isIncomingOperation = !isOutgoing &&
            (type is TransactionType.Operation || type is TransactionType.UnknownOperation)
        val address = if (isIncomingOperation) {
            (sourceType as? TxInfo.SourceType.Single)?.address
        } else {
            (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        } ?: return null
        return lookup.resolveOwner(address = address, networkRawId = currency.network.id.rawId)
    }

    private fun TxInfo.toAmountBlockUM(): TxHistoryDetailsUM.AmountBlockUM = TxHistoryDetailsUM.AmountBlockUM(
        icon = amountIcon(),
        amount = stringReference(signedAmount(currency)),
        label = amountLabel(),
        isFailed = status is TxInfo.TransactionStatus.Failed,
    )

    /**
     * Amount icon. Yield-supply enter/exit shows the asset paired with the hard-wired Aave protocol icon, ordered by
     * direction (Aave leads on "Supplied"/enter, the asset leads on "Returned"/exit — mirroring the two states in the
     * design); every other type shows the single token avatar. Only enter/exit are paired for now — the remaining
     * yield-supply variants (topup / withdraw) can adopt the same pair later.
     */
    private fun TxInfo.amountIcon(): TxHistoryDetailsUM.AmountIconUM {
        val asset = TxHistoryDetailsUM.AmountIconUM.Item.Currency(iconStateConverter.convert(currency))
        val aave = TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22)
        return when (type) {
            is TransactionType.YieldSupply.Enter ->
                TxHistoryDetailsUM.AmountIconUM.OverlappingPair(leading = aave, trailing = asset)
            is TransactionType.YieldSupply.Exit ->
                TxHistoryDetailsUM.AmountIconUM.OverlappingPair(leading = asset, trailing = aave)
            else -> TxHistoryDetailsUM.AmountIconUM.Single(iconStateConverter.convert(currency))
        }
    }

    /** "Supplied"/"Returned" label above the amount for yield-supply enter/exit; `null` (no label) for other types. */
    private fun TxInfo.amountLabel(): TextReference? = when (type) {
        is TransactionType.YieldSupply.Enter -> resourceReference(R.string.yield_module_transaction_supplied)
        is TransactionType.YieldSupply.Exit -> resourceReference(R.string.yield_module_transaction_returned)
        else -> null
    }

    /**
     * Counterparty card ("Recipient" / "From"), built from the `User` interaction address (the same source the history
     * list uses for its subtitle); a counterparty that is not a plain `User` address yields no card (`null`).
     *
     * The address is resolved through the shared [lookup]: an own account / own wallet renders its name and avatar
     * (no copy — the title is a display name, not an address), anything else renders the external brief address with
     * an identicon and the copy action.
     */
    private fun TxInfo.toCounterpartyUM(): TxHistoryDetailsUM.CounterpartyUM? {
        // Contract interactions (yield-supply / staking / approve) talk to a protocol/validator, not a real recipient —
        // no copyable counterparty card.
        val isContractInteraction = type is TransactionType.YieldSupply ||
            type is TransactionType.Staking ||
            type is TransactionType.Approve
        if (isContractInteraction) return null
        return when (val owner = resolvedCounterparty()) {
            is ResolvedOwner.OwnAccount -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_account),
                title = owner.account.accountName.toUM().value,
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Account(
                    iconResId = owner.account.icon.value.getResId(),
                    backgroundColor = owner.account.icon.color.getUiColor(),
                ),
                onCopyClick = null,
            )
            is ResolvedOwner.OwnPaymentAccount -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_account),
                title = owner.account.accountName.toUM().value,
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.PaymentAccount,
                onCopyClick = null,
            )
            is ResolvedOwner.OwnWallet -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_wallet),
                title = stringReference(owner.walletInfo.name),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Wallet(deviceIconUM = owner.walletInfo.deviceIconUM),
                onCopyClick = null,
            )
            is ResolvedOwner.External -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_address),
                title = stringReference(owner.address.toBriefAddressFormat()),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Address(rawAddress = owner.address),
                onCopyClick = { onCopyAddress(owner.address) },
            )
            null -> null
        }
    }

    /**
     * Section label above the counterparty: "Recipient" for outgoing transfers, and for incoming the caller-supplied
     * [incoming] label reflecting the resolved sender kind ("From account" / "From wallet" / "From address").
     */
    private fun TxInfo.counterpartyLabel(@StringRes incoming: Int): TextReference =
        if (isOutgoing) resourceReference(R.string.send_recipient) else resourceReference(incoming)
}

// region Amount / header building helpers

/** How the header amount is signed, decided by the transaction type. */
private enum class AmountSign {

    /** `-` for outgoing, `+` for incoming — plain value transfers. */
    BY_DIRECTION,

    /** Always `+` — an inflow regardless of the reported direction (staking rewards). */
    ALWAYS_PLUS,

    /**
     * No sign — protocol interactions (staking, approvals, yield-supply enter/exit) whose amount is a parameter of the
     * operation, not a transfer in/out of the account.
     */
    NONE,
}

private fun TransactionType.amountSign(): AmountSign = when (this) {
    is TransactionType.Staking.ClaimRewards -> AmountSign.ALWAYS_PLUS
    is TransactionType.Staking,
    is TransactionType.Approve,
    is TransactionType.YieldSupply.Enter,
    is TransactionType.YieldSupply.Exit,
    -> AmountSign.NONE
    else -> AmountSign.BY_DIRECTION
}

/**
 * Signed crypto amount with inline symbol, e.g. `+ 350.31 USDT` / `- 350.31 USDT`. The sign is decided per transaction
 * type by [amountSign], and is dropped for zero amounts and for the failed state (a failed tx moved nothing) — the UI
 * then only strikes the amount through and dims it via [TxHistoryDetailsUM.AmountBlockUM.isFailed].
 */
private fun TxInfo.signedAmount(currency: CryptoCurrency): String {
    val formatted = amount.format { crypto(cryptoCurrency = currency, ignoreSymbolPosition = true) }
    val prefix = when {
        status is TxInfo.TransactionStatus.Failed -> ""
        amount.isZero() -> ""
        else -> when (type.amountSign()) {
            AmountSign.BY_DIRECTION -> if (isOutgoing) "${StringsSigns.MINUS} " else "${StringsSigns.PLUS} "
            AmountSign.ALWAYS_PLUS -> "${StringsSigns.PLUS} "
            AmountSign.NONE -> ""
        }
    }
    return (prefix + formatted).trim()
}

/** Type glyph. Unlike the history list, the failed state keeps the type glyph (only the color changes). */
private fun TxInfo.headerIcon(): TxIcon = when (type) {
    is TransactionType.Approve -> TxIcon.Vector(Icons.ic_success_20)
    is TransactionType.Staking.Stake,
    is TransactionType.Staking.Unstake,
    is TransactionType.Staking.Restake,
    -> TxIcon.Vector(Icons.ic_stack_20)
    is TransactionType.YieldSupply -> TxIcon.Vector(Icons.ic_chart_line_vertical_20)
    is TransactionType.Operation -> TxIcon.Vector(Icons.ic_document_20)
    is TransactionType.Swap -> TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20)
    else -> TxIcon.Vector(if (isOutgoing) Icons.ic_arrow_up_20 else Icons.ic_arrow_down_20)
}

// endregion