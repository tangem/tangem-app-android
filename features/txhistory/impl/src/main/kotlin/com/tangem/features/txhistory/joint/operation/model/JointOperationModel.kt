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
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val params: JointOperationComponent.Params = paramsContainer.require()

    val stackNavigation = StackNavigation<JointOperationRoutes>()

    private val observed: SharedFlow<JointOperationState?> = delegate
        .observe(
            userWalletId = params.userWalletId,
            currency = params.currency,
            operationId = params.operationId,
        )
        .flowOn(dispatchers.io)
        .shareIn(modelScope, SharingStarted.WhileSubscribed(), replay = 1)

    val state: StateFlow<JointOperationState?> = observed
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = null)

    /** Told apart from [state] being `null` before the first emission: an operation gone before the sheet opened */
    val isGone: StateFlow<Boolean> = observed
        .map { operation -> operation == null }
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = false)

    val isBalanceHidden: StateFlow<Boolean> = getBalanceHidingSettingsUseCase()
        .map { it.isBalanceHidden }
        .distinctUntilChanged()
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = false)

    val isSigning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /** The sign use case tolerates a failed refresh, so the cached operation may still miss this member's answer */
    val hasAnswered: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun onSignClick() {
        val operation = state.value ?: return
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
        val recipient = state.value?.displayed?.recipient ?: return

        clipboardManager.setText(text = recipient, isSensitive = false)
    }

    private fun showSignError(error: JointOperationSignError) {
        val message = JointOperationErrorMessage().convert(error) ?: return

        TangemLogger.e("Unable to sign the joint operation: $error")
        uiMessageSender.send(message)
    }
}