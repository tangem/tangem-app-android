package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.capitalize
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import org.joda.time.DateTimeZone

internal class TangemPayTxHistoryItemsConverter(
    private val txHistoryUiActions: TxHistoryUiActions,
) : Converter<VisaTxHistoryItem, TransactionState> {
    override fun convert(value: VisaTxHistoryItem): TransactionState {
        val localDate = value.date.withZone(DateTimeZone.getDefault())
        val fiatAmount = value.fiatAmount.format {
            fiat(
                fiatCurrencyCode = value.fiatCurrency.currencyCode,
                fiatCurrencySymbol = value.fiatCurrency.symbol,
            )
        }

        return TransactionState.Content(
            txHash = value.id,
            amount = "${StringsSigns.MINUS}$fiatAmount",
            time = DateTimeFormatters.formatDate(localDate, DateTimeFormatters.timeFormatter),
            status = TransactionState.Content.Status.Confirmed,
            direction = TransactionState.Content.Direction.OUTGOING,
            iconRes = R.drawable.ic_arrow_up_24,
            title = stringReference(value = value.merchantName?.capitalize() ?: "Unknown merchant"),
            subtitle = stringReference("How to get merchant type?"),
            timestamp = localDate.millis,
            onClick = { txHistoryUiActions.openTxInExplorer(value.id) },
        )
    }
}