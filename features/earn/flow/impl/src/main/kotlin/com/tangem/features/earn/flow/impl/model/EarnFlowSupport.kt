package com.tangem.features.earn.flow.impl.model

import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The transaction error dialog; its Support button sends the same email Yield Supply sends for a failed transaction. */
internal class EarnFlowSupport @Inject constructor(
    private val transactionErrorDialogFactory: TransactionErrorDialogFactory,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) {

    fun transactionErrorDialog(
        error: SendTransactionError,
        product: EarnProduct.Params,
        state: () -> EarnFlowState,
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): DialogMessage? = transactionErrorDialogFactory.create(
        error = error,
        popBackStack = onClose,
        onFailedTxEmailClick = { message -> scope.launch { sendFailedTxEmail(product, message, state()) } },
    )

    suspend fun sendFailedTxEmail(product: EarnProduct.Params, errorMessage: String, state: EarnFlowState) {
        val currency = product.currency
        val amount = state.sendingAmount
        val fee = state.feeSelection?.fee?.amount?.value
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage,
                networkId = currency.network.id,
                destinationAddress = "",
                tokenSymbol = (currency as? CryptoCurrency.Token)?.symbol,
                amount = amount?.let { "${it.toPlainString()} ${currency.symbol}" }.orEmpty(),
                fee = fee?.let { "${it.toPlainString()} ${currency.network.currencySymbol}" },
            ),
        )
        val metaInfo = getWalletMetaInfoUseCase(product.userWalletId).getOrNull()
        if (metaInfo == null) {
            TangemLogger.e("Earn flow: no wallet meta info for the support email")
            return
        }
        sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(metaInfo))
    }
}