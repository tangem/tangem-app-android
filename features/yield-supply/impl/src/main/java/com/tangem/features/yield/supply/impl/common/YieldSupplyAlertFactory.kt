package com.tangem.features.yield.supply.impl.common

import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.yield.supply.impl.R
import javax.inject.Inject

@ModelScoped
class YieldSupplyAlertFactory @Inject constructor(
    private val uiMessageSender: UiMessageSender,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val transactionErrorDialogFactory: TransactionErrorDialogFactory,
) {

    fun getGenericErrorState(onFailedTxEmailClick: () -> Unit, popBack: () -> Unit = {}) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.send_alert_transaction_failed_title),
                message = resourceReference(id = R.string.common_unknown_error),
                onDismissRequest = popBack,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_support),
                        onClick = onFailedTxEmailClick,
                    )
                },
                secondActionBuilder = { cancelAction { } },
            ),
        )
    }

    fun getSendTransactionErrorState(
        error: SendTransactionError,
        popBack: () -> Unit,
        onFailedTxEmailClick: (String) -> Unit,
    ) {
        val errorDialog = transactionErrorDialogFactory.create(
            error = error,
            popBackStack = popBack,
            onFailedTxEmailClick = onFailedTxEmailClick,
        ) ?: return

        uiMessageSender.send(errorDialog)
    }

    suspend fun onFailedTxEmailClick(userWallet: UserWallet, cryptoCurrency: CryptoCurrency?, errorMessage: String?) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage.orEmpty(),
                blockchainId = cryptoCurrency?.network?.rawId.orEmpty(),
                derivationPath = cryptoCurrency?.network?.derivationPath?.value.orEmpty(),
                tokenSymbol = cryptoCurrency?.symbol.orEmpty(),
                destinationAddress = "",
                amount = "",
                fee = "",
            ),
        )

        val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return

        sendFeedbackEmailUseCase(
            type = FeedbackEmailType.TransactionSendingProblem(metaInfo),
        )
    }
}