package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.capitalize
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import org.joda.time.DateTimeZone
import java.util.Currency

internal class TangemPayTxHistoryItemsConverter(
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<TangemPayTxHistoryItem, TransactionState> {
    override fun convert(value: TangemPayTxHistoryItem): TransactionState {
        val localDate = value.date?.withZone(DateTimeZone.getDefault())
        val currency = Currency.getInstance(value.currency)
        val amount = value.amount.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }

        return TransactionState.Content(
            txHash = value.id,
            amount = "${StringsSigns.MINUS}$amount",
            time = localDate?.let { DateTimeFormatters.formatDate(it, DateTimeFormatters.timeFormatter) } ?: "",
            status = TransactionState.Content.Status.Confirmed,
            direction = TransactionState.Content.Direction.OUTGOING,
            iconRes = R.drawable.ic_arrow_up_24,
            title = stringReference(value = value.merchantName?.capitalize() ?: "Unknown merchant"),
            subtitle = stringReference("How to get merchant type?"),
            timestamp = localDate?.millis ?: 0,
            onClick = { txHistoryUiActions.openTxInExplorer(value.id) },
        )
    }
}