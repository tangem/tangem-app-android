package com.tangem.features.send.send.confirm.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import com.tangem.features.send.api.utils.formatFooterFiatFee
import com.tangem.features.send.api.utils.getTronTokenFeeSendingText
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.common.ui.state.SendSignaturesUM
import com.tangem.features.send.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder

@Suppress("LongParameterList")
internal class SendConfirmationNotificationsTransformerV2(
    private val feeSelectorUM: FeeSelectorUM,
    private val amountUM: AmountState,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val cryptoCurrency: CryptoCurrency,
    private val appCurrency: AppCurrency,
    private val analyticsCategoryName: String,
    private val isHighNetworkFee: Boolean = false,
    private val isFeeSubtractedFromAmount: Boolean,
    private val isFeeExceedingBalance: Boolean,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        val state = prevState as? ConfirmUM.Content ?: return prevState
        val feeSelectorUM = feeSelectorUM as? FeeSelectorUM.Content ?: return prevState
        return state.copy(
            sendingFooter = getSendingFooterText(signaturesUM = state.signaturesUM),
            setupFeeNote = getSetupFeeNote(signaturesUM = state.signaturesUM, feeSelectorUM = feeSelectorUM),
            notifications = buildList {
                addTooHighNotification(feeSelectorUM)
                addTooLowNotification(feeSelectorUM)
                addHighNetworkFeeNotification()
            }.toPersistentList(),
        )
    }

    private fun MutableList<NotificationUM>.addHighNetworkFeeNotification() {
        if (isHighNetworkFee) {
            add(NotificationUM.Warning.HighNetworkFee)
        }
    }

    private fun MutableList<NotificationUM>.addTooLowNotification(feeSelectorUM: FeeSelectorUM.Content) {
        if (FeeCalculationUtils.checkIfCustomFeeTooLow(feeSelectorUM)) {
            add(NotificationUM.Warning.FeeTooLow)
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.NoticeTransactionDelays(
                    categoryName = analyticsCategoryName,
                    token = cryptoCurrency.symbol,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addTooHighNotification(feeSelectorUM: FeeSelectorUM.Content) {
        val (isFeeTooHigh, diff) = FeeCalculationUtils.checkIfCustomFeeTooHigh(feeSelectorUM)
        if (isFeeTooHigh) {
            add(NotificationUM.Warning.TooHigh(diff))
        }
    }

    /** The first operation in a network also deploys the Safe: its share is the deployment gas at the fee's gas price */
    private fun getSetupFeeNote(signaturesUM: SendSignaturesUM?, feeSelectorUM: FeeSelectorUM.Content): TextReference? {
        if (signaturesUM?.isFirstOperation != true) return null
        val fee = feeSelectorUM.selectedFeeItem.fee as? Fee.Ethereum.Legacy ?: return null
        val amount = fee.amount
        val setupFee = (SafeTxGasBuilder.DEPLOY_GAS * fee.gasPrice).toBigDecimal().movePointLeft(amount.decimals)

        val crypto = setupFee.format { crypto(symbol = amount.currencySymbol, decimals = amount.decimals) }
        val fiat = feeSelectorUM.feeFiatRateUM
            ?.takeIf { feeSelectorUM.feeExtraInfo.isFeeConvertibleToFiat }
            ?.let { rate -> getFiatString(value = setupFee, rate = rate.rate, appCurrency = rate.appCurrency) }
        val setupFeeText = if (fiat != null) "$fiat ($crypto)" else crypto

        return resourceReference(
            id = R.string.joint_account_send_setup_fee_note,
            formatArgs = wrappedList(cryptoCurrency.network.name, setupFeeText),
        )
    }

    private fun getSendingFooterText(signaturesUM: SendSignaturesUM?): TextReference {
        val feeSelectorUM = feeSelectorUM as? FeeSelectorUM.Content
        val amountUM = amountUM as? AmountState.Data
        val fee = feeSelectorUM?.selectedFeeItem?.fee

        if (fee == null || amountUM == null) return TextReference.EMPTY
        // The Safe never takes the fee out of the amount: a total over the balance is the "exceeds balance" error
        // rather than a smaller transfer, and there is no request to describe
        if (signaturesUM != null && isFeeSubtractedFromAmount) return TextReference.EMPTY

        val fiatAmountValue = amountUM.amountTextField.fiatAmount.value
        val fiatFeeValue = feeSelectorUM.feeFiatRateUM?.rate?.let { fee.amount.value?.multiply(it) }

        val isFeeConvertibleToFiat = feeSelectorUM.feeExtraInfo.isFeeConvertibleToFiat

        val fiatSendingValue = when {
            !isFeeConvertibleToFiat -> fiatAmountValue
            // Fee alone exceeds the balance → the transaction can't go through, nothing is sent.
            isFeeExceedingBalance -> BigDecimal.ZERO
            // When the fee is subtracted from the amount, the entered amount is the gross that already
            // includes the fee, so it must not be added again — that double-counts it.
            isFeeSubtractedFromAmount -> fiatAmountValue
            else -> fiatFeeValue?.let { fiatAmountValue?.plus(it) }
        }

        val fiatSending = fiatSendingValue.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
        val fiatFee = formatFooterFiatFee(
            amount = fee.amount.copy(value = fiatFeeValue),
            isFeeConvertibleToFiat = isFeeConvertibleToFiat,
            isFeeApproximate = feeSelectorUM.feeExtraInfo.isFeeApproximate,
            appCurrency = appCurrency,
        )

        return if (signaturesUM != null) {
            resourceReference(
                id = R.string.joint_account_send_footer_text,
                formatArgs = wrappedList(
                    fiatSending,
                    fiatFee,
                    signaturesUM.threshold.toString(),
                    signaturesUM.membersCount.toString(),
                ),
            )
        } else if (fee is Fee.Tron) {
            getTronTokenFeeSendingText(
                fee = fee,
                fiatFee = fiatFee,
                fiatSending = stringReference(fiatSending),
            )
        } else {
            resourceReference(
                id = if (isFeeConvertibleToFiat) {
                    R.string.send_summary_transaction_description
                } else {
                    R.string.send_summary_transaction_description_no_fiat_fee
                },
                formatArgs = wrappedList(fiatSending, fiatFee),
            )
        }
    }
}