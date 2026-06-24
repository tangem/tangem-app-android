package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
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
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime

/**
 * Converts a [TxHistoryInfo] row to a [TxHistoryDetailsUM] for the in-app transaction details card.
 *
 * The dispatch mirrors the row converters: an [OnChainTx.BSDK] always renders as [TxHistoryDetailsUM.SingleAsset]
 * (a two-asset swap surfaces as [ExpressTx.Swap], handled separately), while an [ExpressTx] (swap / onramp) currently
 * produces a header-only [TxHistoryDetailsUM.TwoAssets] with the express status banner. The express legs (`from`/`to`
 * amounts, currencies, fiat) are populated in a follow-up ([REDACTED_TASK_KEY]).
 */
internal class TxHistoryInfoToTxHistoryDetailsUMConverter(
    private val currency: CryptoCurrency,
    private val onCopyAddress: (String) -> Unit,
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
        // TODO: TxInfo has no network fee / rate yet — empty until those fields are added to TxInfo.
        rows = persistentListOf(),
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
        // TODO: TxInfo has no fiat amount yet — empty until the fiat field is added to TxInfo; a hardcoded
        //  placeholder would show a misleading value.
        fiatAmount = TextReference.EMPTY,
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

    // endregion

    // region Express (swap / onramp)

    private fun convertExpressSwap(swap: ExpressTx.Swap): TxHistoryDetailsUM.TwoAssets {
        val status = exchangeStatusConverter.convert(swap.tx.status)
        return TxHistoryDetailsUM.TwoAssets(
            header = TxHistoryDetailsUM.HeaderUM(
                iconRes = R.drawable.ic_exchange_vertical_24,
                status = status,
                title = status.statusAwareTitle(R.string.common_swapping, R.string.common_swapped),
                subtitle = headerSubtitle(swap.timestampMillis),
            ),
            statusBanner = status.toStatusBannerUM(),
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
            statusBanner = status.toStatusBannerUM(),
        )
    }

    // endregion
}

// region Status helpers

/**
 * Express status plaque under the two-asset block, keyed on the collapsed UI [Status] bucket.
 *
 * A stopgap shared by on-chain swaps and express ops — [Severity.Warning] (verification) is not reachable here yet.
 * [REDACTED_TODO_COMMENT]
 */
private fun Status.toStatusBannerUM(): TxHistoryDetailsUM.StatusBannerUM = when (this) {
    is Status.Unconfirmed -> TxHistoryDetailsUM.StatusBannerUM(
        severity = Severity.Info,
        title = resourceReference(R.string.express_exchange_status_receiving_active),
        isLoading = true,
    )
    is Status.Confirmed -> TxHistoryDetailsUM.StatusBannerUM(
        severity = Severity.Success,
        title = resourceReference(R.string.express_exchange_status_exchanged),
        isLoading = false,
    )
    is Status.Failed -> TxHistoryDetailsUM.StatusBannerUM(
        severity = Severity.Error,
        title = resourceReference(R.string.express_exchange_status_failed),
        subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
        isLoading = false,
    )
}

private fun Status.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference = when (this) {
    is Status.Failed -> resourceReference(R.string.common_action_failed, wrappedList(resourceReference(pending)))
    is Status.Unconfirmed -> resourceReference(pending)
    is Status.Confirmed -> resourceReference(confirmed)
}

// endregion

// region Amount building helpers

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

private fun TxInfo.headerTitle(): TextReference = when (type) {
    is TransactionType.Swap -> statusAwareTitle(R.string.common_swapping, R.string.common_swapped)
    is TransactionType.Transfer -> statusAwareTitle(R.string.common_transfer, R.string.common_transferred)
    else -> stringReference(type.toString())
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