package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.WcAddNetworkUseCase
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.entity.chain.WcAddEthereumChainUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.utils.toUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcAddNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val useCaseFactory: WcRequestUseCaseFactory,
) : Model() {

    private val _uiState = MutableStateFlow<WcAddEthereumChainUM?>(null)
    val uiState: StateFlow<WcAddEthereumChainUM?> = _uiState

    private val params = paramsContainer.require<WcTransactionModelParams>()

    init {
        modelScope.launch {
            val useCase: WcMethodUseCase = useCaseFactory.createUseCase(params.rawRequest)
            val transactionUM = (useCase as? WcAddNetworkUseCase)?.toUM(
                actions = WcTransactionActionsUM(
                    onShowVerifiedAlert = ::showVerifiedAlert,
                    onDismiss = { cancel(useCase) },
                    onSign = { sign(useCase) },
                    onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                ),
            )
            _uiState.emit(transactionUM)
        }
    }

    fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    @Suppress("UnusedPrivateMember")
    private fun showVerifiedAlert(appName: String) {
        // TODO(wc): Nastya [REDACTED_JIRA] // see WcPairComponent and WcPairModel
        // router.push(WcAppInfoRoutes.Alert(elements = message.elements))
    }

    private fun sign(useCase: WcAddNetworkUseCase) {
        modelScope.launch {
            _uiState.update { it?.copy(transaction = it.transaction.copy(isLoading = true)) }
            useCase.approve().getOrNull()?.let {
                router.pop()
            } ?: run {
                _uiState.update { it?.copy(transaction = it.transaction.copy(isLoading = false)) }
                TODO("[REDACTED_JIRA]")
            }
        }
    }

    private fun cancel(useCase: WcAddNetworkUseCase) {
        useCase.reject()
        router.pop()
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }
}