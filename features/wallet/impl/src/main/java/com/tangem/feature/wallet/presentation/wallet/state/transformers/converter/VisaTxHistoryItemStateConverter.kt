package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.extensions.capitalize
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.VisaWalletIntents
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import org.joda.time.DateTimeZone

internal class VisaTxHistoryItemStateConverter(
    private val visaCurrency: VisaCurrency,
    private val clickIntents: VisaWalletIntents,
) : Converter<VisaTxHistoryItem, TransactionState> {

    override fun convert(value: VisaTxHistoryItem): TransactionState {
        val localDate = value.date.withZone(DateTimeZone.getDefault())
        val time = DateTimeFormatters.formatDate(localDate, DateTimeFormatters.timeFormatter)
        val subtitle = "$time ${StringsSigns.DOT} ${value.status.capitalize()}"

        return TransactionState.Content(
            txHash = value.id,
            amount = value.amount.format { crypto(visaCurrency.symbol, visaCurrency.decimals) },
            // Show tx fiat amount instead of tx time
            time = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = value.fiatAmount,
                fiatCurrencyCode = value.fiatCurrency.currencyCode,
                fiatCurrencySymbol = value.fiatCurrency.symbol,
            ),
            status = TransactionState.Content.Status.Confirmed,
            direction = TransactionState.Content.Direction.INCOMING,
            iconRes = R.drawable.ic_arrow_up_24,
            title = stringReference(value = value.merchantName?.capitalize() ?: "Unknown merchant"),
            subtitle = stringReference(subtitle),
            timestamp = localDate.millis,
            onClick = { clickIntents.onVisaTransactionClick(value.id) },
        )
    }
}