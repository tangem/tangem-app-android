package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Direction as RowDirection
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle.Direction as SubtitleDirection
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Maps an [ExpressTx] (swap / onramp) row directly to [TransactionItemUM.Content].
 *
 * The viewed leg is [CryptoCurrency] ([currency], the token-details currency): outgoing swap shows the pay-in
 * (`from`) amount with a minus, incoming swap / onramp shows the received (`to`) amount with a plus. The 26 typed
 * express statuses collapse into the three [Status] buckets (those drive title/icon/amount colors in the row UI).
 *
 * The counterparty ticker symbol+icon come from the resolved [ExpressTransactionAsset.cryptoCurrency] (swap);
 * onramp shows the real fiat code with no icon yet (fiat carries no `CryptoCurrency`). The row click opens the
 * explorer.
 */
internal class ExpressTxToTransactionItemUMConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<ExpressTx, TransactionItemUM> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: ExpressTx): TransactionItemUM = when (value) {
        is ExpressTx.Swap -> swapContent(value)
        is ExpressTx.Onramp -> onrampContent(value)
    }

    private fun swapContent(swap: ExpressTx.Swap): TransactionItemUM.Content {
        val status = swap.tx.status.toUiStatus()
        val viewedAmount = if (swap.isOutgoing) swap.tx.fromAsset.amount else swap.tx.toAsset.amount
        val counterparty = if (swap.isOutgoing) swap.tx.toAsset else swap.tx.fromAsset
        val prefix = when {
            status is Status.Failed -> ""
            swap.isOutgoing -> StringsSigns.MINUS
            else -> StringsSigns.PLUS
        }
        return buildContent(
            tx = swap,
            status = status,
            amount = formatAmount(viewedAmount, prefix),
            direction = if (swap.isOutgoing) RowDirection.OUTGOING else RowDirection.INCOMING,
            iconRes = R.drawable.ic_exchange_vertical_24,
            title = swapTitle(status),
            subtitle = ContentSubtitle.Asset(
                direction = if (swap.isOutgoing) SubtitleDirection.TO else SubtitleDirection.FROM,
                symbol = counterparty.cryptoCurrency?.symbol ?: counterparty.id.networkId,
                icon = counterparty.cryptoCurrency?.let(iconStateConverter::convert),
            ),
            // TODO: replace null to warning logic.
            warning = null,
        )
    }

    private fun onrampContent(onramp: ExpressTx.Onramp): TransactionItemUM.Content {
        val status = onramp.tx.status.toUiStatus()
        val prefix = when {
            status is Status.Failed -> ""
            status is Status.Confirmed -> StringsSigns.PLUS
            else -> StringsSigns.TILDE_SIGN
        }
        return buildContent(
            tx = onramp,
            status = status,
            amount = formatAmount(onramp.tx.toAsset.amount, prefix),
            direction = RowDirection.INCOMING,
            iconRes = R.drawable.ic_tangem_card_24,
            title = onrampTitle(status),
            subtitle = ContentSubtitle.Asset(
                direction = SubtitleDirection.FROM,
                symbol = onramp.tx.fromFiat.currencySymbol,
                // TODO: fiat carries no CryptoCurrency, so no icon yet — render with a fiat country flag once available.
                icon = null,
            ),
            // TODO: replace null to warning logic.
            warning = null,
        )
    }

    @Suppress("LongParameterList")
    private fun buildContent(
        tx: ExpressTx,
        status: Status,
        amount: String,
        direction: RowDirection,
        iconRes: Int,
        title: TextReference,
        subtitle: ContentSubtitle,
        warning: TextReference?,
    ): TransactionItemUM.Content {
        val explorerHash = tx.matchHash ?: tx.txId
        return TransactionItemUM.Content(
            txHash = explorerHash,
            amount = amount,
            currencySymbol = currency.symbol,
            time = tx.timestampMillis.toTimeFormat(),
            status = status,
            direction = direction,
            onClick = { txHistoryUiActions.openTxInExplorer(explorerHash) },
            iconRes = iconRes,
            title = title,
            subtitle = subtitle,
            timestamp = tx.timestampMillis,
            warning = warning,
        )
    }

    private fun formatAmount(amount: BigDecimal, prefix: String): String =
        prefix + amount.format { crypto(symbol = "", decimals = currency.decimals) }.trim()

    private fun swapTitle(status: Status): TextReference = when (status) {
        is Status.Confirmed -> resourceReference(R.string.common_swapped)
        is Status.Unconfirmed -> resourceReference(R.string.common_swapping)
        is Status.Failed ->
            resourceReference(R.string.common_action_failed, wrappedList(resourceReference(R.string.common_swapping)))
    }

    private fun onrampTitle(status: Status): TextReference = when (status) {
        is Status.Confirmed -> resourceReference(R.string.tx_history_onramp_topped_up)
        is Status.Unconfirmed -> resourceReference(R.string.tx_history_onramp_top_up)
        is Status.Failed -> resourceReference(
            R.string.common_action_failed,
            wrappedList(resourceReference(R.string.tx_history_onramp_top_up)),
        )
    }
}

// region Status mapping

/**
 * Collapses the typed swap status into a UI [Status] bucket: the single success state ([Finished][Confirmed]),
 * the failure/return states ([Failed]/[TxFailed]/[Refunded]/[Expired]/[Unknown]) → Failed, everything in flight
 * (incl. [Verifying] and [Paused]) → Unconfirmed.
 */
private fun ExpressExchangeStatus.toUiStatus(): Status = when (this) {
    ExpressExchangeStatus.Finished -> Status.Confirmed
    ExpressExchangeStatus.Failed,
    ExpressExchangeStatus.TxFailed,
    ExpressExchangeStatus.Refunded,
    ExpressExchangeStatus.Expired,
    ExpressExchangeStatus.Unknown,
    -> Status.Failed
    ExpressExchangeStatus.Preview,
    ExpressExchangeStatus.Created,
    ExpressExchangeStatus.ExchangeTxSent,
    ExpressExchangeStatus.Waiting,
    ExpressExchangeStatus.WaitingTxHash,
    ExpressExchangeStatus.Confirming,
    ExpressExchangeStatus.Exchanging,
    ExpressExchangeStatus.Sending,
    ExpressExchangeStatus.Verifying,
    ExpressExchangeStatus.Paused,
    -> Status.Unconfirmed
}

private fun ExpressOnrampStatus.toUiStatus(): Status = when (this) {
    ExpressOnrampStatus.Finished -> Status.Confirmed
    ExpressOnrampStatus.Failed,
    ExpressOnrampStatus.Expired,
    ExpressOnrampStatus.Unknown,
    -> Status.Failed
    ExpressOnrampStatus.Created,
    ExpressOnrampStatus.WaitingForPayment,
    ExpressOnrampStatus.PaymentProcessing,
    ExpressOnrampStatus.Verifying,
    ExpressOnrampStatus.Paid,
    ExpressOnrampStatus.Sending,
    ExpressOnrampStatus.Paused,
    -> Status.Unconfirmed
}

// endregion