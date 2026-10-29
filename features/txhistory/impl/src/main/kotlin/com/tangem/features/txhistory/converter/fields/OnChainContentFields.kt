package com.tangem.features.txhistory.converter.fields

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_percent_20
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.converter.TxHistoryTitleConverter
import com.tangem.features.txhistory.converter.amountOrFee
import com.tangem.features.txhistory.converter.displayedStatus
import com.tangem.features.txhistory.converter.approximateMark
import com.tangem.features.txhistory.converter.feeForAmount
import com.tangem.features.txhistory.converter.feeForToken
import com.tangem.features.txhistory.converter.isFeeOnlyOperation
import com.tangem.features.txhistory.converter.readsFailed
import com.tangem.features.txhistory.converter.showsFeeAsAmount
import com.tangem.features.txhistory.converter.toSubtitle
import com.tangem.features.txhistory.converter.toUiStatus
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.resolveCounterparty
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isZero

/**
 * The on-chain half of the per-field mapping: what an [TxInfo] row puts into one cell of
 * [TransactionItemUM.Content], dispatched on [TransactionType].
 *
 * Called from [TxHistoryInfoFieldConverter], which owns the `TxHistoryInfo`-level dispatch — a field function here
 * answers for a single cell of a single on-chain row, never for the row as a whole. Every `when` is exhaustive over
 * [TransactionType] without an `else`, so a new transaction type cannot be added without deciding what each cell
 * shows for it; see the no-`else` rule on [TxHistoryInfoFieldConverter] before editing these dispatches.
 *
 * Types that render as a [TransactionItemUM.Pill] are filtered out before these functions are reached (see
 * [TxHistoryInfoFieldConverter.pillSpecOrNull]); they keep a branch here only to hold the `when`s exhaustive and are
 * grouped with the neutral fallback of their cell.
 */
internal class OnChainContentFields(
    private val currency: CryptoCurrency,
    private val lookup: TxHistoryLookupContext?,
    private val networkTokens: Set<CryptoCurrency.Token>,
) {

    private val titleConverter = TxHistoryTitleConverter(currency)
    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    /** A fee-only row moved nothing of its own, so it states the fee it cost instead; a joint fee is a cap, hence `~`. */
    fun amount(tx: TxInfo): String {
        val value = tx.amountOrFee(currency)
        val prefix = when {
            tx.readsFailed(currency) -> ""
            value.isZero() -> ""
            tx.type is TransactionType.Staking.ClaimRewards -> ""
            tx.isOutgoing -> StringsSigns.MINUS_SIGN
            else -> StringsSigns.PLUS
        }
        val approximateMark = if (tx.showsFeeAsAmount(currency)) tx.approximateMark() else ""

        return prefix + approximateMark +
            value.format { crypto(symbol = "", decimals = currency.displayDecimals) }.trim()
    }

    fun status(tx: TxInfo): TransactionItemUM.Content.Status = tx.displayedStatus(currency).toUiStatus()

    /** Dispatched on [TransactionType] by [TxHistoryTitleConverter], shared with the details header. */
    fun title(tx: TxInfo): TextReference = titleConverter.convert(tx, isOwnTransfer = tx.hasOwnCounterparty())

    @Suppress("CyclomaticComplexMethod")
    fun icon(tx: TxInfo): TxIcon {
        if (tx.isFeeOnlyOperation(currency)) return TxIcon.Vector(Icons.ic_percent_20)

        return when (tx.type) {
            is TransactionType.Operation,
            is TransactionType.YieldSupply.DeployContract,
            -> TxIcon.Vector(Icons.ic_document_20)
            is TransactionType.Staking.ClaimRewards ->
                TxIcon.Res(R.drawable.ic_transaction_history_claim_rewards_24)
            is TransactionType.YieldSupply.InitializeToken -> TxIcon.Res(R.drawable.ic_gear_24)
            is TransactionType.YieldSupply.ReactivateToken -> TxIcon.Vector(Icons.ic_arrow_refresh_20)
            is TransactionType.GaslessFee,
            is TransactionType.NetworkFee,
            -> TxIcon.Vector(Icons.ic_percent_20)
            is TransactionType.Transfer,
            is TransactionType.Joint.Transfer,
            is TransactionType.Swap,
            is TransactionType.UnknownOperation,
            is TransactionType.IncentiveClaim,
            is TransactionType.YieldSupply.Send,
            // Pill types — see the class KDoc.
            is TransactionType.Approve,
            is TransactionType.Staking.Stake,
            is TransactionType.Staking.Unstake,
            is TransactionType.Staking.Restake,
            is TransactionType.Staking.Vote,
            is TransactionType.Staking.Withdraw,
            is TransactionType.YieldSupply.Enter,
            is TransactionType.YieldSupply.Exit,
            is TransactionType.YieldSupply.Topup,
            -> tx.directionalIcon()
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun subtitle(tx: TxInfo): ContentSubtitle {
        if (tx.isFeeOnlyOperation(currency)) return tx.feeOnlyOperationSubtitle()

        return when (tx.type) {
            is TransactionType.Transfer,
            is TransactionType.Joint.Transfer,
            // A withdraw Send is a pill; any other one is a transfer the yield module made from the user's address.
            is TransactionType.YieldSupply.Send,
            -> tx.transferSubtitle()
            is TransactionType.Operation,
            is TransactionType.UnknownOperation,
            -> tx.operationCounterpartySubtitle()
            is TransactionType.Staking.ClaimRewards ->
                ContentSubtitle.Plain(resourceReference(R.string.transaction_history_earned_from_stake))
            // The reward reads as coming from Merkl rather than from the module contract that relayed it.
            is TransactionType.IncentiveClaim -> ContentSubtitle.Provider(
                direction = tx.subtitleDirection(),
                name = resourceReference(R.string.yield_module_incentives_provider),
                iconResId = R.drawable.img_merkl_22,
            )
            is TransactionType.GaslessFee,
            is TransactionType.NetworkFee,
            -> tx.feeSubtitle()
            is TransactionType.Swap,
            is TransactionType.YieldSupply.DeployContract,
            is TransactionType.YieldSupply.InitializeToken,
            is TransactionType.YieldSupply.ReactivateToken,
            // Pill types — see the class KDoc.
            is TransactionType.Approve,
            is TransactionType.Staking.Stake,
            is TransactionType.Staking.Unstake,
            is TransactionType.Staking.Restake,
            is TransactionType.Staking.Vote,
            is TransactionType.Staking.Withdraw,
            is TransactionType.YieldSupply.Enter,
            is TransactionType.YieldSupply.Exit,
            is TransactionType.YieldSupply.Topup,
            -> tx.addressSubtitle()
        }
    }

    /**
     * Whether the counterparty this row interacted with is one of the user's own portfolios, which turns
     * "Send" / "Receive" into "Transfer".
     */
    private fun TxInfo.hasOwnCounterparty(): Boolean {
        val address = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address ?: return false
        return resolveCounterparty(address).isOwnPortfolio()
    }

    /** "to / from" the counterparty the transfer settled with: its portfolio, its contact name, or its address. */
    private fun TxInfo.transferSubtitle(): ContentSubtitle {
        val address = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
            ?: return addressSubtitle()
        val direction = subtitleDirection()

        return resolveCounterparty(address)?.toSubtitle(direction)
            ?: ContentSubtitle.ExternalAddress(direction = direction, rawAddress = address)
    }

    /**
     * Counterparty subtitle for an unrecognized contract call. For an **incoming** call
     * [TxInfo.interactionAddressType] is the destination — the viewed wallet itself — so the sender is read from
     * [TxInfo.sourceType] instead; an **outgoing** call keeps its (correct) destination-based subtitle.
     */
    private fun TxInfo.operationCounterpartySubtitle(): ContentSubtitle {
        if (isOutgoing) return addressSubtitle()

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
     * after the portfolio or contact it resolves to; a contract or validator address talks to no one and keeps its
     * own wording, as does an address that resolves to nothing.
     */
    private fun TxInfo.addressSubtitle(): ContentSubtitle {
        val userAddress = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        if (userAddress != null) {
            namedCounterpartySubtitle(address = userAddress, direction = subtitleDirection())
                ?.let { return it }
        }

        val text = addressTypeText()
        val address = interactionAddressType.displayedAddress()

        return if (address == null) {
            ContentSubtitle.Plain(text)
        } else {
            ContentSubtitle.PlainAddress(text = text, address = address)
        }
    }

    /** What the fee was paid for; a gasless leg or an untracked token has nothing to name and says so instead. */
    private fun TxInfo.feeSubtitle(): ContentSubtitle {
        val symbol = feeForAmount()?.currencySymbol?.ifBlank { null }
            ?: return ContentSubtitle.Plain(resourceReference(R.string.transaction_history_fee_for_sending))

        return ContentSubtitle.FeeFor(
            symbol = symbol,
            icon = feeForToken(networkTokens)?.let(iconStateConverter::convert),
        )
    }

    /**
     * What the fee of an undecoded call was paid for: the localized name of the protocol call the operation gave up
     * its type for; else the contract method the history named — raw and unlocalized — or a bare "operation".
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

    /**
     * The subtitle naming [address] as one of the user's own portfolios or a saved contact, or `null` when it is
     * neither — the caller then keeps whatever wording it shows a raw address with.
     */
    private fun namedCounterpartySubtitle(address: String, direction: ContentSubtitle.Direction): ContentSubtitle? =
        resolveCounterparty(address)
            ?.takeIf { it !is ResolvedOwner.External }
            ?.toSubtitle(direction)

    private fun resolveCounterparty(address: String): ResolvedOwner? =
        lookup?.resolveCounterparty(address = address, networkRawId = currency.network.id.rawId)
}

// region Per-cell helpers

private fun TxInfo.subtitleDirection(): ContentSubtitle.Direction = if (isOutgoing) {
    ContentSubtitle.Direction.TO
} else {
    ContentSubtitle.Direction.FROM
}

private fun TxInfo.directionalIcon(): TxIcon =
    TxIcon.Vector(if (isOutgoing) Icons.ic_arrow_up_20 else Icons.ic_arrow_down_20)

private fun ResolvedOwner?.isOwnPortfolio(): Boolean = when (this) {
    is ResolvedOwner.OwnAccount,
    is ResolvedOwner.OwnPaymentAccount,
    is ResolvedOwner.OwnMoneyAccount,
    is ResolvedOwner.OwnWallet,
    -> true
    is ResolvedOwner.Foreign, null -> false
}

private fun TxInfo.InteractionAddressType?.displayedAddress(): String? = when (this) {
    is TxInfo.InteractionAddressType.Contract -> address
    is TxInfo.InteractionAddressType.User -> address
    is TxInfo.InteractionAddressType.Validator -> address
    is TxInfo.InteractionAddressType.Multiple,
    null,
    -> null
}

private fun TxInfo.addressTypeText(): TextReference = when (val interactionAddress = interactionAddressType) {
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

/**
 * The network-fee subtitle naming the protocol call a coin's history only paid the gas of. `null` for a call the row
 * has no wording for, which leaves the subtitle on the raw method name.
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
    is TransactionType.Joint.Transfer,
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
    TransactionType.IncentiveClaim,
    -> null
}

// endregion