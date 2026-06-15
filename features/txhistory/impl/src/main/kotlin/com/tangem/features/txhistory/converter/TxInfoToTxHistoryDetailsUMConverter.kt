package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
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
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.toBriefAddressFormat
import org.joda.time.DateTime

/**
 * Converts a [TxInfo] to a [TxHistoryDetailsUM] for the in-app transaction details card.
 *
 * Single dispatch on [TxInfo.type] picks the layout family — mirroring the same `when(type)` used by
 * [TxHistoryItemToTransactionItemUMConverter]:
 * - [TransactionType.Swap] (and onramp once it lands in `TxInfo`) -> [TxHistoryDetailsUM.TwoAssets]
 * - everything else -> [TxHistoryDetailsUM.SingleAsset]
 */
internal class TxInfoToTxHistoryDetailsUMConverter(
    private val currency: CryptoCurrency,
    private val onCopyAddress: (String) -> Unit,
) : Converter<TxInfo, TxHistoryDetailsUM> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: TxInfo): TxHistoryDetailsUM = when (value.type) {
        is TransactionType.Swap -> TxHistoryDetailsUM.TwoAssets(header = value.toHeaderUM())
        else -> TxHistoryDetailsUM.SingleAsset(
            header = value.toHeaderUM(),
            amountBlock = value.toAmountBlockUM(),
            counterparty = value.toCounterpartyUM(),
            // TODO: TxInfo has no network fee / rate yet — empty until those fields are added to TxInfo.
            rows = emptyList(),
        )
    }

    private fun TxInfo.toHeaderUM(): TxHistoryDetailsUM.HeaderUM = TxHistoryDetailsUM.HeaderUM(
        iconRes = headerIcon(),
        status = status.toUiStatus(),
        title = headerTitle(),
        subtitle = headerSubtitle(),
    )

    private fun TxInfo.toAmountBlockUM(): TxHistoryDetailsUM.AmountBlockUM = TxHistoryDetailsUM.AmountBlockUM(
        currencyIcon = iconStateConverter.convert(currency),
        amount = stringReference(signedAmount(currency)),
        // TODO: TxInfo has no fiat amount yet — placeholder until the fiat field is added to TxInfo.
        fiatAmount = stringReference("\$0.00"),
        isFailed = status is TxInfo.TransactionStatus.Failed,
    )

    /**
     * Counterparty card ("Recipient" / "From"). Currently only the external-address avatar is produced — built from
     * the `User` interaction address (the same source the history list uses for its external-address subtitle).
     *
     * The own-account / own-wallet avatars ([TxHistoryDetailsUM.CounterpartyAvatar.Account] / `Wallet`) require the
     * address->owner lookup the list assembles in `TxHistoryLookupContext`; wiring that into the detail model is a
     * follow-up, so for now a counterparty that is not a plain external `User` address yields no card (`null`).
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
}

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

private fun TxInfo.headerSubtitle(): TextReference {
    val dateTime = DateTime(timestampInMillis)
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

private fun TxInfo.TransactionStatus.toUiStatus(): TransactionItemUM.Content.Status = when (this) {
    TxInfo.TransactionStatus.Confirmed -> TransactionItemUM.Content.Status.Confirmed
    TxInfo.TransactionStatus.Failed -> TransactionItemUM.Content.Status.Failed
    TxInfo.TransactionStatus.Unconfirmed -> TransactionItemUM.Content.Status.Unconfirmed
}

// endregion