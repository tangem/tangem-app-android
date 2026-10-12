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
 * A claim is signed on the Yield Mode screen but settles minutes later, and the result sheet's own "Go to token"
 * button leaves that screen — so a listener tied to it would drop exactly the outcome the user cannot work out on
 * their own: the rewards are still there, the fee was spent, and nothing says why. A confirmed claim needs no such
 * announcement, since the rewards banner and the transaction history already show it.
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