package com.tangem.features.send.v2.send.confirm.model.transformers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.fee.model.checkIfFeeTooHigh
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SendConfirmationNotificationsTransformer(
    private val feeUM: FeeUM,
    private val amountUM: AmountState,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val cryptoCurrency: CryptoCurrency,
    private val appCurrency: AppCurrency,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        val state = prevState as? ConfirmUM.Content ?: return prevState
        val feeUM = feeUM as? FeeUM.Content ?: return prevState
        return state.copy(
            sendingFooter = getSendingFooterText(),
            notifications = buildList {
                addTooHighNotification(feeUM.feeSelectorUM)
                addTooLowNotification(feeUM)
            }.toPersistentList(),
        )
    }

    private fun MutableList<NotificationUM>.addTooLowNotification(feeUM: FeeUM.Content) {
        val feeSelectorUM = feeUM.feeSelectorUM as? FeeSelectorUM.Content ?: return
        val multipleFees = feeSelectorUM.fees as? TransactionFee.Choosable ?: return
        val minimumValue = multipleFees.minimum.amount.value ?: return
        val customAmount = feeSelectorUM.customValues.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        if (feeSelectorUM.selectedType == FeeType.Custom && minimumValue > customValue) {
            add(NotificationUM.Warning.FeeTooLow)
            analyticsEventHandler.send(
                SendAnalyticEvents.NoticeTransactionDelays(cryptoCurrency.symbol),
            )
        }
    }

    private fun MutableList<NotificationUM>.addTooHighNotification(feeSelectorUM: FeeSelectorUM) {
        if (feeSelectorUM !is FeeSelectorUM.Content) return

        val (isFeeTooHigh, diff) = checkIfFeeTooHigh(feeSelectorUM)
        if (isFeeTooHigh) {
            add(NotificationUM.Warning.TooHigh(diff))
        }
    }

    private fun getSendingFooterText(): TextReference {
        val feeUM = feeUM as? FeeUM.Content
        val amountUM = amountUM as? AmountState.Data
        val fee = (feeUM?.feeSelectorUM as? FeeSelectorUM.Content)?.selectedFee

        if (fee == null || amountUM == null) return TextReference.EMPTY

        val fiatAmountValue = amountUM.amountTextField.fiatAmount.value
        val fiatFeeValue = feeUM.rate?.let { fee.amount.value?.multiply(it) }

        val fiatSendingValue = if (feeUM.isFeeConvertibleToFiat) {
            fiatFeeValue?.let { fiatAmountValue?.plus(it) }
        } else {
            fiatAmountValue
        }

        val fiatSending = fiatSendingValue.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
        val fiatFee = formatFiatFee(
            amount = fee.amount,
            isFeeConvertibleToFiat = feeUM.isFeeConvertibleToFiat,
            isFeeApproximate = feeUM.isFeeApproximate,
        )

        return if (feeUM.isTronToken && fee is Fee.Tron) {
            getTokenFeeSendingText(
                fee = fee,
                fiatFee = fiatFee,
                fiatSending = fiatSending,
            )
        } else {
            resourceReference(
                id = if (feeUM.isFeeConvertibleToFiat) {
                    R.string.send_summary_transaction_description
                } else {
                    R.string.send_summary_transaction_description_no_fiat_fee
                },
                formatArgs = wrappedList(fiatSending, fiatFee),
            )
        }
    }

    private fun formatFiatFee(amount: Amount?, isFeeConvertibleToFiat: Boolean, isFeeApproximate: Boolean): String {
        return if (isFeeConvertibleToFiat) {
            amount?.value.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            }
        } else {
            amount?.value.format {
                crypto(
                    decimals = amount?.decimals ?: 0,
                    symbol = amount?.currencySymbol.orEmpty(),
                ).fee(
                    canBeLower = isFeeApproximate,
                )
            }
        }
    }

    private fun getTokenFeeSendingText(fee: Fee.Tron, fiatFee: String, fiatSending: String): TextReference {
        val suffix = when {
            fee.remainingEnergy == 0L -> {
                resourceReference(
                    R.string.send_summary_transaction_description_suffix_including,
                    wrappedList(fiatFee),
                )
            }
            fee.feeEnergy <= fee.remainingEnergy -> {
                resourceReference(
                    R.string.send_summary_transaction_description_suffix_fee_covered,
                    wrappedList(fee.feeEnergy),
                )
            }
            else -> {
                resourceReference(
                    R.string.send_summary_transaction_description_suffix_fee_reduced,
                    wrappedList(fee.remainingEnergy),
                )
            }
        }
        val prefix = resourceReference(
            R.string.send_summary_transaction_description_prefix,
            wrappedList(fiatSending),
        )

        return combinedReference(prefix, COMMA_SEPARATOR, suffix)
    }

    companion object {
        private val COMMA_SEPARATOR = stringReference(", ")
    }
}