package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.WcAddNetworkUseCase
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.converter.WcAddEthereumChainUMConverter
import com.tangem.features.walletconnect.transaction.converter.WcHandleMethodErrorConverter
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
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class WcAddNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val analytics: AnalyticsEventHandler,
    private val useCaseFactory: WcRequestUseCaseFactory,
    private val wcAddEthereumChainUMConverter: WcAddEthereumChainUMConverter,
) : Model(), WcCommonTransactionModel {

    private val _uiState = MutableStateFlow<WcAddEthereumChainUM?>(null)
    override val uiState: StateFlow<WcAddEthereumChainUM?> = _uiState

    val stackNavigation = StackNavigation<WcTransactionRoutes>()

    private val params = paramsContainer.require<WcTransactionModelParams>()
    private var useCase by Delegates.notNull<WcAddNetworkUseCase>()
    private val signatureReceivedAnalyticsSendState = MutableStateFlow(false)

    init {
        modelScope.launch {
            useCase = useCaseFactory.createUseCase<WcAddNetworkUseCase>(params.rawRequest)
                .onLeft { router.push(WcHandleMethodErrorConverter.convert(it)) }
                .getOrNull() ?: return@launch
            sendSignatureReceivedAnalytics(useCase)
            val either = useCase.invoke()
            either
                .onLeft { router.push(WcHandleMethodErrorConverter.convert(it)) }
                .map {
                    if (it.isExistInWcSession) cancel(useCase) else showUI()
                }
        }
    }

    private fun showUI() {
        _uiState.value = wcAddEthereumChainUMConverter.convert(
            WcAddEthereumChainUMConverter.Input(
                useCase = useCase,
                actions = WcTransactionActionsUM(
                    onShowVerifiedAlert = ::showVerifiedAlert,
                    onDismiss = { cancel(useCase) },
                    onSign = { sign(useCase) },
                    onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                ),
            ),
        )
    }

    override fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    override fun popBack() {
        router.pop()
    }

    fun showTransactionRequest() {
        analytics.send(
            WcAnalyticEvents.TransactionDetailsOpened(
                rawRequest = useCase.rawSdkRequest,
                network = useCase.network,
            ),
        )
        stackNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcTransactionRoutes.Alert(WcTransactionRoutes.Alert.Type.Verified(appName)))
    }

    private fun sign(useCase: WcAddNetworkUseCase) {
        modelScope.launch {
            _uiState.update { it?.copy(transaction = it.transaction.copy(isLoading = true)) }
            useCase.approve().getOrNull()?.let {
                showSuccessSignMessage()
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

    private fun sendSignatureReceivedAnalytics(useCase: WcAddNetworkUseCase) {
        if (signatureReceivedAnalyticsSendState.value) return

        analytics.send(
            WcAnalyticEvents.SignatureRequestReceived(
                rawRequest = useCase.rawSdkRequest,
                network = useCase.network,
                emulationStatus = null,
            ),
        )

        signatureReceivedAnalyticsSendState.value = true
    }
}