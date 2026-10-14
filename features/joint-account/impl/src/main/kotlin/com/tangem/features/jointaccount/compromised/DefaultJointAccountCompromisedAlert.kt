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
import kotlinx.coroutines.launch
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

    override suspend fun showIfCompromised(userWalletId: UserWalletId, accountId: String): Boolean {
        val status = singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            ?.accountStatuses
            ?.firstOrNull { it.accountId.value == accountId } as? AccountStatus.Joint
        val isCompromised = status?.value is JointAccountStatusValue.Error.Compromised

        if (isCompromised) show(userWalletId = userWalletId)
        return isCompromised
    }

    private fun openSupport(userWalletId: UserWalletId) {
        appScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWalletId).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }
}