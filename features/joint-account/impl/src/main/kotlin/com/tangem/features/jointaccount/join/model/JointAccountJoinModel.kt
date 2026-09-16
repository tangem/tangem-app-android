package com.tangem.features.jointaccount.join.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountJoinError
import com.tangem.domain.jointaccount.usecase.JoinJointAccountUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
internal class JointAccountJoinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val joinJointAccountUseCase: JoinJointAccountUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
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

        if (isJointLimitReached(walletId = walletId)) {
            showJointLimitReached()
            return JoinResultAction.None
        }

        return joinJointAccountUseCase(
            userWalletId = walletId,
            inviteId = inviteId,
            config = config,
            memberName = memberName,
        ).fold(
            ifLeft = ::handleErrorAction,
            ifRight = {
                showAccountAdded()
                JoinResultAction.CloseFlow
            },
        )
    }

    private suspend fun isJointLimitReached(walletId: UserWalletId): Boolean {
        val accountList = singleAccountListSupplier.getSyncOrNull(userWalletId = walletId) ?: return false
        return !accountList.canAddMoreJointAccounts
    }

    private fun showJointLimitReached() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.account_add_limit_dialog_title),
                message = resourceReference(
                    id = CoreUiR.string.account_add_limit_dialog_description,
                    formatArgs = wrappedList(AccountList.MAX_JOINT_ACCOUNTS_COUNT.toString()),
                ),
                firstActionBuilder = { okAction() },
            ),
        )
    }

    /** The join confirmation, shown over whatever screen the closed flow returns the user to */
    private fun showAccountAdded() {
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.joint_account_join_success_message),
                startIconId = CoreUiR.drawable.ic_success_20,
                isIconTinted = false,
            ),
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