package com.tangem.features.jointaccount.compromised

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.tangem.core.ui.R as CoreUiR

internal class DefaultJointAccountCompromisedAlert @Inject constructor(
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val appScope: AppCoroutineScope,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) : JointAccountCompromisedAlert {

    override fun show(userWalletId: UserWalletId) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.common_something_went_wrong),
                message = resourceReference(CoreUiR.string.common_try_again_later),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreUiR.string.common_contact_support),
                        onClick = { openSupport(userWalletId) },
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreUiR.string.common_cancel),
                        onClick = onDismissRequest,
                    )
                },
            ),
        )
    }

    /**
     * Waits for the account's verified status rather than taking the first snapshot: on a cold start the joint
     * status is still loading, and a loading account is not a trusted one. No verdict in time blocks it too.
     */
    override suspend fun showIfCompromised(userWalletId: UserWalletId, accountId: String): Boolean {
        val isReportedCompromised = withTimeoutOrNull(STATUS_TIMEOUT) {
            singleAccountStatusListSupplier(userWalletId)
                .mapNotNull { list -> list.accountStatuses.firstOrNull { it.accountId.value == accountId } }
                .mapNotNull { status -> status.compromisedOrNull() }
                .first()
        }
        val isCompromised = isReportedCompromised != false

        if (isCompromised) show(userWalletId = userWalletId)
        return isCompromised
    }

    /** `null` while the joint status is still loading — the answer is not known yet */
    private fun AccountStatus.compromisedOrNull(): Boolean? {
        if (this !is AccountStatus.Joint) return false

        return when (value) {
            is JointAccountStatusValue.Loading -> null
            is JointAccountStatusValue.Error.Compromised -> true
            else -> false
        }
    }

    private fun openSupport(userWalletId: UserWalletId) {
        appScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWalletId).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    private companion object {
        val STATUS_TIMEOUT = 10.seconds
    }
}