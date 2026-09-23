package com.tangem.features.txhistory.joint.operation.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.converter.JointOperationUMConverter
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.features.txhistory.joint.operation.signers.converter.JointOperationSignersUMConverter
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import com.tangem.features.txhistory.joint.operation.model.JointOperationState.Role
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@ModelScoped
internal class JointOperationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val delegate: JointOperationDelegate,
    private val uiMessageSender: UiMessageSender,
    private val clipboardManager: ClipboardManager,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    private val params: JointOperationComponent.Params = paramsContainer.require()

    private val isColdWallet = delegate.isColdWallet(userWalletId = params.userWalletId)

    private val operationState: SharedFlow<JointOperationState?> = delegate
        .observe(
            userWalletId = params.userWalletId,
            currency = params.currency,
            operationId = params.operationId,
        )
        .flowOn(dispatchers.io)
        .shareIn(modelScope, SharingStarted.WhileSubscribed(), replay = 1)

    /** What the sheet shows right now; the actions act on it, not on a cache of their own */
    private val currentState: JointOperationState?
        get() = operationState.replayCache.firstOrNull()

    private val isBalanceHidden = getBalanceHidingSettingsUseCase()
        .map { it.isBalanceHidden }
        .distinctUntilChanged()

    private val isSigning = MutableStateFlow(false)
    private val isRefusing = MutableStateFlow(false)

    /** The use cases tolerate a failed refresh, so the cached operation may still miss this member's answer */
    private val localAnswer = MutableStateFlow<Answer?>(null)

    /** The backend no longer has the operation (deleted here, or gone before the refusal); the cache may still */
    private val isGoneLocally = MutableStateFlow(false)

    private val userActions = combine(
        flow = isSigning,
        flow2 = isRefusing,
        flow3 = localAnswer,
        flow4 = isGoneLocally,
        transform = ::UserActions,
    )

    private val isBusy: Boolean
        get() = isSigning.value || isRefusing.value

    private val canCreateContact = operationState.flatMapLatest { state ->
        if (state == null) return@flatMapLatest flowOf(false)

        delegate.canCreateContact(
            userWalletId = params.userWalletId,
            recipient = state.displayed.recipient,
            network = params.currency.network,
        )
    }

    private val operationConverter = JointOperationUMConverter(
        currency = params.currency,
        onSignClick = ::onSignClick,
        onSignersClick = ::onSignersClick,
        onCopyRecipientClick = ::onCopyRecipientClick,
        onCreateContactClick = ::onCreateContactClick,
        onRefuseClick = ::onRefuseClick,
    )

    private val refusalMessages = JointOperationRefusalMessages(
        onConfirm = ::refuse,
        onOperationGone = params.onDismiss,
    )

    private val signersConverter = JointOperationSignersUMConverter()

    val stackNavigation = StackNavigation<JointOperationRoutes>()

    /**
     * [JointOperationUM.Gone] is told apart from nothing having arrived yet: an operation gone before the sheet
     * opened, or one whose threshold the remaining members can no longer reach — the backend drops it, the sheet
     * does not wait for that
     */
    val uiState: StateFlow<JointOperationUM> = combine(
        flow = operationState,
        flow2 = userActions,
        flow3 = canCreateContact,
        flow4 = isBalanceHidden,
    ) { state, actions, canCreateContact, isBalanceHidden ->
        val answered = state?.withOwnAnswer(answeredLocally = actions.localAnswer)
        val isGone = answered == null || actions.isGone || !answered.isReachable && !answered.isSending
        if (isGone) return@combine JointOperationUM.Gone

        JointOperationUM.Content(
            operation = operationConverter.convert(
                value = JointOperationUMConverter.Input(
                    state = answered,
                    isSigning = actions.isSigning,
                    isRefusing = actions.isRefusing,
                    hasAnswered = actions.localAnswer != null,
                    canCreateContact = canCreateContact,
                    isBalanceHidden = isBalanceHidden,
                    isColdWallet = isColdWallet,
                ),
            ),
            signers = signersConverter.convert(
                value = JointOperationSignersUMConverter.Input(
                    membersCount = answered.membersCount,
                    signers = answered.signers,
                ),
            ),
        )
    }
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = JointOperationUM.Loading)

    fun onSignClick() {
        val operation = currentState ?: return
        if (!operation.canSign || isBusy) return

        modelScope.launch {
            isSigning.value = true
            try {
                delegate.sign(
                    state = operation,
                    network = params.currency.network,
                    operationId = params.operationId,
                )
                    .onRight { localAnswer.value = Answer.SIGNED }
                    .onLeft(::showSignError)
            } finally {
                isSigning.value = false
            }
        }
    }

    fun onRefuseClick() {
        val operation = currentState ?: return
        if (isBusy || !operation.canRefuse) return

        uiMessageSender.send(refusalMessages.confirmation(isInitiator = operation.role == Role.INITIATOR))
    }

    fun onCreateContactClick() {
        val recipient = currentState?.displayed?.recipient ?: return

        delegate.openContactCreation(address = recipient, network = params.currency.network)
    }

    fun onSignersClick() {
        stackNavigation.pushNew(JointOperationRoutes.Signers)
    }

    fun onBackClick() {
        stackNavigation.pop()
    }

    fun onCopyRecipientClick() {
        val recipient = currentState?.displayed?.recipient ?: return

        clipboardManager.setText(text = recipient, isSensitive = false)
    }

    /** The use case tolerates a failed refresh, so a deletion (or an operation already gone) is recorded locally too */
    private fun refuse() {
        val operation = currentState ?: return
        if (isBusy) return

        modelScope.launch {
            isRefusing.value = true
            try {
                delegate.reject(
                    state = operation,
                    network = params.currency.network,
                    operationId = params.operationId,
                )
                    .onRight { onRefused(operation) }
                    .onLeft { error ->
                        if (error is JointOperationRejectError.OperationNotFound) isGoneLocally.value = true
                        showRejectError(error)
                    }
            } finally {
                isRefusing.value = false
            }
        }
    }

    private fun onRefused(operation: JointOperationState) {
        when (operation.role) {
            Role.INITIATOR -> {
                isGoneLocally.value = true
                showDeleted()
            }
            Role.MEMBER -> localAnswer.value = Answer.DECLINED
            Role.UNKNOWN -> Unit
        }
    }

    private fun showDeleted() {
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.joint_operation_deleted_snackbar),
                startIconId = CoreUiR.drawable.ic_trash_24,
            ),
        )
    }

    private fun showSignError(error: JointOperationSignError) {
        val message = JointOperationErrorMessage(onSupportClick = ::onSupportClick, onOperationGone = params.onDismiss)
            .convert(error)
            ?: return

        TangemLogger.e("Unable to sign the joint operation: $error")
        uiMessageSender.send(message)
    }

    private fun onSupportClick() {
        modelScope.launch { delegate.openSupport(userWalletId = params.userWalletId) }
    }

    private fun showRejectError(error: JointOperationRejectError) {
        val message = refusalMessages.error(error) ?: return

        TangemLogger.e("Unable to reject the joint operation: $error")
        uiMessageSender.send(message)
    }

    /** What this member is doing or has done on the sheet, on top of what the backend reports */
    private data class UserActions(
        val isSigning: Boolean,
        val isRefusing: Boolean,
        val localAnswer: Answer?,
        val isGone: Boolean,
    )
}