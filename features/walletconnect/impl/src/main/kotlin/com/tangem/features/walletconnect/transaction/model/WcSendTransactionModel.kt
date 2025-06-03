package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.domain.blockaid.models.transaction.TransactionData
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.*
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes
import com.tangem.features.walletconnect.transaction.converter.WcCommonTransactionUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcSendTransactionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val useCaseFactory: WcRequestUseCaseFactory,
    private val converter: WcCommonTransactionUMConverter,
) : Model(), WcCommonTransactionModel {

    private val params = paramsContainer.require<WcTransactionModelParams>()

    private val _uiState = MutableStateFlow<WcSendTransactionUM?>(null)
    override val uiState: StateFlow<WcSendTransactionUM?> = _uiState

    val stackNavigation = StackNavigation<WcTransactionRoutes>()

    init {
        modelScope.launch {
            when (val useCase: WcSignUseCase<TransactionData> = useCaseFactory.createUseCase(params.rawRequest)) {
                is WcTransactionUseCase,
                is WcListTransactionUseCase,
                -> {
                    useCase.invoke().onEach { signState ->
                        if (signingIsDone(signState)) return@onEach
                        val transactionUM = converter.convert(
                            WcCommonTransactionUMConverter.Input(
                                useCase = useCase,
                                signState = signState,
                                actions = WcTransactionActionsUM(
                                    onShowVerifiedAlert = ::showVerifiedAlert,
                                    onDismiss = { cancel(useCase) },
                                    onSign = useCase::sign,
                                    onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                                ),
                            ),
                        ) as? WcSendTransactionUM
                        _uiState.emit(transactionUM)
                    }
                        .launchIn(this)
                }
            }
        }
    }

    override fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    fun showTransactionRequest() {
        stackNavigation.pushNew(WcTransactionRoutes.TransactionRequestInfo)
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcTransactionRoutes.Alert(WcTransactionRoutes.Alert.Type.Verified(appName)))
    }

    private fun signingIsDone(signState: WcSignState<*>): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.let {
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
}