package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.swap.models.SwapDirection.Companion.withSwapDirection
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkIfCustomFeeTooHigh
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkIfCustomFeeTooLow
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SendWithSwapConfirmationNotificationsTransformer : Transformer<SendWithSwapUM> {
    override fun transform(prevState: SendWithSwapUM): SendWithSwapUM {
        val confirmUM = prevState.confirmUM as? ConfirmUM.Content ?: return prevState
        val feeSelectorUM = prevState.feeSelectorUM as? FeeSelectorUM.Content ?: return prevState

        return prevState.copy(
            confirmUM = confirmUM.copy(
                sendingFooter = getSendingFooterText(feeSelectorUM, prevState.amountUM),
                notifications = buildList {
                    addTooHighNotification(feeSelectorUM = feeSelectorUM)
                    addTooLowNotification(feeSelectorUM = feeSelectorUM)
                }.toPersistentList(),
            ),
        )
    }

    private fun MutableList<NotificationUM>.addTooLowNotification(feeSelectorUM: FeeSelectorUM.Content) {
        if (checkIfCustomFeeTooLow(feeSelectorUM = feeSelectorUM)) {
            add(NotificationUM.Warning.FeeTooLow)
        }
    }

    private fun MutableList<NotificationUM>.addTooHighNotification(feeSelectorUM: FeeSelectorUM.Content) {
        val (isFeeTooHigh, diff) = checkIfCustomFeeTooHigh(feeSelectorUM = feeSelectorUM)
        if (isFeeTooHigh) {
            add(NotificationUM.Warning.TooHigh(diff))
        }
    }

    private fun getSendingFooterText(feeSelectorUM: FeeSelectorUM.Content, swapAmountUM: SwapAmountUM): TextReference {
        val amountUM = swapAmountUM.swapDirection.withSwapDirection(
            onDirect = { swapAmountUM.primaryAmount.amountField },
            onReverse = { swapAmountUM.secondaryAmount.amountField },
        ) as? AmountState.Data
        val feeItem = feeSelectorUM.selectedFeeItem
        val feeFiatRateUM = feeSelectorUM.feeFiatRateUM

        val appCurrency = feeFiatRateUM?.appCurrency

        if (amountUM == null || appCurrency == null) return TextReference.EMPTY

        val fiatAmountValue = amountUM.amountTextField.fiatAmount.value
        val fiatFeeValue = feeItem.fee.amount.value?.multiply(feeFiatRateUM.rate)

        val fiatSendingValue = if (feeSelectorUM.feeExtraInfo.isFeeConvertibleToFiat) {
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
        val fiatFee = TextReference.EMPTY
        // todo send with swap footer
        // formatFooterFiatFee(
        //     amount = feeItem.fee.amount.copy(value = fiatFeeValue),
        //     isFeeConvertibleToFiat = feeSelectorUM.feeExtraInfo.isFeeConvertibleToFiat,
        //     isFeeApproximate = feeSelectorUM.feeExtraInfo.isFeeApproximate,
        //     appCurrency = appCurrency,
        // )

        return if (feeSelectorUM.feeExtraInfo.isTronToken && feeItem.fee is Fee.Tron) {
            // todo send with swap footer
            // getTronTokenFeeSendingText(
            //     fee = feeItem.fee,
            //     fiatFee = fiatFee,
            //     fiatSending = stringReference(fiatSending),
            // )
            TextReference.EMPTY
        } else {
            resourceReference(
                id = if (feeSelectorUM.feeExtraInfo.isFeeConvertibleToFiat) {
                    R.string.send_summary_transaction_description
                } else {
                    R.string.send_summary_transaction_description_no_fiat_fee
                },
                formatArgs = wrappedList(fiatSending, fiatFee),
            )
        }
    }
}