package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.core.ui.res.generated.icons.ic_share_android_24
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.joda.time.DateTime

// region Status helpers

/** Maps the domain [TxInfo.TransactionStatus] to the UI [Status] bucket that drives row title/icon/amount colors. */
internal fun TxInfo.TransactionStatus.toUiStatus(): Status = when (this) {
    TxInfo.TransactionStatus.Confirmed -> Status.Confirmed
    TxInfo.TransactionStatus.Failed -> Status.Failed
    TxInfo.TransactionStatus.Unconfirmed -> Status.Unconfirmed
}

/**
 * Status-aware action title: the [confirmed] label once settled, the [pending] label while in flight, and the
 * "{pending} failed" template on failure.
 */
internal fun Status.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference = when (this) {
    is Status.Failed -> resourceReference(R.string.common_action_failed, wrappedList(resourceReference(pending)))
    is Status.Unconfirmed -> resourceReference(pending)
    is Status.Confirmed -> resourceReference(confirmed)
}

/** [statusAwareTitle] keyed off an on-chain [TxInfo]'s status. */
internal fun TxInfo.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference =
    status.toUiStatus().statusAwareTitle(pending, confirmed)

// endregion

// region Express asset helpers

/** Ticker shown for an express leg: the resolved currency symbol, falling back to the network id while unresolved. */
internal val ExpressTransactionAsset.displaySymbol: String
    get() = cryptoCurrency?.symbol ?: id.networkId

/** Fiat currency code of an [Amount], falling back to its symbol when the amount is not a fiat type. */
internal val Amount.fiatCode: String
    get() = (type as? AmountType.FiatType)?.code ?: currencySymbol

// endregion

// region Details header helpers

/**
 * Header overflow context menu of the details card, shared by all transaction types. Each row is dropped when its
 * action is absent: "Transaction ID" (copy; dropped when [onCopyTxId] is `null` — no id to copy), "Share" (dropped
 * when [onShare] is `null`) and "Explore" (dropped when [onExplore] is `null`). An empty list leaves the header with
 * no "•••" button. Repeat / Hide are not part of this iteration.
 */
internal fun buildDetailsMenu(
    onCopyTxId: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onExplore: (() -> Unit)?,
): ImmutableList<TxHistoryDetailsUM.MenuItemUM> = buildList {
    onCopyTxId?.let { copy ->
        add(
            TxHistoryDetailsUM.MenuItemUM(
                icon = Icons.ic_copy_24,
                title = resourceReference(R.string.common_transaction_id),
                onClick = copy,
            ),
        )
    }
    onShare?.let { share ->
        add(
            TxHistoryDetailsUM.MenuItemUM(
                icon = Icons.ic_share_android_24,
                title = resourceReference(R.string.common_share),
                onClick = share,
            ),
        )
    }
    onExplore?.let { explore ->
        add(
            TxHistoryDetailsUM.MenuItemUM(
                icon = Icons.ic_globe_24,
                title = resourceReference(R.string.common_explore),
                onClick = explore,
            ),
        )
    }
}.toImmutableList()

internal fun headerSubtitle(timestampMillis: Long): TextReference {
    val dateTime = DateTime(timestampMillis)
    val date = DateTimeFormatters.dateMMMdYYYY.print(dateTime)
    val time = DateTimeFormatters.timeFormatter.print(dateTime)
    return stringReference("$date, $time")
}

// endregion

// region Network-fee row

/**
 * Detail rows of an on-chain tx: the network-fee row when a fee with a value is present (rate is not surfaced). Shared
 * by the on-chain details card and the express card (which pulls the fee from its matched on-chain leg).
 */
internal fun TxInfo.toInfoRows(): ImmutableList<TxHistoryDetailsUM.InfoRowUM> =
    listOfNotNull(feeRow()).toImmutableList()

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