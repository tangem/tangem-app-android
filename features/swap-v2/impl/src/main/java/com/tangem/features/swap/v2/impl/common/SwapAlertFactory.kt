package com.tangem.features.swap.v2.impl.common

import com.tangem.common.ui.alerts.TransactionErrorAlertConverter
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.swap.v2.impl.R
import javax.inject.Inject

@ModelScoped
internal class SwapAlertFactory @Inject constructor(
    private val uiMessageSender: UiMessageSender,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) {
    fun getGenericErrorState(expressError: ExpressError, onFailedTxEmailClick: () -> Unit, popBack: () -> Unit = {}) {
        uiMessageSender.send(
            DialogMessage.Companion(
                title = SwapUtils.getExpressErrorTitle(expressError),
                message = SwapUtils.getExpressErrorMessage(expressError),
                onDismissRequest = popBack,
                dismissOnFirstAction = false,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_support),
                        onClick = onFailedTxEmailClick,
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    fun getSendTransactionErrorState(
        error: SendTransactionError?,
        popBack: () -> Unit,
        onFailedTxEmailClick: (String) -> Unit,
    ) {
        val transactionErrorAlertConverter = TransactionErrorAlertConverter(
            popBackStack = popBack,
            onFailedTxEmailClick = onFailedTxEmailClick,
        )

        val errorAlert = error?.let { transactionErrorAlertConverter.convert(error) } ?: return
        val onConfirmClick = errorAlert.onConfirmClick ?: return

        uiMessageSender.send(
            DialogMessage.Companion(
                title = errorAlert.title,
                message = errorAlert.message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = errorAlert.confirmButtonText,
                        onClick = onConfirmClick,
                    )
                },
                secondActionBuilder = if (errorAlert !is AlertDemoModeUM) {
                    { cancelAction() }
                } else {
                    null
                },
            ),
        )
    }

    suspend fun onFailedTxEmailClick(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency?,
        errorMessage: String?,
        txId: String? = null,
        confirmData: ConfirmData? = null,
    ) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage.orEmpty(),
                blockchainId = cryptoCurrency?.network?.rawId.orEmpty(),
                derivationPath = cryptoCurrency?.network?.derivationPath?.value.orEmpty(),
                destinationAddress = confirmData?.enteredDestination.orEmpty(),
                tokenSymbol = confirmData?.toCryptoCurrencyStatus?.currency?.symbol.orEmpty(),
                amount = confirmData?.enteredAmount?.toString().orEmpty(),
                fee = confirmData?.fee?.amount?.value?.toString().orEmpty(),
            ),
        )

        val cardInfo =
            getCardInfoUseCase(userWallet.requireColdWallet().scanResponse).getOrNull() ?: return // TODO [REDACTED_TASK_KEY]

        sendFeedbackEmailUseCase(
            type = FeedbackEmailType.SwapProblem(
                cardInfo = cardInfo,
                providerName = confirmData?.quote?.provider?.name.orEmpty(),
                txId = txId.orEmpty(),
            ),
        )
    }
}