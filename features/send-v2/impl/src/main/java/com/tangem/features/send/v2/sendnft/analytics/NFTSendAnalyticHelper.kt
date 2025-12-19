package com.tangem.features.send.v2.sendnft.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.Companion.NFT_SEND_CATEGORY
import com.tangem.features.send.v2.api.entity.FeeNonce
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
import javax.inject.Inject

@ModelScoped
internal class NFTSendAnalyticHelper @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun nftSendSuccessAnalytics(cryptoCurrency: CryptoCurrency, nftSendUM: NFTSendUM) {
        val destinationUM = nftSendUM.destinationUM as? DestinationUM.Content
        val feeSelectorUM = nftSendUM.feeSelectorUM as? FeeSelectorUM.Content ?: return
        val feeType = feeSelectorUM.toAnalyticType()
        analyticsEventHandler.send(
            NFTSendAnalyticEvents.TransactionScreenOpened(
                token = cryptoCurrency.symbol,
                feeType = feeType,
                blockchain = cryptoCurrency.network.name,
                isNonceNotEmpty = feeSelectorUM.feeNonce is FeeNonce.Nonce,
            ),
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.NFT(
                    blockchain = cryptoCurrency.network.name,
                    token = NFT_SEND_CATEGORY, // should send "NFT" in token param
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