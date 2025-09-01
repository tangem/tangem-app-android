package com.tangem.features.send.v2.send.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import javax.inject.Inject

@ModelScoped
internal class SendAnalyticHelper @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun sendSuccessAnalytics(cryptoCurrency: CryptoCurrency, sendUM: SendUM) {
        val destinationUM = sendUM.destinationUM as? DestinationUM.Content
        val feeUM = sendUM.feeUM as? FeeUM.Content
        val feeSelectorUM = feeUM?.feeSelectorUM as? FeeSelectorUM.Content ?: return
        val feeType = feeSelectorUM.selectedType.toAnalyticType(feeSelectorUM)
        analyticsEventHandler.send(
            SendAnalyticEvents.TransactionScreenOpened(
                token = cryptoCurrency.symbol,
                feeType = feeType,
                blockchain = cryptoCurrency.network.name,
                nonceNotEmpty = feeSelectorUM.nonce != null,
            ),
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Send(
                    blockchain = cryptoCurrency.network.name,
                    token = cryptoCurrency.symbol,
                    feeType = feeType,
                ),
                memoType = getSendTransactionMemoType(destinationUM?.memoTextField),
            ),
        )
    }

    private fun getSendTransactionMemoType(
        recipientMemo: DestinationTextFieldUM.RecipientMemo?,
    ): Basic.TransactionSent.MemoType {
        val memo = recipientMemo?.value
        return when {
            memo?.isBlank() == true -> Basic.TransactionSent.MemoType.Empty
            memo?.isNotBlank() == true -> Basic.TransactionSent.MemoType.Full
            else -> Basic.TransactionSent.MemoType.Null
        }
    }
}