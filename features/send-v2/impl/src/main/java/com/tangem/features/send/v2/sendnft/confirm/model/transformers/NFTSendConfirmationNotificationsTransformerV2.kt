package com.tangem.features.send.v2.sendnft.confirm.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import com.tangem.features.send.v2.api.utils.formatFooterFiatFee
import com.tangem.features.send.v2.api.utils.getTronTokenFeeSendingText
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class NFTSendConfirmationNotificationsTransformerV2(
    private val feeSelectorUM: FeeSelectorUM,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val cryptoCurrency: CryptoCurrency,
    private val appCurrency: AppCurrency,
    private val analyticsCategoryName: String,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        val state = prevState as? ConfirmUM.Content ?: return prevState
        val feeSelectorUM = feeSelectorUM as? FeeSelectorUM.Content ?: return prevState
        return state.copy(
            sendingFooter = getSendingFooterText(),
            notifications = buildList {
                addTooHighNotification(feeSelectorUM)
                addTooLowNotification(feeSelectorUM)
            }.toPersistentList(),
        )
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

    private fun getSendingFooterText(): TextReference {
        val feeSelectorUM = feeSelectorUM as? FeeSelectorUM.Content
        val fee = feeSelectorUM?.selectedFeeItem?.fee ?: return TextReference.EMPTY

        val fiatFeeValue = feeSelectorUM.feeFiatRateUM?.rate?.let { fee.amount.value?.multiply(it) }
        val isFeeConvertibleToFiat = feeSelectorUM.feeExtraInfo.isFeeConvertibleToFiat

        val fiatFee = formatFooterFiatFee(
            amount = fee.amount.copy(value = fiatFeeValue),
            isFeeConvertibleToFiat = isFeeConvertibleToFiat,
            isFeeApproximate = feeSelectorUM.feeExtraInfo.isFeeApproximate,
            appCurrency = appCurrency,
        )

        return if (fee is Fee.Tron) {
            getTronTokenFeeSendingText(
                fee = fee,
                fiatFee = fiatFee,
                fiatSending = resourceReference(R.string.common_nft),
            )
        } else {
            resourceReference(
                id = if (isFeeConvertibleToFiat) {
                    R.string.send_summary_transaction_description
                } else {
                    R.string.send_summary_transaction_description_no_fiat_fee
                },
                formatArgs = wrappedList(
                    resourceReference(R.string.common_nft),
                    fiatFee,
                ),
            )
        }
    }
}