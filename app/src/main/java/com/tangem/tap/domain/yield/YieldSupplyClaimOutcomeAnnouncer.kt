package com.tangem.tap.domain.yield

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports a Merkl rewards claim that reverted on chain, wherever the user happens to be by then.
 *
 * A claim settles minutes after it is signed, long after the sheet that sent it is gone, so a listener tied to that
 * sheet would drop the one outcome the user cannot work out alone. Only a claim started from the claim sheet is
 * announced: one signed on the way out of Yield Mode was never put to the user as a transaction of its own.
 */
@Singleton
class YieldSupplyClaimOutcomeAnnouncer @Inject constructor(
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val appScope: AppCoroutineScope,
) {

    fun start() {
        yieldSupplyClaimTracker.outcomes
            .filter { claimOutcome ->
                claimOutcome.outcome == YieldSupplyTransactionOutcome.Reverted &&
                    claimOutcome.origin == YieldSupplyClaimOrigin.ClaimSheet
            }
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
                                onClick = { onSupportClick(claimOutcome.userWalletId) },
                            )
                        },
                    ),
                )
            }
            .launchIn(appScope)
    }

    /** A revert carries no error text of its own, so the plain transaction-sending report is the whole email. */
    private fun onSupportClick(userWalletId: UserWalletId) {
        appScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(metaInfo))
        }
    }
}