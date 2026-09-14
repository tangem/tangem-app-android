package com.tangem.features.txhistory.joint.operation.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.converter.JointOperationUMConverter
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.features.txhistory.joint.operation.signers.converter.JointOperationSignersUMConverter
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val observed: SharedFlow<JointOperationState?> = delegate
        .observe(
            userWalletId = params.userWalletId,
            currency = params.currency,
            operationId = params.operationId,
        )
        .flowOn(dispatchers.io)
        .shareIn(modelScope, SharingStarted.WhileSubscribed(), replay = 1)

    /** What the sheet shows right now; the actions act on it, not on a cache of their own */
    private val shown: JointOperationState?
        get() = observed.replayCache.firstOrNull()

    private val isBalanceHidden = getBalanceHidingSettingsUseCase()
        .map { it.isBalanceHidden }
        .distinctUntilChanged()

    private val isSigning = MutableStateFlow(false)

    /** The sign use case tolerates a failed refresh, so the cached operation may still miss this member's answer */
    private val hasAnswered = MutableStateFlow(false)

    private val operationConverter = JointOperationUMConverter(
        currency = params.currency,
        onSignClick = ::onSignClick,
        onSignersClick = ::onSignersClick,
        onCopyRecipientClick = ::onCopyRecipientClick,
    )

    private val signersConverter = JointOperationSignersUMConverter()

    val stackNavigation = StackNavigation<JointOperationRoutes>()

    /** [JointOperationUM.Gone] is told apart from nothing having arrived yet: an operation gone before the sheet opened */
    val uiState: StateFlow<JointOperationUM> = combine(
        flow = observed,
        flow2 = isSigning,
        flow3 = hasAnswered,
        flow4 = isBalanceHidden,
    ) { state, isSigning, hasAnswered, isBalanceHidden ->
        state ?: return@combine JointOperationUM.Gone

        JointOperationUM.Content(
            operation = operationConverter.convert(
                value = JointOperationUMConverter.Input(
                    state = state,
                    isSigning = isSigning,
                    hasAnswered = hasAnswered,
                    isBalanceHidden = isBalanceHidden,
                ),
            ),
            signers = signersConverter.convert(
                value = JointOperationSignersUMConverter.Input(
                    membersCount = state.membersCount,
                    signers = state.signers,
                ),
            ),
        )
    }
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = JointOperationUM.Loading)

    fun onSignClick() {
        val operation = shown ?: return
        if (!operation.canSign || isSigning.value) return

        modelScope.launch {
            isSigning.value = true
            try {
                delegate.sign(
                    state = operation,
                    network = params.currency.network,
                    operationId = params.operationId,
                )
                    .onRight { hasAnswered.value = true }
                    .onLeft(::showSignError)
            } finally {
                isSigning.value = false
            }
        }
    }

    fun onSignersClick() {
        stackNavigation.pushNew(JointOperationRoutes.Signers)
    }

    fun onBackClick() {
        stackNavigation.pop()
    }

    fun onCopyRecipientClick() {
        val recipient = shown?.displayed?.recipient ?: return

        clipboardManager.setText(text = recipient, isSensitive = false)
    }

    private fun showSignError(error: JointOperationSignError) {
        val message = JointOperationErrorMessage().convert(error) ?: return

        TangemLogger.e("Unable to sign the joint operation: $error")
        uiMessageSender.send(message)
    }
}