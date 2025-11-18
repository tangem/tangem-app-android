package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.entity.TangemPayTransactionState
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import org.joda.time.DateTimeZone

internal class TangemPayTxHistoryItemsConverter(
    private val txHistoryUiActions: TangemPayTxHistoryUiActions,
) : Converter<TangemPayTxHistoryItem, TangemPayTransactionState.Content> {
    override fun convert(value: TangemPayTxHistoryItem): TangemPayTransactionState.Content {
        return when (value) {
            is TangemPayTxHistoryItem.Spend -> convertSpend(spend = value)
            is TangemPayTxHistoryItem.Payment -> convertPayment(payment = value)
            is TangemPayTxHistoryItem.Fee -> convertFee(fee = value)
        }
    }

    private fun convertSpend(spend: TangemPayTxHistoryItem.Spend): TangemPayTransactionState.Content.Spend {
        val localDate = spend.date.withZone(DateTimeZone.getDefault())
        val amountPrefix = when (spend.status) {
            TangemPayTxHistoryItem.Status.DECLINED -> ""
            else -> StringsSigns.MINUS
        }
        val amount = amountPrefix + spend.amount.format {
            fiat(fiatCurrencyCode = spend.currency.currencyCode, fiatCurrencySymbol = spend.currency.symbol)
        }
        return TangemPayTransactionState.Content.Spend(
            id = spend.id,
            onClick = { txHistoryUiActions.onTransactionClick(spend) },
            amount = amount,
            amountColor = {
                when (spend.status) {
                    TangemPayTxHistoryItem.Status.DECLINED -> TangemTheme.colors.text.warning
                    else -> TangemTheme.colors.text.primary1
                }
            },
            title = stringReference(spend.enrichedMerchantName ?: spend.merchantName),
            subtitle = stringReference(spend.enrichedMerchantCategory ?: spend.merchantCategory),
            time = DateTimeFormatters.formatDate(localDate, DateTimeFormatters.timeFormatter),
            iconUrl = spend.enrichedMerchantIconUrl,
        )
    }

    private fun convertPayment(payment: TangemPayTxHistoryItem.Payment): TangemPayTransactionState.Content.Payment {
        val amountPrefix = if (payment.amount.isPositive()) StringsSigns.PLUS else StringsSigns.MINUS
        val amount = amountPrefix + payment.amount.format {
            fiat(fiatCurrencyCode = payment.currency.currencyCode, fiatCurrencySymbol = payment.currency.symbol)
        }
        val title = if (payment.amount.isPositive()) "Deposit" else "Withdrawal"
        return TangemPayTransactionState.Content.Payment(
            id = payment.id,
            onClick = { txHistoryUiActions.onTransactionClick(payment) },
            amount = amount,
            amountColor = {
                if (payment.amount.isPositive()) {
                    TangemTheme.colors.text.accent
                } else {
                    TangemTheme.colors.text.primary1
                }
            },
            title = stringReference(title),
            subtitle = stringReference("Transfers"),
            time = DateTimeFormatters.formatDate(payment.date, DateTimeFormatters.timeFormatter),
            isIncome = payment.amount.isPositive(),
        )
    }

    private fun convertFee(fee: TangemPayTxHistoryItem.Fee): TangemPayTransactionState.Content.Fee {
        val amount = StringsSigns.MINUS + fee.amount.format {
            fiat(fiatCurrencyCode = fee.currency.currencyCode, fiatCurrencySymbol = fee.currency.symbol)
        }
        return TangemPayTransactionState.Content.Fee(
            id = fee.id,
            onClick = { txHistoryUiActions.onTransactionClick(fee) },
            amount = amount,
            amountColor = { TangemTheme.colors.text.primary1 },
            title = stringReference("Fee"),
            subtitle = stringReference("Service fees"),
            time = DateTimeFormatters.formatDate(fee.date, DateTimeFormatters.timeFormatter),
        )
    }
}