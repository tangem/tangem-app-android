package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.StatusBannerUM.Severity
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.toBriefAddressFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Converts a [TxHistoryInfo] row to a [TxHistoryDetailsUM] for the in-app transaction details card.
 *
 * The dispatch mirrors the row converters: an [OnChainTx.BSDK] always renders as [TxHistoryDetailsUM.SingleAsset]
 * (a two-asset swap surfaces as [ExpressTx.Swap], handled separately), while an [ExpressTx] (swap / onramp) renders as
 * [TxHistoryDetailsUM.TwoAssets] — the `from`/`to` legs come from the express deal ([ExchangeTransaction] asset pair /
 * [OnrampTransaction] fiat→asset), and the network-fee row comes from the matched on-chain leg ([ExpressTx.txInfo]).
 */
internal class TxHistoryInfoToTxHistoryDetailsUMConverter(
    private val currency: CryptoCurrency,
    private val onCopyAddress: (String) -> Unit,
    /** Own deposit addresses for this currency's network — used to label own-transfers as "Transfer". */
    private val ownAddresses: Set<String> = emptySet(),
) : Converter<TxHistoryInfo, TxHistoryDetailsUM> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val exchangeStatusConverter = ExpressExchangeStatusToUiStatusConverter()
    private val onrampStatusConverter = ExpressOnrampStatusToUiStatusConverter()

    override fun convert(value: TxHistoryInfo): TxHistoryDetailsUM = when (value) {
        is OnChainTx.BSDK -> convertOnChain(value.txInfo)
        is ExpressTx.Swap -> convertExpressSwap(value)
        is ExpressTx.Onramp -> convertExpressOnramp(value)
    }

    // region On-chain (TxInfo)

    /**
     * Every on-chain row renders as [TxHistoryDetailsUM.SingleAsset]. A two-asset swap always surfaces as
     * [ExpressTx.Swap] (handled separately); an on-chain `TxInfo` of type `Swap` (e.g. a DEX swap with no express
     * record) carries no legs, so it falls back to the single amount it does have rather than an empty two-asset card.
     */
    private fun convertOnChain(value: TxInfo): TxHistoryDetailsUM = TxHistoryDetailsUM.SingleAsset(
        header = value.toHeaderUM(),
        amountBlock = value.toAmountBlockUM(),
        counterparty = value.toCounterpartyUM(),
        // Network fee from the tx itself; rate is not surfaced (no data).
        rows = value.toInfoRows(),
    )

    private fun TxInfo.toHeaderUM(): TxHistoryDetailsUM.HeaderUM = TxHistoryDetailsUM.HeaderUM(
        iconRes = headerIcon(),
        status = status.toUiStatus(),
        title = headerTitle(),
        subtitle = headerSubtitle(timestampInMillis),
    )

    private fun TxInfo.toAmountBlockUM(): TxHistoryDetailsUM.AmountBlockUM = TxHistoryDetailsUM.AmountBlockUM(
        currencyIcon = iconStateConverter.convert(currency),
        amount = stringReference(signedAmount(currency)),
        isFailed = status is TxInfo.TransactionStatus.Failed,
    )

    /**
     * Counterparty card ("Recipient" / "From"). Currently only the external-address avatar is produced — built from
     * the `User` interaction address (the same source the history list uses for its external-address subtitle).
     *
     * The own-account / own-wallet avatars require the address->owner lookup the list assembles in
     * `TxHistoryLookupContext`; wiring that into the detail model is a follow-up, so for now a counterparty that is not
     * a plain external `User` address yields no card (`null`).
     */
    private fun TxInfo.toCounterpartyUM(): TxHistoryDetailsUM.CounterpartyUM? {
        val address = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address ?: return null
        return TxHistoryDetailsUM.CounterpartyUM(
            label = counterpartyLabel(),
            title = stringReference(address.toBriefAddressFormat()),
            avatar = TxHistoryDetailsUM.CounterpartyAvatar.Address(rawAddress = address),
            onCopyClick = { onCopyAddress(address) },
        )
    }

    /** Section label above the counterparty: "Recipient" for outgoing transfers, "From" for incoming. */
    private fun TxInfo.counterpartyLabel(): TextReference =
        if (isOutgoing) resourceReference(R.string.send_recipient) else resourceReference(R.string.common_from)

    private fun TxInfo.headerTitle(): TextReference = when (type) {
        is TransactionType.Swap -> statusAwareTitle(R.string.common_swapping, R.string.common_swapped)
        is TransactionType.Transfer -> transferTitle()
        else -> stringReference(type.toString())
    }

    /**
     * Transfer header label, mirroring the history row: a transfer between the user's own accounts/wallets reads
     * "Transfer", an outgoing transfer to an external address "Send", an incoming one "Receive" (status-aware).
     */
    private fun TxInfo.transferTitle(): TextReference {
        val counterpartyAddress = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address
        val isOwnTransfer = counterpartyAddress != null && counterpartyAddress in ownAddresses
        return when {
            isOwnTransfer -> statusAwareTitle(R.string.common_transfer, R.string.common_transferred)
            isOutgoing -> statusAwareTitle(R.string.common_sending, R.string.common_sent)
            else -> statusAwareTitle(R.string.common_receiving, R.string.common_received)
        }
    }

    // endregion

    // region Express (swap / onramp)

    /**
     * The two-asset block always renders the deal's `fromAsset`→`toAsset` regardless of [ExpressTx.Swap.isOutgoing] —
     * `isOutgoing` only selects which leg is the *viewed* one in the history row, it does not reorder the detail legs.
     */
    private fun convertExpressSwap(swap: ExpressTx.Swap): TxHistoryDetailsUM.TwoAssets {
        val status = exchangeStatusConverter.convert(swap.tx.status)
        return TxHistoryDetailsUM.TwoAssets(
            header = TxHistoryDetailsUM.HeaderUM(
                iconRes = R.drawable.ic_exchange_vertical_24,
                status = status,
                title = status.statusAwareTitle(R.string.common_swapping, R.string.common_swapped),
                subtitle = headerSubtitle(swap.timestampMillis),
            ),
            from = swap.tx.fromAsset.toAssetUM(
                label = resourceReference(R.string.swapping_from_title_v2),
                sign = status.outgoingSign(),
                isFaded = status is Status.Failed,
            ),
            to = swap.tx.toAsset.toAssetUM(
                label = resourceReference(R.string.swapping_to_title),
                sign = status.incomingSign(),
                isFaded = status is Status.Failed,
            ),
            statusBanner = swap.tx.status.toStatusBannerUM(),
            rows = swap.toInfoRows(),
        )
    }

    private fun convertExpressOnramp(onramp: ExpressTx.Onramp): TxHistoryDetailsUM.TwoAssets {
        val status = onrampStatusConverter.convert(onramp.tx.status)
        return TxHistoryDetailsUM.TwoAssets(
            header = TxHistoryDetailsUM.HeaderUM(
                iconRes = R.drawable.ic_tangem_card_24,
                status = status,
                title = status.statusAwareTitle(
                    R.string.tx_history_onramp_top_up,
                    R.string.tx_history_onramp_topped_up,
                ),
                subtitle = headerSubtitle(onramp.timestampMillis),
            ),
            from = onramp.tx.fromFiat.toFiatAssetUM(
                label = resourceReference(R.string.swapping_from_title_v2),
                isFaded = status is Status.Failed,
            ),
            to = onramp.tx.toAsset.toAssetUM(
                label = resourceReference(R.string.swapping_to_title),
                sign = status.incomingSign(),
                isFaded = status is Status.Failed,
            ),
            statusBanner = onramp.tx.status.toStatusBannerUM(),
            rows = onramp.toInfoRows(),
        )
    }

    /**
     * Builds one crypto leg of the two-asset block. The ticker symbol and icon come from the resolved
     * [ExpressTransactionAsset.cryptoCurrency]; when it is unresolved the symbol falls back to the network id and the
     * icon slot is left empty ([currencyIcon] = `null`).
     */
    private fun ExpressTransactionAsset.toAssetUM(
        label: TextReference,
        sign: String,
        isFaded: Boolean,
    ): TxHistoryDetailsUM.AssetUM {
        val symbol = cryptoCurrency?.symbol ?: id.networkId
        val formatted = amount.format { crypto(
            symbol = symbol,
            decimals = decimals,
            ignoreSymbolPosition = true,
        ) }.trim()
        return TxHistoryDetailsUM.AssetUM(
            label = label,
            owner = null,
            amount = stringReference((sign + formatted).trim()),
            currencyIcon = cryptoCurrency?.let(iconStateConverter::convert),
            isFaded = isFaded,
        )
    }

    /**
     * Builds the fiat ("You paid") leg of an onramp. The paid fiat amount is exact and carries no sign — neither `+`/`−`
     * nor the `~` estimate — so only the value is shown. Fiat has no `CryptoCurrency`, so it also has no icon.
     */
    private fun Amount.toFiatAssetUM(label: TextReference, isFaded: Boolean): TxHistoryDetailsUM.AssetUM {
        val code = (type as? AmountType.FiatType)?.code ?: currencySymbol
        val formatted = (value ?: BigDecimal.ZERO)
            .format { fiat(fiatCurrencyCode = code, fiatCurrencySymbol = currencySymbol) }
        return TxHistoryDetailsUM.AssetUM(
            label = label,
            owner = null,
            amount = stringReference(formatted.trim()),
            currencyIcon = null,
            isFaded = isFaded,
        )
    }

    // endregion
}

// region Status helpers

/**
 * Express swap status → the status plaque under the two-asset block.
 *
 * In-flight stages render as [Severity.Info] with the rotating loader; [Verifying][ExpressExchangeStatus.Verifying]
 * (KYC) and the paused / refunded terminals as [Severity.Warning]; the failure terminals as [Severity.Error]; the
 * [Finished][ExpressExchangeStatus.Finished] success as [Severity.Success] (the plaque then auto-collapses — see
 * `TxHistoryDetailsStatusBanner`). [Unknown][ExpressExchangeStatus.Unknown] carries nothing to show, so it hides the
 * plaque (`null`).
 */
private fun ExpressExchangeStatus.toStatusBannerUM(): TxHistoryDetailsUM.StatusBannerUM? = when (this) {
    ExpressExchangeStatus.Preview,
    ExpressExchangeStatus.Created,
    ExpressExchangeStatus.ExchangeTxSent,
    ExpressExchangeStatus.Waiting,
    -> loadingBanner(R.string.express_exchange_status_receiving_active)
    ExpressExchangeStatus.WaitingTxHash -> loadingBanner(R.string.express_exchange_status_waiting_tx_hash)
    ExpressExchangeStatus.Confirming -> loadingBanner(R.string.express_exchange_status_confirming_active)
    ExpressExchangeStatus.Exchanging -> loadingBanner(R.string.express_exchange_status_exchanging_active)
    ExpressExchangeStatus.Sending -> loadingBanner(R.string.express_exchange_status_sending_active)
    ExpressExchangeStatus.Verifying -> verificationBanner()
    ExpressExchangeStatus.Refunded -> warningBanner(R.string.express_exchange_status_refunded)
    ExpressExchangeStatus.Paused -> warningBanner(R.string.express_exchange_status_paused)
    ExpressExchangeStatus.Failed,
    ExpressExchangeStatus.TxFailed,
    -> failedBanner()
    ExpressExchangeStatus.Expired -> errorBanner(R.string.express_exchange_status_failed)
    ExpressExchangeStatus.Finished -> successBanner(R.string.express_exchange_status_exchanged)
    ExpressExchangeStatus.Unknown -> null
}

/**
 * Express onramp status → the status plaque under the two-asset block. Same severity mapping as the swap variant; the
 * [Finished][ExpressOnrampStatus.Finished] success ("Purchase completed") is the only [Severity.Success] (auto-collapsed).
 */
private fun ExpressOnrampStatus.toStatusBannerUM(): TxHistoryDetailsUM.StatusBannerUM? = when (this) {
    ExpressOnrampStatus.Created,
    ExpressOnrampStatus.WaitingForPayment,
    -> loadingBanner(R.string.express_exchange_status_receiving_active)
    ExpressOnrampStatus.PaymentProcessing -> loadingBanner(R.string.express_exchange_status_confirming_active)
    ExpressOnrampStatus.Verifying -> verificationBanner()
    ExpressOnrampStatus.Paid -> loadingBanner(R.string.express_exchange_status_buying_active)
    ExpressOnrampStatus.Sending -> loadingBanner(R.string.express_exchange_status_sending_active)
    ExpressOnrampStatus.Paused -> warningBanner(R.string.express_exchange_status_paused)
    ExpressOnrampStatus.Failed -> failedBanner()
    ExpressOnrampStatus.Expired -> errorBanner(R.string.express_exchange_status_failed)
    ExpressOnrampStatus.Finished -> successBanner(R.string.express_exchange_status_bought)
    ExpressOnrampStatus.Unknown -> null
}

private fun loadingBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    severity = Severity.Info,
    title = resourceReference(title),
    isLoading = true,
)

private fun successBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    severity = Severity.Success,
    title = resourceReference(title),
    isLoading = false,
)

private fun warningBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    severity = Severity.Warning,
    title = resourceReference(title),
    isLoading = false,
)

private fun errorBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    severity = Severity.Error,
    title = resourceReference(title),
    isLoading = false,
)

/** Failure terminal: red plaque with the shared "visit provider to refund" hint. */
private fun failedBanner() = TxHistoryDetailsUM.StatusBannerUM(
    severity = Severity.Error,
    title = resourceReference(R.string.express_exchange_status_failed),
    subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
    isLoading = false,
)

/** KYC verification: amber plaque with the "visit provider for verification" hint. */
private fun verificationBanner() = TxHistoryDetailsUM.StatusBannerUM(
    severity = Severity.Warning,
    title = resourceReference(R.string.express_exchange_status_verifying),
    subtitle = resourceReference(R.string.express_exchange_notification_verification_text),
    isLoading = false,
)

private fun Status.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference = when (this) {
    is Status.Failed -> resourceReference(R.string.common_action_failed, wrappedList(resourceReference(pending)))
    is Status.Unconfirmed -> resourceReference(pending)
    is Status.Confirmed -> resourceReference(confirmed)
}

// endregion

// region Info rows (network fee)

/** Detail rows of an on-chain tx: the network-fee row when a fee with a value is present (rate is not surfaced). */
private fun TxInfo.toInfoRows(): ImmutableList<TxHistoryDetailsUM.InfoRowUM> = listOfNotNull(feeRow()).toImmutableList()

/**
 * Detail rows of an express op: the [provider] row (its name) followed by the network-fee row from the matched on-chain
 * leg. The provider row is dropped while the provider is unresolved; the fee row while no on-chain leg / fee is present.
 * (Rate is not surfaced yet — no data.)
 */
private fun ExpressTx.toInfoRows(): ImmutableList<TxHistoryDetailsUM.InfoRowUM> = buildList {
    provider?.let { add(it.providerRow()) }
    addAll(txInfo.toInfoRows())
}.toImmutableList()

private fun ExpressProvider.providerRow(): TxHistoryDetailsUM.InfoRowUM = TxHistoryDetailsUM.InfoRowUM(
    label = resourceReference(R.string.express_provider),
    value = stringReference(name),
    trailingIconRes = R.drawable.ic_arrow_top_right_24,
)

/** Detail rows pulled from the matched on-chain leg of an express op; empty while the leg has not loaded. */
private fun OnChainTx?.toInfoRows(): ImmutableList<TxHistoryDetailsUM.InfoRowUM> =
    (this as? OnChainTx.BSDK)?.txInfo?.toInfoRows() ?: persistentListOf()

private fun TxInfo.feeRow(): TxHistoryDetailsUM.InfoRowUM? {
    val fee = fee ?: return null
    val value = fee.value ?: return null
    return TxHistoryDetailsUM.InfoRowUM(
        label = resourceReference(R.string.common_network_fee_title),
        value = stringReference(
            value.format { crypto(symbol = fee.currencySymbol, decimals = fee.decimals, ignoreSymbolPosition = true) },
        ),
    )
}

// endregion

// region Amount building helpers

/** Leading sign of the pay-in / "You send" leg: `−` while in flight or settled, dropped on a failed deal. */
private fun Status.outgoingSign(): String = if (this is Status.Failed) "" else "${StringsSigns.MINUS} "

/**
 * Leading sign of the payout / "You receive" leg: `~` while in flight (the final received amount is still an estimate),
 * `+` once the funds have settled, and dropped on a failed deal (the amount is then only struck through).
 */
private fun Status.incomingSign(): String = when (this) {
    is Status.Unconfirmed -> "${StringsSigns.TILDE_SIGN} "
    is Status.Confirmed -> "${StringsSigns.PLUS} "
    is Status.Failed -> ""
}

/**
 * Signed crypto amount with inline symbol, e.g. `+ 350.31 USDT` / `- 350.31 USDT`. The sign is `-` for outgoing, `+`
 * otherwise, and is dropped for zero amounts and for the failed state (a failed tx moved nothing) — the UI then only
 * strikes the amount through and dims it via [TxHistoryDetailsUM.AmountBlockUM.isFailed].
 */
private fun TxInfo.signedAmount(currency: CryptoCurrency): String {
    val formatted = amount.format { crypto(cryptoCurrency = currency, ignoreSymbolPosition = true) }
    val prefix = when {
        status is TxInfo.TransactionStatus.Failed -> ""
        amount.isZero() -> ""
        isOutgoing -> "${StringsSigns.MINUS} "
        else -> "${StringsSigns.PLUS} "
    }
    return (prefix + formatted).trim()
}

// endregion

// region Header building helpers

/** Type glyph. Unlike the history list, the failed state keeps the type glyph (only the color changes). */
private fun TxInfo.headerIcon(): Int = when (type) {
    is TransactionType.Swap -> R.drawable.ic_exchange_vertical_24
    else -> if (isOutgoing) R.drawable.ic_arrow_up_24 else R.drawable.ic_arrow_down_24
}

private fun headerSubtitle(timestampMillis: Long): TextReference {
    val dateTime = DateTime(timestampMillis)
    val date = DateTimeFormatters.dateMMMdYYYY.print(dateTime)
    val time = DateTimeFormatters.timeFormatter.print(dateTime)
    return stringReference("$date, $time")
}

private fun TxInfo.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference = when (status) {
    is TxInfo.TransactionStatus.Failed ->
        resourceReference(R.string.common_action_failed, wrappedList(resourceReference(pending)))
    is TxInfo.TransactionStatus.Unconfirmed -> resourceReference(pending)
    is TxInfo.TransactionStatus.Confirmed -> resourceReference(confirmed)
}

private fun TxInfo.TransactionStatus.toUiStatus(): Status = when (this) {
    TxInfo.TransactionStatus.Confirmed -> Status.Confirmed
    TxInfo.TransactionStatus.Failed -> Status.Failed
    TxInfo.TransactionStatus.Unconfirmed -> Status.Unconfirmed
}

// endregion