package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.WcAddNetworkUseCase
import com.tangem.features.walletconnect.connections.routing.WcInnerRoute
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.converter.WcAddEthereumChainUMConverter
import com.tangem.features.walletconnect.transaction.entity.chain.WcAddEthereumChainUM
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes
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
    private val wcAddEthereumChainUMConverter: WcAddEthereumChainUMConverter,
) : Model(), WcCommonTransactionModel {

    private val _uiState = MutableStateFlow<WcAddEthereumChainUM?>(null)
    override val uiState: StateFlow<WcAddEthereumChainUM?> = _uiState

    val stackNavigation = StackNavigation<WcTransactionRoutes>()

    private val params = paramsContainer.require<WcTransactionModelParams>()

    init {
        modelScope.launch {
            val useCase: WcAddNetworkUseCase = useCaseFactory.createUseCase<WcAddNetworkUseCase>(params.rawRequest)
                .onLeft { router.push(WcInnerRoute.UnsupportedMethodAlert(params.rawRequest)) }
                .getOrNull() ?: return@launch
            _uiState.emit(
                wcAddEthereumChainUMConverter.convert(
                    WcAddEthereumChainUMConverter.Input(
                        useCase = useCase,
                        actions = WcTransactionActionsUM(
                            onShowVerifiedAlert = ::showVerifiedAlert,
                            onDismiss = { cancel(useCase) },
                            onSign = { sign(useCase) },
                            onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                        ),
                    ),
                ),
            )
        }
    }

    override fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    override fun popBack() {
        router.pop()
    }

    fun showTransactionRequest() {
        stackNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcTransactionRoutes.Alert(WcTransactionRoutes.Alert.Type.Verified(appName)))
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