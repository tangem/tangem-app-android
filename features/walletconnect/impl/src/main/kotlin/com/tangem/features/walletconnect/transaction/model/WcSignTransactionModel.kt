package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import com.tangem.features.walletconnect.transaction.components.WcSignTransactionComponent
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.utils.toUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcSignTransactionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val useCaseFactory: WcRequestUseCaseFactory,
) : Model() {

    private val _uiState = MutableStateFlow<WcSignTransactionUM?>(null)
    val uiState: StateFlow<WcSignTransactionUM?> = _uiState

    private val params = paramsContainer.require<WcSignTransactionComponent.Params>()

    init {
        modelScope.launch {
            val useCase: WcSignUseCase.SimpleRun<*> = useCaseFactory.createUseCase(params.rawRequest)
            useCase.invoke()
                .onEach { signState ->
                    if (signingIsDone(signState)) return@onEach
                    val signTransactionUM = (useCase as? WcSignUseCase)?.toUM(
                        signState = signState,
                        actions = WcTransactionActionsUM(
                            onDismiss = { cancel(useCase) },
                            onBack = ::showTransactionState,
                            activeButtonOnClick = useCase::sign,
                            onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                            transactionRequestOnClick = ::showTransactionRequestState,
                        ),
                    )
                    _uiState.emit(signTransactionUM)
                }
                .launchIn(this)
        }
    }

    private fun signingIsDone(signState: WcSignState<*>): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.let {
            router.pop()
            return true
        }
        return false
    }

    private fun cancel(useCase: WcSignUseCase) {
        useCase.cancel()
        router.pop()
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }

    private fun showTransactionRequestState() {
        _uiState.value = _uiState.value?.copy(state = WcSignTransactionUM.State.TRANSACTION_REQUEST_INFO)
    }

    private fun showTransactionState() {
        _uiState.value = _uiState.value?.copy(state = WcSignTransactionUM.State.TRANSACTION)
    }
}