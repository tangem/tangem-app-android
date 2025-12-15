package com.tangem.features.send.v2.send.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.entity.FeeNonce
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.send.ui.state.SendUM
import javax.inject.Inject

@ModelScoped
internal class SendAnalyticHelper @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun sendSuccessAnalytics(cryptoCurrency: CryptoCurrency, sendUM: SendUM, account: Account.CryptoPortfolio?) {
        val destinationUM = sendUM.destinationUM as? DestinationUM.Content
        val feeSelectorUM = sendUM.feeSelectorUM as? FeeSelectorUM.Content ?: return
        val feeType = feeSelectorUM.toAnalyticType()
        val isNotMainAccount = account != null && !account.isMainAccount
        val derivationIndex = if (isNotMainAccount) account.derivationIndex.value else null
        analyticsEventHandler.send(
            SendAnalyticEvents.TransactionScreenOpened(
                token = cryptoCurrency.symbol,
                feeType = feeType,
                blockchain = cryptoCurrency.network.name,
                isNonceNotEmpty = feeSelectorUM.feeNonce is FeeNonce.Nonce,
                ensStatus = getEnsStatus(sendUM),
                derivationIndex = derivationIndex,
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

    private fun getEnsStatus(sendUM: SendUM): AnalyticsParam.EmptyFull {
        val isBlockchainAddressForEns =
            (sendUM.destinationUM as? DestinationUM.Content)?.addressTextField?.isAddressEns
        return if (isBlockchainAddressForEns == true) {
            AnalyticsParam.EmptyFull.Full
        } else {
            AnalyticsParam.EmptyFull.Empty
        }
    }
}