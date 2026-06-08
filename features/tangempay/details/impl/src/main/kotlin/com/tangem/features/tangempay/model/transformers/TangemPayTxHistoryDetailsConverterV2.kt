package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.ButtonState
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUMV2
import com.tangem.features.tangempay.entity.TransactionLabelUM
import com.tangem.features.tangempay.entity.TransactionStateType
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero

internal object TangemPayTxHistoryDetailsConverterV2 :
    Converter<TangemPayTxHistoryDetailsConverterV2.Input, TangemPayTxHistoryDetailsUMV2> {
    private val dateFormatter = DateTimeFormatters.getBestFormatterBySkeleton("MMM dd yyyy, HH:mm")

    override fun convert(value: Input): TangemPayTxHistoryDetailsUMV2 {
        val transaction = value.item
        return TangemPayTxHistoryDetailsUMV2(
            isBalanceHidden = value.isBalanceHidden,
            title = transaction.extractBottomSheetTitle(),
            subtitle = transaction.extractDate(),
            iconState = transaction.extractIcon(),
            transactionTitle = transaction.extractTransactionTitle(),
            transactionCategory = transaction.extractTransactionCategory(),
            mcc = transaction.extractMcc(),
            transactionAmount = transaction.extractAmount(),
            localTransactionText = transaction.extractLocalAmount(),
            label = transaction.extractTransactionLabel(),
            buttonState = value.extractButtonState(),
            dismiss = value.onDismiss,
        )
    }

    private fun TangemPayTxHistoryItem.extractDate(): TextReference {
        val date = DateTimeFormatters.formatDate(this.date, dateFormatter)
        return stringReference(date)
    }

    private fun TangemPayTxHistoryItem.extractBottomSheetTitle(): TextReference {
        return when (this) {
            is TangemPayTxHistoryItem.Spend -> resourceReference(R.string.tangem_pay_purchase)
            is TangemPayTxHistoryItem.Payment -> resourceReference(R.string.tangem_pay_withdrawal)
            is TangemPayTxHistoryItem.Collateral -> when (this.type) {
                TangemPayTxHistoryItem.Type.Deposit -> resourceReference(R.string.tangem_pay_deposit)
                TangemPayTxHistoryItem.Type.Withdrawal -> resourceReference(R.string.tangem_pay_withdrawal)
            }
            is TangemPayTxHistoryItem.Fee -> resourceReference(R.string.tangem_pay_fee_title)
        }
    }

    private fun TangemPayTxHistoryItem.extractIcon(): TangemIconUM {
        return when (this) {
            is TangemPayTxHistoryItem.Collateral -> when (this.type) {
                TangemPayTxHistoryItem.Type.Deposit -> TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_down_24,
                    tintReference = {
                        TangemTheme.colors3.icon.secondary
                    },
                )
                TangemPayTxHistoryItem.Type.Withdrawal -> TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_up_24,
                    tintReference = {
                        TangemTheme.colors3.icon.secondary
                    },
                )
            }
            is TangemPayTxHistoryItem.Fee -> {
                TangemIconUM.Icon(
                    iconRes = R.drawable.ic_percent_24,
                    tintReference = {
                        TangemTheme.colors3.icon.secondary
                    },
                )
            }
            is TangemPayTxHistoryItem.Payment -> TangemIconUM.Icon(
                imageVector = Icons.ic_arrow_up_24,
                tintReference = {
                    TangemTheme.colors3.icon.secondary
                },
            )
            is TangemPayTxHistoryItem.Spend -> {
                val merchantIcon = this.enrichedMerchantIconUrl
                if (merchantIcon != null) {
                    TangemIconUM.Url(merchantIcon, R.drawable.ic_category_24)
                } else {
                    TangemIconUM.Icon(
                        iconRes = R.drawable.ic_category_24,
                        tintReference = {
                            TangemTheme.colors3.icon.secondary
                        },
                    )
                }
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractTransactionTitle(): TextReference {
        return when (this) {
            is TangemPayTxHistoryItem.Spend -> stringReference(this.enrichedMerchantName ?: this.merchantName)
            is TangemPayTxHistoryItem.Payment -> resourceReference(R.string.common_transfer)
            is TangemPayTxHistoryItem.Collateral -> resourceReference(R.string.common_transfer)
            is TangemPayTxHistoryItem.Fee -> {
                this.description?.let(::stringReference) ?: resourceReference(R.string.tangem_pay_fee_subtitle)
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractTransactionCategory(): TextReference {
        return when (this) {
            is TangemPayTxHistoryItem.Fee -> resourceReference(R.string.tangem_pay_fee_subtitle)
            is TangemPayTxHistoryItem.Payment -> resourceReference(R.string.common_transfer)
            is TangemPayTxHistoryItem.Collateral -> resourceReference(R.string.common_transfer)
            is TangemPayTxHistoryItem.Spend -> extractSpendCategory()
        }
    }

    private fun TangemPayTxHistoryItem.Spend.extractSpendCategory(): TextReference {
        val merchantCategory = merchantCategory
        val enrichedMerchantCategory = enrichedMerchantCategory
        return when {
            !merchantCategory.isNullOrEmpty() -> stringReference(merchantCategory)
            !enrichedMerchantCategory.isNullOrEmpty() -> stringReference(enrichedMerchantCategory)
            else -> resourceReference(R.string.tangem_pay_other)
        }
    }

    private fun TangemPayTxHistoryItem.extractMcc(): TextReference? {
        return when (this) {
            is TangemPayTxHistoryItem.Spend -> merchantCategoryCode
                ?.takeIf { it.isNotEmpty() }
                ?.let(::stringReference)
            is TangemPayTxHistoryItem.Fee,
            is TangemPayTxHistoryItem.Payment,
            is TangemPayTxHistoryItem.Collateral,
            -> null
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
                    this.status == TangemPayTxHistoryItem.Status.REVERSED -> StringsSigns.MINUS
                    this.status == TangemPayTxHistoryItem.Status.DECLINED ||
                        this.amount.isPositive() -> StringsSigns.MINUS
                    else -> StringsSigns.PLUS
                }
                val amount = when (this.status) {
                    TangemPayTxHistoryItem.Status.DECLINED -> this.authorizedAmount
                    else -> this.amount
                }
                val formattedAmount = amount.abs().format {
                    fiat(
                        fiatCurrencyCode = this@extractAmount.currency.currencyCode,
                        fiatCurrencySymbol = this@extractAmount.currency.symbol,
                    )
                }
                amountPrefix + formattedAmount
            }
            is TangemPayTxHistoryItem.Collateral -> {
                val amountPrefix = when {
                    this.amount.isZero() -> ""
                    this.amount.isPositive() -> StringsSigns.PLUS
                    else -> StringsSigns.MINUS
                }
                val amount = this.amount.abs().format {
                    fiat(
                        fiatCurrencyCode = this@extractAmount.currency.currencyCode,
                        fiatCurrencySymbol = this@extractAmount.currency.symbol,
                    )
                }
                amountPrefix + amount
            }
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
                    val amountPrefix = when {
                        localAmount.isZero() -> ""
                        this.status == TangemPayTxHistoryItem.Status.DECLINED ||
                            localAmount.isPositive() -> StringsSigns.MINUS
                        else -> StringsSigns.PLUS
                    }
                    val amount = localAmount.abs().format {
                        fiat(
                            fiatCurrencyCode = localCurrency.currencyCode,
                            fiatCurrencySymbol = localCurrency.symbol,
                        ).price()
                    }
                    amountPrefix + amount
                } else {
                    null
                }
            }
        }
    }

    private fun TangemPayTxHistoryItem.extractTransactionLabel(): TransactionLabelUM? {
        return when (this) {
            is TangemPayTxHistoryItem.Payment,
            is TangemPayTxHistoryItem.Collateral,
            -> {
                TransactionLabelUM(
                    transactionStateType = TransactionStateType.Completed,
                    icon = TangemIconUM.Icon(
                        imageVector = Icons.ic_success_24,
                        tintReference = { TangemTheme.colors3.icon.status.success },
                    ),
                    title = resourceReference(R.string.tangem_pay_status_completed),
                )
            }
            is TangemPayTxHistoryItem.Fee -> TransactionLabelUM(
                transactionStateType = TransactionStateType.Completed,
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_success_24,
                    tintReference = { TangemTheme.colors3.icon.status.success },
                ),
                title = resourceReference(R.string.tangem_pay_status_completed),
                subtitle = resourceReference(R.string.tangem_pay_transaction_fee_notification_text),
            )
            is TangemPayTxHistoryItem.Spend -> when (this.status) {
                TangemPayTxHistoryItem.Status.COMPLETED -> TransactionLabelUM(
                    transactionStateType = TransactionStateType.Completed,
                    icon = TangemIconUM.Icon(
                        imageVector = Icons.ic_success_24,
                        tintReference = { TangemTheme.colors3.icon.status.success },
                    ),
                    title = resourceReference(R.string.tangem_pay_status_completed),
                )
                TangemPayTxHistoryItem.Status.PENDING,
                TangemPayTxHistoryItem.Status.RESERVED,
                -> TransactionLabelUM(
                    transactionStateType = TransactionStateType.InProgress,
                    icon = TangemIconUM.Icon(
                        imageVector = Icons.ic_clock_24,
                        tintReference = { TangemTheme.colors3.icon.status.info },
                    ),
                    title = resourceReference(R.string.tangem_pay_status_pending),
                )
                TangemPayTxHistoryItem.Status.DECLINED -> TransactionLabelUM(
                    transactionStateType = TransactionStateType.Rejected,
                    icon = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_warning_20,
                        tintReference = { TangemTheme.colors3.icon.status.error },
                    ),
                    title = resourceReference(R.string.tangem_pay_status_declined),
                    subtitle = extractDeclinedSubtitle(),
                )
                TangemPayTxHistoryItem.Status.REVERSED -> TransactionLabelUM(
                    transactionStateType = TransactionStateType.Reversed,
                    icon = TangemIconUM.Icon(
                        imageVector = Icons.ic_success_24,
                        tintReference = { TangemTheme.colors3.icon.status.success },
                    ),
                    title = resourceReference(R.string.tangem_pay_status_reversed),
                    subtitle = resourceReference(R.string.tangem_pay_transaction_reversed_notification_text),
                )
                TangemPayTxHistoryItem.Status.UNKNOWN -> null
            }
        }
    }

    private fun TangemPayTxHistoryItem.Spend.extractDeclinedSubtitle(): TextReference {
        val reason = declinedReason
        return if (reason.isNullOrEmpty()) {
            resourceReference(R.string.tangem_pay_transaction_declined_notification_text)
        } else {
            resourceReference(
                id = R.string.tangem_pay_history_item_spend_mc_declined_reason,
                formatArgs = wrappedList(TangemPayDeclinedReasonMapper.map(reason)),
            )
        }
    }

    private fun Input.extractButtonState(): ButtonState {
        return ButtonState(
            text = resourceReference(R.string.tangem_pay_get_help),
            onClick = this.onDisputeClick,
        )
    }

    data class Input(
        val item: TangemPayTxHistoryItem,
        val isBalanceHidden: Boolean,
        val onExplorerClick: (String?) -> Unit,
        val onDisputeClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}