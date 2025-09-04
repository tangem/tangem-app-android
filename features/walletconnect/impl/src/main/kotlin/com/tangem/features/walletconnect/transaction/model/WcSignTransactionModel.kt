package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
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
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.converter.WcHandleMethodErrorConverter
import com.tangem.features.walletconnect.transaction.converter.WcSignTransactionUMConverter
import com.tangem.features.walletconnect.transaction.converter.WcSignTypedDataUMConverter
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionUM
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class WcSignTransactionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val analytics: AnalyticsEventHandler,
    private val useCaseFactory: WcRequestUseCaseFactory,
    private val signTypedDataUMConverter: WcSignTypedDataUMConverter,
    private val signTransactionUMConverter: WcSignTransactionUMConverter,
) : Model(), WcCommonTransactionModel {

    private val params = paramsContainer.require<WcTransactionModelParams>()

    private val _uiState = MutableStateFlow<WcSignTransactionUM?>(null)
    override val uiState: StateFlow<WcSignTransactionUM?> = _uiState

    val stackNavigation = StackNavigation<WcTransactionRoutes>()

    private var useCase by Delegates.notNull<WcMessageSignUseCase>()
    private val signatureReceivedAnalyticsSendState = MutableStateFlow(false)

    init {
        modelScope.launch {
            useCase = useCaseFactory.createUseCase<WcMessageSignUseCase>(params.rawRequest)
                .onLeft { router.push(WcHandleMethodErrorConverter.convert(it)) }
                .getOrNull() ?: return@launch
            sendSignatureReceivedAnalytics(useCase)
            useCase.invoke()
                .onEach { signState ->
                    if (signingIsDone(signState)) return@onEach
                    val signTransactionUM = convertToUI(useCase, signState)
                    _uiState.emit(signTransactionUM)
                }
                .launchIn(this)
        }
    }

    private fun convertToUI(
        useCase: WcMessageSignUseCase,
        signState: WcSignState<WcMessageSignUseCase.SignModel>,
    ): WcSignTransactionUM? {
        val actions = WcTransactionActionsUM(
            onShowVerifiedAlert = ::showVerifiedAlert,
            onDismiss = { cancel(useCase) },
            onSign = useCase::sign,
            onCopy = { copyData(useCase.rawSdkRequest.request.params) },
        )
        return when (useCase.method) {
            is WcEthMethod.SignTypedData -> signTypedDataUMConverter.convert(
                WcSignTypedDataUMConverter.Input(
                    context = useCase,
                    signState = signState,
                    signModel = signState.signModel,
                    actions = actions,
                ),
            )
            is WcEthMethod.MessageSign, is WcSolanaMethod.SignMessage -> signTransactionUMConverter.convert(
                WcSignTransactionUMConverter.Input(
                    context = useCase,
                    signState = signState,
                    signModel = signState.signModel,
                    actions = actions,
                ),
            )
            else -> null
        }
    }

    override fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    override fun popBack() {
        stackNavigation.pop()
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

    private fun signingIsDone(signState: WcSignState<*>): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.let { result ->
            if (result.isRight()) {
                showSuccessSignMessage()
            }
            router.pop()
            return true
        }
        return false
    }

    private fun cancel(useCase: WcSignUseCase<*>) {
        useCase.cancel()
        router.pop()
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }

    private fun sendSignatureReceivedAnalytics(useCase: WcMessageSignUseCase) {
        if (signatureReceivedAnalyticsSendState.value) return

        analytics.send(
            WcAnalyticEvents.SignatureRequestReceived(
                rawRequest = useCase.rawSdkRequest,
                network = useCase.network,
                emulationStatus = null,
                securityStatus = null,
            ),
        )

        signatureReceivedAnalyticsSendState.value = true
    }
}