package com.tangem.features.jointaccount.join.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountJoinError
import com.tangem.domain.jointaccount.usecase.JoinJointAccountUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
internal class JointAccountJoinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val joinJointAccountUseCase: JoinJointAccountUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    val draftHolder = JointAccountJoinDraftHolder()

    suspend fun join(inviteId: String, memberName: String): JoinResultAction {
        draftHolder.setDisplayName(displayName = memberName)

        val draft = draftHolder.draft.value
        val walletId = draft.selectedWalletId
        val config = draft.config

        if (walletId == null || config == null) {
            TangemLogger.e("Joint account join failed: the draft has no wallet or config from the preview step")
            showGenericError()
            return JoinResultAction.None
        }

        // TODO([REDACTED_TASK_KEY]): gate joining on the 20 active joint accounts limit before signing — the backend
        //  rejects the 21st with a 400, but the user must hear it from us before the tap, not after
        return joinJointAccountUseCase(
            userWalletId = walletId,
            inviteId = inviteId,
            config = config,
            memberName = memberName,
        ).fold(
            ifLeft = ::handleErrorAction,
            ifRight = { account ->
                JoinResultAction.OpenMembers(userWalletId = walletId, cryptoAccountId = account.cryptoAccountId)
            },
        )
    }

    private fun handleErrorAction(error: JointAccountJoinError): JoinResultAction = when (error) {
        JointAccountFlowError.UserCancelled -> JoinResultAction.None
        JointAccountJoinError.InvalidInvite -> {
            showInvalidInvite()
            JoinResultAction.CloseFlow
        }
        is JointAccountFlowError.InvalidDerivationIndex -> {
            TangemLogger.e("Joint account join failed: $error")
            showGenericError()
            JoinResultAction.None
        }
        is JointAccountFlowError.Failed -> {
            TangemLogger.e("Joint account join failed", error.cause)
            showGenericError()
            JoinResultAction.None
        }
    }

    private fun showInvalidInvite() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.joint_account_join_invalid_link_title),
                message = resourceReference(CoreUiR.string.joint_account_join_invalid_link_subtitle),
            ),
        )
    }

    private fun showGenericError() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.common_error),
                message = resourceReference(CoreUiR.string.common_something_went_wrong),
            ),
        )
    }
}