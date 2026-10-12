package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_chart_line_vertical_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.core.ui.res.generated.icons.ic_percent_20
import com.tangem.core.ui.res.generated.icons.ic_stack_20
import com.tangem.core.ui.res.generated.icons.ic_success_20
import com.tangem.domain.addressbook.model.portfolioIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.mayCarryStakingTarget
import com.tangem.features.txhistory.model.reclassifyOwnOperationAsTransfer
import com.tangem.features.txhistory.model.resolveCounterparty
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Website the "Explore Aave" menu row opens — the hard-wired Aave integration's home page. */
private const val AAVE_WEBSITE = "https://aave.com/"

/**
 * Converts an on-chain [TxInfo] to the [TxHistoryDetailsUM.SingleAsset] details card (Receive / Send / Transfer /
 * staking / yield-supply). A two-asset swap always surfaces as an [com.tangem.domain.txhistory.model.ExpressTx.Swap]
 * (handled by [ExpressTxToDetailsUMConverter]); an on-chain `TxInfo` of type `Swap` (e.g. a DEX swap with no express
 * record) carries no legs, so it falls back to the single amount it does have rather than an empty two-asset card.
 */
@Suppress("LongParameterList")
internal class OnChainTxToDetailsUMConverter(
    private val currency: CryptoCurrency,
    private val onCopyAddress: (String) -> Unit,
    private val menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM>,
    /** Known staking targets (validators and vaults) keyed by on-chain address; resolves the validator row's name. */
    private val targetsByAddress: Map<String, StakingTarget>,
    private val onExploreProvider: (String) -> Unit,
    /** Resolves a transfer counterparty to one of the user's own accounts/wallets — the same lookup the list uses. */
    private val lookup: TxHistoryLookupContext,
    private val isBalanceHidden: Boolean,
    /** Names and draws the asset of a network-fee transaction. */
    private val networkTokens: Set<CryptoCurrency.Token> = emptySet(),
    /** Prices the amount in the user's app currency for the secondary line under it. */
    private val fiatConversion: FiatConversion = FiatConversion.NONE,
) {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val titleConverter = TxHistoryTitleConverter(currency)

    /**
     * [targetsByAddress] re-keyed by lowercase address, for the case-insensitive fallback in [resolveTarget]. Built
     * once per conversion rather than per lookup.
     */
    private val targetsByLowercaseAddress: Map<String, StakingTarget> by lazy {
        targetsByAddress.mapKeys { (address, _) -> address.lowercase() }
    }

    fun convert(value: TxInfo): TxHistoryDetailsUM.SingleAsset {
        // An unrecognized contract call between the user's own portfolios reads as a Transfer, not a raw operation —
        // mirrors the history list so the two never disagree.
        val tx = when {
            // A call that moved no coin is the fee it cost, never a transfer between the user's own portfolios.
            value.isFeeOnlyOperation(currency) -> value
            value is TxInfo.Single -> value.reclassifyOwnOperationAsTransfer(lookup, currency.network.id.rawId)
            else -> value
        }
        return TxHistoryDetailsUM.SingleAsset(
            header = tx.toHeaderUM(),
            isBalanceHidden = isBalanceHidden,
            amountBlock = tx.toAmountBlockUM(),
            counterparty = tx.toTransferredAssetUM() ?: tx.toCounterpartyUM(),
            // The validator of a staking tx, then the network fee from the tx; rate is not surfaced.
            rows = buildList {
                tx.validatorRow()?.let(::add)
                if (!tx.hasForeignFee() && !tx.isFeeOnly(currency)) addAll(tx.feeRows(currency))
            }.toImmutableList(),
            statusBanner = tx.approveRiskBanner(),
        )
    }

    /**
     * The menu with a trailing "Explore Aave" row for a yield-supply tx — the DeFi protocol the funds are supplied to.
     * The yield-supply product is a single hard-wired integration across the app (Aave), so the row opens the constant
     * [AAVE_WEBSITE] rather than a per-tx resolved link, and every yield-supply transaction offers it.
     */
    private fun ImmutableList<TxHistoryDetailsUM.MenuItemUM>.withExploreProvider(
        tx: TxInfo,
    ): ImmutableList<TxHistoryDetailsUM.MenuItemUM> {
        if (tx.type !is TransactionType.YieldSupply) return this
        val exploreProvider = TxHistoryDetailsUM.MenuItemUM(
            icon = Icons.ic_globe_24,
            title = resourceReference(R.string.yield_module_explore_sheet_explore_aave_button_title),
            action = TxHistoryDetailsUM.MenuItemUM.Action.Direct { onExploreProvider(AAVE_WEBSITE) },
        )
        return (this + exploreProvider).toImmutableList()
    }

    /**
     * Validator row of a staking tx: the target's display [name][StakingTarget.name] as plain text — the row carries
     * no link to the target's page. Present only for a tx that [may carry a target][mayCarryStakingTarget] and either
     * resolves one of its [candidate addresses][validatorAddressCandidates] in [targetsByAddress] or at least
     * [names a validator itself][declaredValidatorAddress]; otherwise `null` — a chain that omits the validator
     * address entirely (Solana unstake/withdraw and Tron freeze/unfreeze/claim carry no validator field in the SDK at
     * all).
     */
    private fun TxInfo.validatorRow(): TxHistoryDetailsUM.InfoRowUM? {
        if (!mayCarryStakingTarget()) return null
        val name = validatorAddressCandidates().firstNotNullOfOrNull(::resolveTarget)?.name
        val value = name ?: declaredValidatorAddress() ?: return null
        return TxHistoryDetailsUM.InfoRowUM(
            label = resourceReference(R.string.staking_validator),
            value = stringReference(value),
            isValueAddress = name == null,
        )
    }

    /**
     * Address the transaction itself declares to be the validator, shown in place of a name when [targetsByAddress]
     * has none for it — a validator the staking API has since dropped from the yield must still name the target of an
     * old transaction instead of making the row disappear.
     *
     * Only addresses the SDK types as a validator qualify, unlike the broader [validatorAddressCandidates]: a
     * `Contract` / `User` / plain destination address is a target only if the lookup confirms it, so falling back to
     * one would label an arbitrary counterparty "Validator". Blank addresses are skipped — the Tron vote parser
     * reports a missing validator as an empty string.
     */
    private fun TxInfo.declaredValidatorAddress(): String? = buildList {
        (type as? TransactionType.Staking.Vote)?.let { add(it.validatorAddress) }
        (interactionAddressType as? TxInfo.InteractionAddressType.Validator)?.let { add(it.address) }
        when (val destination = destinationType) {
            is TxInfo.DestinationType.Single -> destination.addressType.validatorAddress()?.let(::add)
            is TxInfo.DestinationType.Multiple -> addAll(destination.addressTypes.mapNotNull { it.validatorAddress() })
        }
    }.firstOrNull { it.isNotBlank() }

    private fun TxInfo.AddressType.validatorAddress(): String? = (this as? TxInfo.AddressType.Validator)?.address

    /**
     * Addresses of a staking tx that may identify the staking target, most specific first. Membership in
     * [targetsByAddress] is what decides — an address that is a known validator/vault *is* the target — so the
     * candidates are deliberately not restricted to `Validator`-typed addresses: on EVM chains the staking call's
     * destination is the validator contract (e.g. a Polygon ValidatorShare, which is exactly the address StakeKit
     * lists as the validator) but the SDK types it as `Contract`, and only Solana ever emits `AddressType.Validator`.
     * A `Vote` (Tron) carries the address in the type itself, where the SDK may leave it blank.
     */
    private fun TxInfo.validatorAddressCandidates(): List<String> = buildList {
        (type as? TransactionType.Staking.Vote)?.let { add(it.validatorAddress) }
        interactionAddressType?.let { interaction ->
            when (interaction) {
                is TxInfo.InteractionAddressType.Validator -> add(interaction.address)
                is TxInfo.InteractionAddressType.Contract -> add(interaction.address)
                is TxInfo.InteractionAddressType.User -> add(interaction.address)
                is TxInfo.InteractionAddressType.Multiple -> addAll(interaction.addresses)
            }
        }
        when (val destination = destinationType) {
            is TxInfo.DestinationType.Single -> add(destination.addressType.address)
            is TxInfo.DestinationType.Multiple -> addAll(destination.addressTypes.map { it.address })
        }
    }

    /**
     * The known staking target at [address], or `null` when it is blank (the Tron vote parser reports a missing
     * validator as an empty string) or unknown. Falls back to a case-insensitive match because an EVM address reaches
     * us checksummed or lowercased depending on the history provider, while the staking API picks its own casing.
     */
    private fun resolveTarget(address: String): StakingTarget? {
        if (address.isBlank()) return null
        return targetsByAddress[address] ?: targetsByLowercaseAddress[address.lowercase()]
    }

    private fun TxInfo.toHeaderUM(): TxHistoryDetailsUM.HeaderUM = TxHistoryDetailsUM.HeaderUM(
        icon = headerIcon(currency),
        status = status.toUiStatus(),
        title = titleConverter.convert(this, isOwnTransfer = isOwnTransfer()),
        subtitle = headerSubtitle(timestampInMillis),
        menu = menu.withCopyAddress(copyableAddress(), onCopyAddress).withExploreProvider(this),
    )

    /** A transfer whose counterparty resolves to one of the user's own accounts/wallets reads "Transfer". */
    private fun TxInfo.isOwnTransfer(): Boolean = when (resolvedCounterparty()) {
        is ResolvedOwner.OwnAccount, is ResolvedOwner.OwnPaymentAccount, is ResolvedOwner.OwnWallet -> true
        is ResolvedOwner.Foreign, null -> false
    }

    /**
     * The counterparty resolved against the user's portfolios and address books on the viewed currency's network, so
     * the title and the counterparty card never disagree with the list. [interactionAddressType] already carries the
     * correct counterparty for every type — including an incoming [TxInfo.TransactionType.Transfer], whose interaction
     * address the mapper takes from the sender. The one exception is an **incoming** unrecognized call
     * ([TxInfo.TransactionType.Operation] / [TxInfo.TransactionType.UnknownOperation]): there [interactionAddressType]
     * is the destination — the viewed wallet itself — so the real sender ([TxInfo.sourceType]) is resolved instead.
     * `null` when there is no single plain counterparty address.
     */
    private fun TxInfo.resolvedCounterparty(): ResolvedOwner? {
        val address = counterpartyAddress() ?: return null
        return lookup.resolveCounterparty(address = address, networkRawId = currency.network.id.rawId)
    }

    /** The raw counterparty address [resolvedCounterparty] resolves, before the [lookup] turns it into an owner. */
    private fun TxInfo.counterpartyAddress(): String? {
        val isIncomingOperation = !isOutgoing &&
            (type is TransactionType.Operation || type is TransactionType.UnknownOperation)
        return if (isIncomingOperation) {
            (sourceType as? TxInfo.SourceType.Single)?.address
        } else {
            (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        }
    }

    private fun TxInfo.toAmountBlockUM(): TxHistoryDetailsUM.AmountBlockUM = TxHistoryDetailsUM.AmountBlockUM(
        icon = amountIcon(),
        amount = feeAmountText(currency) ?: approveAmountText() ?: stringReference(signedAmount(currency)),
        label = amountLabel(),
        fiatAmount = fiatAmountText(),
        isFailed = status is TxInfo.TransactionStatus.Failed,
    )

    /**
     * Fiat equivalent shown under the amount, unsigned — it prices what moved, not the direction. `null` leaves the
     * line out: an approval states an allowance in a token that is not the viewed currency, so the viewed currency's
     * rate cannot price it, and a currency with no known quote has no rate to price anything with.
     */
    private fun TxInfo.fiatAmountText(): TextReference? {
        if (type is TransactionType.Approve) return null

        return fiatConversion.convert(amountOrFee(currency))
    }

    /**
     * The card of a fee-only transaction, standing in for the counterparty one: the asset the fee was paid for, over
     * the "Send" label. An untracked token can be neither named nor denominated, so the card is dropped altogether.
     */
    private fun TxInfo.toTransferredAssetUM(): TxHistoryDetailsUM.CounterpartyUM? {
        val networkFee = type as? TransactionType.NetworkFee ?: return null
        val transferred = networkFee.amount ?: return null
        val symbol = transferred.currencySymbol.ifBlank { null } ?: return null
        val amount = transferred.value?.format {
            crypto(symbol = symbol, decimals = transferred.decimals, ignoreSymbolPosition = true)
        }

        return TxHistoryDetailsUM.CounterpartyUM(
            label = resourceReference(R.string.common_send),
            title = TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference(amount ?: symbol)),
            avatar = TxHistoryDetailsUM.CounterpartyAvatar.Glyph(icon = TxIcon.Vector(Icons.ic_arrow_up_20)),
            titleIcon = networkFee.amount.resolveToken(networkTokens)?.let(iconStateConverter::convert),
        )
    }

    /**
     * Approve's amount is a parameter of the granted allowance, not the tx's own transferred value — it comes from
     * [TransactionType.Approve.amount] instead of [signedAmount], in the approved token's symbol (not the viewed
     * currency's: an approval in the coin history is still denominated in the token). A `null` allowance value means
     * an unlimited approval, shown as "Unlimited <symbol>" rather than a number (paired with [approveRiskBanner]).
     * `null` return (non-Approve types) tells the caller to fall back to the regular signed amount.
     */
    private fun TxInfo.approveAmountText(): TextReference? {
        val allowance = (type as? TransactionType.Approve)?.amount ?: return null
        val allowanceValue = allowance.value
            ?: return resourceReference(
                R.string.transaction_history_unlimited_amount,
                wrappedList(allowance.currencySymbol),
            )
        return stringReference(
            allowanceValue.format {
                crypto(symbol = allowance.currencySymbol, decimals = allowance.decimals, ignoreSymbolPosition = true)
            },
        )
    }

    /**
     * Amount icon. A yield-supply operation shows the asset paired with the hard-wired Aave protocol icon, ordered by
     * direction (Aave leads on "Supplied"/enter, the asset leads on "Returned"/exit and on a withdraw — mirroring the
     * states in the design); an approval shows the approved token, which in a coin's history is not the viewed
     * currency — the same distinction [approveAmountText] draws for the amount; every other type shows the single
     * token avatar. A non-withdraw yield Send is a plain transfer and stays unpaired.
     */
    private fun TxInfo.amountIcon(): TxHistoryDetailsUM.AmountIconUM {
        val asset = TxHistoryDetailsUM.AmountIconUM.Item.Currency(iconStateConverter.convert(currency))
        val aave = TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22)
        return when (val type = type) {
            is TransactionType.YieldSupply.Enter ->
                TxHistoryDetailsUM.AmountIconUM.OverlappingPair(leading = aave, trailing = asset)
            is TransactionType.YieldSupply.Exit,
            is TransactionType.YieldSupply.Topup,
            -> TxHistoryDetailsUM.AmountIconUM.OverlappingPair(leading = asset, trailing = aave)
            is TransactionType.YieldSupply.Send -> if (type.isYieldSupplyWithdraw) {
                TxHistoryDetailsUM.AmountIconUM.OverlappingPair(leading = asset, trailing = aave)
            } else {
                TxHistoryDetailsUM.AmountIconUM.Single(iconStateConverter.convert(currency))
            }
            is TransactionType.Approve -> TxHistoryDetailsUM.AmountIconUM.Single(type.approvedTokenIcon())
            else -> TxHistoryDetailsUM.AmountIconUM.Single(iconStateConverter.convert(currency))
        }
    }

    /**
     * Avatar of the token the allowance was granted in, falling back to the viewed currency — which is also what the
     * data layer denominates the allowance in when the wallet does not track the approved token.
     */
    private fun TransactionType.Approve.approvedTokenIcon(): CurrencyIconState =
        amount.resolveToken(networkTokens)?.let(iconStateConverter::convert) ?: iconStateConverter.convert(currency)

    /**
     * "Supplied"/"Returned" label above the amount for yield-supply enter/exit, "Approved amount" for an approval;
     * `null` (no label) for other types.
     */
    private fun TxInfo.amountLabel(): TextReference? = when (type) {
        is TransactionType.YieldSupply.Enter -> resourceReference(R.string.yield_module_transaction_supplied)
        is TransactionType.YieldSupply.Exit -> resourceReference(R.string.yield_module_transaction_returned)
        is TransactionType.Approve -> resourceReference(R.string.transaction_history_approved_amount)
        else -> null
    }

    /**
     * "High Risk" warning shown under the amount block for an unlimited approval — `null` (no banner) once the
     * approval carries a concrete amount, or while the tx is [Failed][TxInfo.TransactionStatus.Failed] (an approval
     * that never went through granted no real allowance, so there is nothing to warn about).
     */
    private fun TxInfo.approveRiskBanner(): TxHistoryDetailsUM.StatusBannerUM? {
        val approve = type as? TransactionType.Approve ?: return null
        if (approve.amount.value != null || status is TxInfo.TransactionStatus.Failed) return null
        return TxHistoryDetailsUM.StatusBannerUM(
            style = TxHistoryDetailsUM.StatusBannerUM.Style.Warning,
            title = resourceReference(R.string.transaction_history_approve_high_risk_title),
            subtitle = resourceReference(R.string.transaction_history_approve_high_risk_subtitle),
            isLoading = false,
        )
    }

    /**
     * Counterparty card ("Recipient" / "From"), built from the `User` interaction address (the same source the history
     * list uses for its subtitle); a counterparty that is not a plain `User` address yields no card (`null`).
     *
     * The address is resolved through the shared [lookup] for display only: an own account / own wallet renders its
     * name and avatar, anything else the external brief address with an identicon. Whatever it renders, the card copies
     * the underlying address on tap — the same address the header menu's "Copy address" row copies.
     */
    private fun TxInfo.toCounterpartyUM(): TxHistoryDetailsUM.CounterpartyUM? {
        val address = copyableAddress() ?: return null
        val copyAddress = { onCopyAddress(address) }
        return when (val owner = resolvedCounterparty()) {
            is ResolvedOwner.OwnAccount -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_account),
                title = TxHistoryDetailsUM.CounterpartyTitle.Name(owner.account.accountName.toUM().value),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Account(
                    name = owner.account.accountName.toUM().value,
                    icon = CryptoPortfolioIconConverter.convert(owner.account.icon),
                ),
                onCopyClick = copyAddress,
            )
            is ResolvedOwner.OwnPaymentAccount -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_account),
                title = TxHistoryDetailsUM.CounterpartyTitle.Name(owner.account.accountName.toUM().value),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.PaymentAccount,
                onCopyClick = copyAddress,
            )
            is ResolvedOwner.OwnWallet -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_wallet),
                title = TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference(owner.walletInfo.name)),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Wallet(deviceIconUM = owner.walletInfo.deviceIconUM),
                onCopyClick = copyAddress,
            )
            is ResolvedOwner.SavedContact -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_address),
                title = TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference(owner.contact.name.value)),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Contact(
                    name = owner.contact.name.value,
                    icon = CryptoPortfolioIconConverter.convert(owner.contact.portfolioIcon()),
                ),
                onCopyClick = copyAddress,
            )
            is ResolvedOwner.External -> TxHistoryDetailsUM.CounterpartyUM(
                label = counterpartyLabel(incoming = R.string.common_from_address),
                title = TxHistoryDetailsUM.CounterpartyTitle.Address(owner.address),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Address(rawAddress = owner.address),
                onCopyClick = copyAddress,
            )
            null -> null
        }
    }

    /**
     * Whether the transaction has a counterparty worth a card. A fee-only transaction has none — what it paid the fee
     * for is named by [toTransferredAssetUM] instead, and where the fee itself went is not a recipient the user chose.
     * Contract interactions (yield-supply / staking / approve) talk to a protocol/validator, not a real recipient. A
     * NON-withdraw Send is exempt: it is a plain transfer and keeps its recipient card. A withdraw Send stays a yield
     * operation, so it gets none (no "from: Aave" in details).
     */
    private fun TxInfo.hasCounterpartyCard(): Boolean {
        if (isFeeOnly(currency)) return false
        val isPlainSend = (type as? TransactionType.YieldSupply.Send)?.isYieldSupplyWithdraw == false
        val isProtocolType = type is TransactionType.YieldSupply ||
            type is TransactionType.Staking ||
            type is TransactionType.Approve
        return !isProtocolType || isPlainSend
    }

    /**
     * Address copied by both the counterparty card and the header menu's "Copy address" row — the two are offered
     * together, on exactly the transactions that show a [counterparty card][toCounterpartyUM]. What the card *displays*
     * plays no part: an own account or wallet renders its name, and copying still yields the address behind it.
     */
    private fun TxInfo.copyableAddress(): String? {
        if (!hasCounterpartyCard()) return null
        return counterpartyAddress()?.ifBlank { null }
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
private fun TxInfo.headerIcon(currency: CryptoCurrency): TxIcon {
    if (isFeeOnlyOperation(currency)) return TxIcon.Vector(Icons.ic_percent_20)
    return when (val type = type) {
        is TransactionType.Approve -> TxIcon.Vector(Icons.ic_success_20)
        is TransactionType.GaslessFee,
        is TransactionType.NetworkFee,
        -> TxIcon.Vector(Icons.ic_percent_20)
        is TransactionType.Staking.Stake,
        is TransactionType.Staking.Unstake,
        is TransactionType.Staking.Restake,
        -> TxIcon.Vector(Icons.ic_stack_20)
        is TransactionType.YieldSupply.Enter,
        is TransactionType.YieldSupply.Exit,
        -> TxIcon.Vector(Icons.ic_chart_line_vertical_20)
        // A non-withdraw Send is a plain transfer — directional arrow. A withdraw stays a yield operation and
        // keeps the yield lightning glyph (falls through to the YieldSupply branch below); it only drops the
        // validator/counterparty.
        is TransactionType.YieldSupply.Send -> if (type.isYieldSupplyWithdraw) {
            TxIcon.Vector(Icons.ic_lightning_20)
        } else {
            TxIcon.Vector(if (isOutgoing) Icons.ic_arrow_up_20 else Icons.ic_arrow_down_20)
        }
        is TransactionType.YieldSupply -> TxIcon.Vector(Icons.ic_lightning_20)
        is TransactionType.Operation -> TxIcon.Vector(Icons.ic_document_20)
        is TransactionType.Swap -> TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20)
        else -> TxIcon.Vector(if (isOutgoing) Icons.ic_arrow_up_20 else Icons.ic_arrow_down_20)
    }
}

// endregion