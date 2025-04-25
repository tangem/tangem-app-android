package com.tangem.features.send.v2.sendnft.confirm.model.transformers

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.send.v2.common.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.fee.model.checkIfFeeTooHigh
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class NFTSendConfirmationNotificationsTransformer(
    private val feeUM: FeeUM,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val cryptoCurrency: CryptoCurrency,
    private val analyticsCategoryName: String,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        val state = prevState as? ConfirmUM.Content ?: return prevState
        val feeUM = feeUM as? FeeUM.Content ?: return prevState
        return state.copy(
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
                CommonSendAnalyticEvents.NoticeTransactionDelays(
                    categoryName = analyticsCategoryName,
                    token = cryptoCurrency.symbol,
                ),
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
}