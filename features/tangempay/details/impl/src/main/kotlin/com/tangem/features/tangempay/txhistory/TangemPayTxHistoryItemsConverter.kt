package com.tangem.features.tangempay.txhistory

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import org.joda.time.DateTimeZone

internal class TangemPayTxHistoryItemsConverter(
    private val txHistoryUiActions: TangemPayTxHistoryUiActions,
    private val isCashbackEnabled: Boolean,
) : Converter<TangemPayTxHistoryItem, TangemPayTransactionState.Content> {

    private val paySpendSubtitleConverter = PaySpendSubtitleConverter

    override fun convert(value: TangemPayTxHistoryItem): TangemPayTransactionState.Content {
        return when (value) {
            is TangemPayTxHistoryItem.Spend -> convertSpend(spend = value)
            is TangemPayTxHistoryItem.Payment -> convertPayment(payment = value)
            is TangemPayTxHistoryItem.Fee -> convertFee(fee = value)
            is TangemPayTxHistoryItem.Collateral -> convertCollateral(collateral = value)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun convertSpend(spend: TangemPayTxHistoryItem.Spend): TangemPayTransactionState.Content.Spend {
        val localDate = spend.date.withZone(DateTimeZone.getDefault())
        val amountPrefix = when {
            spend.amount.isZero() -> ""
            spend.status == TangemPayTxHistoryItem.Status.REVERSED -> StringsSigns.MINUS
            spend.status == TangemPayTxHistoryItem.Status.DECLINED || spend.amount.isPositive() -> StringsSigns.MINUS
            else -> StringsSigns.PLUS
        }
        val amount = when (spend.status) {
            TangemPayTxHistoryItem.Status.DECLINED -> spend.authorizedAmount
            else -> spend.amount
        }
        val formattedAmount = amountPrefix + amount.abs().format {
            fiat(fiatCurrencyCode = spend.currency.currencyCode, fiatCurrencySymbol = spend.currency.symbol)
        }
        return TangemPayTransactionState.Content.Spend(
            id = spend.id,
            onClick = { txHistoryUiActions.onTransactionClick(spend) },
            amount = formattedAmount,
            amountColor = {
                if (spend.status == TangemPayTxHistoryItem.Status.DECLINED) {
                    TangemTheme.colors3.text.status.error
                } else {
                    TangemTheme.colors3.text.primary
                }
            },
            title = stringReference(spend.enrichedMerchantName ?: spend.merchantName),
            subtitle = paySpendSubtitleConverter.convert(spend),
            time = DateTimeFormatters.formatDate(localDate, DateTimeFormatters.timeFormatter),
            icon = if (spend.enrichedMerchantIconUrl != null) {
                TangemIconUM.Url(
                    url = spend.enrichedMerchantIconUrl,
                    fallbackRes = R.drawable.ic_category_24,
                )
            } else {
                TangemIconUM.Icon(
                    iconRes = R.drawable.ic_category_24,
                    tintReference = { TangemTheme.colors3.icon.secondary },
                )
            },
            cashback = buildCashbackUm(spend.cashback),
        )
    }

    /**
     * Maps [cashback] to the inline badge UM, or `null` to hide it. Hidden when the feature toggle is
     * off, cashback is absent, excluded/awaiting/unknown, or has a zero/absent amount.
     */
    private fun buildCashbackUm(cashback: TangemPayTxHistoryItem.Cashback?): TangemPayTransactionCashbackUM? {
        if (!isCashbackEnabled || cashback == null) return null
        return when (cashback.status) {
            TangemPayTxHistoryItem.Cashback.Status.CONFIRMED ->
                cashback.toBadgeUm(TangemPayTransactionCashbackUM.Style.Confirmed)
            TangemPayTxHistoryItem.Cashback.Status.ESTIMATED ->
                cashback.toBadgeUm(TangemPayTransactionCashbackUM.Style.Estimated)
            TangemPayTxHistoryItem.Cashback.Status.EXCLUDED,
            TangemPayTxHistoryItem.Cashback.Status.AWAITING_CALCULATION,
            TangemPayTxHistoryItem.Cashback.Status.UNKNOWN,
            -> null
        }
    }

    private fun TangemPayTxHistoryItem.Cashback.toBadgeUm(
        style: TangemPayTransactionCashbackUM.Style,
    ): TangemPayTransactionCashbackUM? {
        val amount = amount?.takeUnless { it.isZero() }
        val currency = currency
        if (amount == null || currency == null) return null
        val prefix = if (amount.signum() < 0) StringsSigns.MINUS else StringsSigns.PLUS
        val formatted = amount.abs().format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
        return TangemPayTransactionCashbackUM(amount = prefix + formatted, style = style)
    }

    private fun convertPayment(payment: TangemPayTxHistoryItem.Payment): TangemPayTransactionState.Content.Payment {
        val amount = StringsSigns.MINUS + payment.amount.format {
            fiat(fiatCurrencyCode = payment.currency.currencyCode, fiatCurrencySymbol = payment.currency.symbol)
        }
        return TangemPayTransactionState.Content.Payment(
            id = payment.id,
            onClick = { txHistoryUiActions.onTransactionClick(payment) },
            amount = amount,
            amountColor = { TangemTheme.colors3.text.primary },
            title = resourceReference(R.string.tangem_pay_withdrawal),
            subtitle = stringReference("Transfers"),
            time = DateTimeFormatters.formatDate(payment.date, DateTimeFormatters.timeFormatter),
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_arrow_up_24,
                tintReference = { TangemTheme.colors3.icon.secondary },
            ),
        )
    }

    private fun convertFee(fee: TangemPayTxHistoryItem.Fee): TangemPayTransactionState.Content.Fee {
        val amountPrefix = if (fee.amount.isZero()) "" else StringsSigns.MINUS
        val amount = amountPrefix + fee.amount.format {
            fiat(fiatCurrencyCode = fee.currency.currencyCode, fiatCurrencySymbol = fee.currency.symbol)
        }
        return TangemPayTransactionState.Content.Fee(
            id = fee.id,
            onClick = { txHistoryUiActions.onTransactionClick(fee) },
            amount = amount,
            amountColor = { TangemTheme.colors3.text.primary },
            title = resourceReference(R.string.tangem_pay_fee_title),
            subtitle = fee.description?.let(::stringReference) ?: resourceReference(R.string.tangem_pay_fee_subtitle),
            time = DateTimeFormatters.formatDate(fee.date, DateTimeFormatters.timeFormatter),
            icon = TangemIconUM.Icon(
                iconRes = R.drawable.ic_percent_24,
                tintReference = { TangemTheme.colors3.icon.secondary },
            ),

        )
    }

    private fun convertCollateral(
        collateral: TangemPayTxHistoryItem.Collateral,
    ): TangemPayTransactionState.Content.Collateral {
        val amountPrefix = when {
            collateral.amount.isZero() -> ""
            collateral.amount.isPositive() -> StringsSigns.PLUS
            else -> StringsSigns.MINUS
        }
        val amount = amountPrefix + collateral.amount.abs().format {
            fiat(fiatCurrencyCode = collateral.currency.currencyCode, fiatCurrencySymbol = collateral.currency.symbol)
        }
        return TangemPayTransactionState.Content.Collateral(
            id = collateral.id,
            onClick = { txHistoryUiActions.onTransactionClick(collateral) },
            amount = amount,
            amountColor = { TangemTheme.colors3.text.primary },
            title = when (collateral.type) {
                TangemPayTxHistoryItem.Type.Deposit -> resourceReference(R.string.tangem_pay_deposit)
                TangemPayTxHistoryItem.Type.Withdrawal -> resourceReference(R.string.tangem_pay_withdrawal)
            },
            subtitle = resourceReference(R.string.common_transfer),
            time = DateTimeFormatters.formatDate(collateral.date, DateTimeFormatters.timeFormatter),
            icon = TangemIconUM.Icon(
                imageVector = when (collateral.type) {
                    TangemPayTxHistoryItem.Type.Deposit -> Icons.ic_arrow_down_24
                    TangemPayTxHistoryItem.Type.Withdrawal -> Icons.ic_arrow_up_24
                },
                tintReference = { TangemTheme.colors3.icon.secondary },
            ),
        )
    }
}