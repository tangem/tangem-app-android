package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUM
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUM.ButtonState
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal object TangemPayTxHistoryDetailsConverter :
    Converter<TangemPayTxHistoryDetailsConverter.Input, TangemPayTxHistoryDetailsUM> {
    private val dateFormatter = DateTimeFormatters.getBestFormatterBySkeleton("dd MMMM")

    override fun convert(value: Input): TangemPayTxHistoryDetailsUM {
        val transaction = value.item
        return TangemPayTxHistoryDetailsUM(
            title = transaction.extractDate(),
            iconState = transaction.extractIcon(),
            transactionTitle = transaction.extractTransactionTitle(),
            transactionSubtitle = transaction.extractTransactionSubtitle(),
            transactionAmount = transaction.extractAmount(),
            transactionAmountColor = transaction.extractAmountColor(),
            localTransactionText = transaction.extractLocalAmount(),
            labelState = transaction.extractLabel(),
            notification = transaction.extractNotification(),
            buttons = value.extractButtonsState(),
            dismiss = value.onDismiss,
        )
    }

    private fun TangemPayTxHistoryItem.extractDate(): TextReference {
        val date = DateTimeFormatters.formatDate(this.date, dateFormatter)
        val time = DateTimeFormatters.formatDate(this.date, DateTimeFormatters.timeFormatter)

        return stringReference("$date ${StringsSigns.DOT} $time")
    }

    private fun TangemPayTxHistoryItem.extractIcon(): ImageReference {
        return when (this) {
            is TangemPayTxHistoryItem.Collateral -> ImageReference.Res(R.drawable.ic_arrow_down_24)
            is TangemPayTxHistoryItem.Fee -> ImageReference.Res(R.drawable.ic_percent_24)
            is TangemPayTxHistoryItem.Payment -> ImageReference.Res(R.drawable.ic_arrow_up_24)
            is TangemPayTxHistoryItem.Spend -> {
                val merchantIcon = this.enrichedMerchantIconUrl
                if (merchantIcon != null) {
                    ImageReference.Url(merchantIcon)
                } else {
                    ImageReference.Res(R.drawable.ic_category_24)
                }
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractTransactionTitle(): TextReference {
        return when (this) {
            is TangemPayTxHistoryItem.Spend -> stringReference(this.enrichedMerchantName ?: this.merchantName)
            is TangemPayTxHistoryItem.Payment -> resourceReference(R.string.tangem_pay_withdrawal)
            is TangemPayTxHistoryItem.Collateral -> resourceReference(R.string.tangem_pay_deposit)
            is TangemPayTxHistoryItem.Fee -> {
                this.description?.let(::stringReference) ?: resourceReference(R.string.tangem_pay_fee_title)
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractTransactionSubtitle(): TextReference {
        return when (this) {
            is TangemPayTxHistoryItem.Fee -> resourceReference(R.string.tangem_pay_fee_subtitle)
            is TangemPayTxHistoryItem.Payment -> resourceReference(R.string.common_transfer)
            is TangemPayTxHistoryItem.Collateral -> resourceReference(R.string.common_transfer)
            is TangemPayTxHistoryItem.Spend -> {
                val subtitle = merchantCategory ?: enrichedMerchantCategory
                subtitle?.let(::stringReference) ?: resourceReference(R.string.tangem_pay_other)
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractAmount(): String {
        return when (this) {
            is TangemPayTxHistoryItem.Fee,
            is TangemPayTxHistoryItem.Payment,
            -> {
                val amount = this.amount.format {
                    fiat(
                        fiatCurrencyCode = this@extractAmount.currency.currencyCode,
                        fiatCurrencySymbol = this@extractAmount.currency.symbol,
                    )
                }
                StringsSigns.MINUS + amount
            }
            is TangemPayTxHistoryItem.Spend -> {
                val amountPrefix = when {
                    this.amount.isZero() -> ""
                    this.status == TangemPayTxHistoryItem.Status.DECLINED -> ""
                    else -> StringsSigns.MINUS
                }
                val amount = this.amount.format {
                    fiat(
                        fiatCurrencyCode = this@extractAmount.currency.currencyCode,
                        fiatCurrencySymbol = this@extractAmount.currency.symbol,
                    )
                }
                amountPrefix + amount
            }
            is TangemPayTxHistoryItem.Collateral -> {
                val amount = this.amount.format {
                    fiat(
                        fiatCurrencyCode = this@extractAmount.currency.currencyCode,
                        fiatCurrencySymbol = this@extractAmount.currency.symbol,
                    )
                }
                StringsSigns.PLUS + amount
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractAmountColor(): ColorReference {
        return when (this) {
            is TangemPayTxHistoryItem.Fee,
            is TangemPayTxHistoryItem.Spend,
            is TangemPayTxHistoryItem.Payment,
            -> themedColor { TangemTheme.colors.text.primary1 }
            is TangemPayTxHistoryItem.Collateral -> themedColor { TangemTheme.colors.text.accent }
        }
    }

    private fun TangemPayTxHistoryItem.extractLocalAmount(): String? {
        return when (this) {
            is TangemPayTxHistoryItem.Collateral,
            is TangemPayTxHistoryItem.Fee,
            is TangemPayTxHistoryItem.Payment,
            -> null
            is TangemPayTxHistoryItem.Spend -> {
                val localCurrency = this.localCurrency
                val localAmount = this.localAmount
                if (localCurrency != null && localAmount != null && localCurrency != currency) {
                    localAmount.format {
                        fiat(
                            fiatCurrencyCode = localCurrency.currencyCode,
                            fiatCurrencySymbol = localCurrency.symbol,
                        ).price()
                    }
                } else {
                    null
                }
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractLabel(): LabelUM? {
        return when (this) {
            is TangemPayTxHistoryItem.Fee,
            is TangemPayTxHistoryItem.Payment,
            is TangemPayTxHistoryItem.Collateral,
            -> null
            is TangemPayTxHistoryItem.Spend -> when (this.status) {
                TangemPayTxHistoryItem.Status.COMPLETED -> LabelUM(
                    text = resourceReference(R.string.tangem_pay_status_completed),
                    style = LabelStyle.ACCENT,
                )
                TangemPayTxHistoryItem.Status.PENDING -> LabelUM(
                    text = resourceReference(R.string.tangem_pay_status_pending),
                    style = LabelStyle.REGULAR,
                    icon = com.tangem.core.ui.R.drawable.ic_clock_24,
                )
                TangemPayTxHistoryItem.Status.DECLINED -> LabelUM(
                    text = resourceReference(R.string.tangem_pay_status_declined),
                    style = LabelStyle.WARNING,
                )
                TangemPayTxHistoryItem.Status.RESERVED,
                TangemPayTxHistoryItem.Status.UNKNOWN,
                -> null
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractNotification(): NotificationConfig? {
        return when (this) {
            is TangemPayTxHistoryItem.Payment -> null
            is TangemPayTxHistoryItem.Collateral -> null
            is TangemPayTxHistoryItem.Fee -> NotificationConfig(
                title = resourceReference(R.string.tangem_pay_transaction_fee_notification_text),
                subtitle = TextReference.EMPTY,
                iconResId = R.drawable.ic_token_info_24,
            )
            is TangemPayTxHistoryItem.Spend -> when (this.status) {
                TangemPayTxHistoryItem.Status.DECLINED -> NotificationConfig(
                    title = resourceReference(R.string.tangem_pay_transaction_declined_notification_text),
                    subtitle = TextReference.EMPTY,
                    iconResId = R.drawable.ic_token_info_24,
                )
                TangemPayTxHistoryItem.Status.PENDING,
                TangemPayTxHistoryItem.Status.COMPLETED,
                TangemPayTxHistoryItem.Status.RESERVED,
                TangemPayTxHistoryItem.Status.UNKNOWN,
                -> null
            }
        }
    }

    private fun Input.extractButtonsState(): ImmutableList<ButtonState> {
        return when (this.item) {
            is TangemPayTxHistoryItem.Fee -> persistentListOf(
                ButtonState(
                    text = resourceReference(R.string.tangem_pay_dispute),
                    onClick = this.onDisputeClick,
                ),
            )
            is TangemPayTxHistoryItem.Spend -> persistentListOf(
                ButtonState(
                    text = resourceReference(R.string.tangem_pay_dispute),
                    onClick = this.onDisputeClick,
                ),
            )
            is TangemPayTxHistoryItem.Payment -> persistentListOf(
                ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = this.onDisputeClick,
                ),
            )
            is TangemPayTxHistoryItem.Collateral -> persistentListOf(
                ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = this.onDisputeClick,
                ),
            )
        }
    }

    data class Input(
        val item: TangemPayTxHistoryItem,
        val onExplorerClick: (String?) -> Unit,
        val onDisputeClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}