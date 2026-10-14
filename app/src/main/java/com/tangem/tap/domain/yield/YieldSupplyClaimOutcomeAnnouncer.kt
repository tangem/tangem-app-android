package com.tangem.tap.domain.yield

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Announces a reverted claim app-wide: it settles after the user has left the claim sheet. */
@Singleton
class YieldSupplyClaimOutcomeAnnouncer @Inject constructor(
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val appScope: AppCoroutineScope,
) {

    fun start() {
        yieldSupplyClaimTracker.outcomes
            .filter { claimOutcome -> claimOutcome.outcome == YieldSupplyTransactionOutcome.Reverted }
            .onEach { claimOutcome ->
                uiMessageSender.send(
                    DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.yield_module_claim_error_message),
                        firstActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.common_close),
                                onClick = onDismissRequest,
                            )
                        },
                        secondActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.common_support),
                                onClick = { onSupportClick(claimOutcome) },
                            )
                        },
                    ),
                )
            }
            .launchIn(appScope)
    }

    private fun onSupportClick(claimOutcome: YieldSupplyClaimOutcome) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = "Claim transaction ${claimOutcome.transactionHash} reverted",
                networkId = claimOutcome.networkId,
                tokenSymbol = claimOutcome.tokenSymbol,
                destinationAddress = "",
                amount = "",
                fee = "",
            ),
        )

        appScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(claimOutcome.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(metaInfo))
        }
    }
}