package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayTransactionState
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import org.joda.time.DateTimeZone

internal class TangemPayTxHistoryItemsConverter(
    private val txHistoryUiActions: TangemPayTxHistoryUiActions,
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

    private fun convertSpend(spend: TangemPayTxHistoryItem.Spend): TangemPayTransactionState.Content.Spend {
        val localDate = spend.date.withZone(DateTimeZone.getDefault())
        val amountPrefix = when {
            spend.amount.isZero() -> ""
            spend.status == TangemPayTxHistoryItem.Status.DECLINED || spend.amount.isPositive() -> StringsSigns.MINUS
            else -> StringsSigns.PLUS
        }
        val amount = amountPrefix + spend.amount.abs().format {
            fiat(fiatCurrencyCode = spend.currency.currencyCode, fiatCurrencySymbol = spend.currency.symbol)
        }
        return TangemPayTransactionState.Content.Spend(
            id = spend.id,
            onClick = { txHistoryUiActions.onTransactionClick(spend) },
            amount = amount,
            amountColor = themedColor {
                when {
                    spend.status == TangemPayTxHistoryItem.Status.DECLINED -> TangemTheme.colors.text.warning
                    amountPrefix == StringsSigns.PLUS -> TangemTheme.colors.text.accent
                    else -> TangemTheme.colors.text.primary1
                }
            },
            title = stringReference(spend.enrichedMerchantName ?: spend.merchantName),
            subtitle = paySpendSubtitleConverter.convert(spend),
            time = DateTimeFormatters.formatDate(localDate, DateTimeFormatters.timeFormatter),
            icon = spend.enrichedMerchantIconUrl?.let(ImageReference::Url)
                ?: ImageReference.Res(R.drawable.ic_category_24),
        )
    }

    private fun convertPayment(payment: TangemPayTxHistoryItem.Payment): TangemPayTransactionState.Content.Payment {
        val amount = StringsSigns.MINUS + payment.amount.format {
            fiat(fiatCurrencyCode = payment.currency.currencyCode, fiatCurrencySymbol = payment.currency.symbol)
        }
        return TangemPayTransactionState.Content.Payment(
            id = payment.id,
            onClick = { txHistoryUiActions.onTransactionClick(payment) },
            amount = amount,
            amountColor = themedColor { TangemTheme.colors.text.primary1 },
            title = resourceReference(R.string.tangem_pay_withdrawal),
            subtitle = stringReference("Transfers"),
            time = DateTimeFormatters.formatDate(payment.date, DateTimeFormatters.timeFormatter),
            icon = ImageReference.Res(R.drawable.ic_arrow_up_24),
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
            amountColor = themedColor { TangemTheme.colors.text.primary1 },
            title = resourceReference(R.string.tangem_pay_fee_title),
            subtitle = fee.description?.let(::stringReference) ?: resourceReference(R.string.tangem_pay_fee_subtitle),
            icon = ImageReference.Res(R.drawable.ic_percent_24),
            time = DateTimeFormatters.formatDate(fee.date, DateTimeFormatters.timeFormatter),
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
            amountColor = when (collateral.type) {
                TangemPayTxHistoryItem.Type.Deposit -> themedColor { TangemTheme.colors.text.accent }
                TangemPayTxHistoryItem.Type.Withdrawal -> themedColor { TangemTheme.colors.text.primary1 }
            },
            title = when (collateral.type) {
                TangemPayTxHistoryItem.Type.Deposit -> resourceReference(R.string.tangem_pay_deposit)
                TangemPayTxHistoryItem.Type.Withdrawal -> resourceReference(R.string.tangem_pay_withdrawal)
            },
            subtitle = resourceReference(R.string.common_transfer),
            icon = when (collateral.type) {
                TangemPayTxHistoryItem.Type.Deposit -> ImageReference.Res(R.drawable.ic_arrow_down_24)
                TangemPayTxHistoryItem.Type.Withdrawal -> ImageReference.Res(R.drawable.ic_arrow_up_24)
            },
            time = DateTimeFormatters.formatDate(collateral.date, DateTimeFormatters.timeFormatter),
        )
    }
}