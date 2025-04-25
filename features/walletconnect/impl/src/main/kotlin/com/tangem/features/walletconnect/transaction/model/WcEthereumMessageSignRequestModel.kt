package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletconnect.usecase.sign.WcSignStep
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.components.WcEthereumMessageSignRequestComponent
import com.tangem.features.walletconnect.transaction.entity.*
import com.tangem.features.walletconnect.transaction.entity.WcEthereumMessageSignRequestUM.State
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcEthereumMessageSignRequestModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val useCaseFactory: WcRequestUseCaseFactory,
) : Model() {

    private val _uiState = MutableStateFlow<WcEthereumMessageSignRequestUM?>(null)
    val uiState: StateFlow<WcEthereumMessageSignRequestUM?> = _uiState

    private val params = paramsContainer.require<WcEthereumMessageSignRequestComponent.Params>()

    init {
        modelScope.launch {
            val useCase: WcEthMessageSignUseCase = useCaseFactory.createUseCase(params.rawRequest)
            useCase.invoke()
                .onEach { signState ->
                    if (signingIsDone(useCase, signState)) return@onEach
                    _uiState.emit(domainToUM(useCase, signState))
                }
                .launchIn(this)
        }
    }

    private fun signingIsDone(
        useCase: WcEthMessageSignUseCase,
        signState: WcSignState<WcEthMessageSignUseCase.SignModel>,
    ): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.fold(
            ifLeft = { /* [REDACTED_TODO_COMMENT]*/ },
            ifRight = {
                cancel(useCase)
                return true
            },
        )
        return false
    }

    private fun domainToUM(
        useCase: WcEthMessageSignUseCase,
        signState: WcSignState<WcEthMessageSignUseCase.SignModel>,
    ): WcEthereumMessageSignRequestUM {
        return WcEthereumMessageSignRequestUM(
            startIconRes = R.drawable.ic_back_24,
            endIconRes = R.drawable.ic_close_24,
            state = State.TRANSACTION,
            actions = WcTransactionActionsUM(
                onDismiss = { cancel(useCase) },
                onBack = ::showTransactionState,
                onSign = useCase::sign,
                onCopy = { onCopy(signState.signModel.rawMsg) },
                transactionRequestOnClick = ::showTransactionRequestState,
            ),
            transaction = WcTransactionUM(
                appName = useCase.session.sdkModel.appMetaData.name,
                appIcon = useCase.session.sdkModel.appMetaData.url,
                isVerified = useCase.session.securityStatus == CheckDAppResult.SAFE,
                appSubtitle = useCase.session.sdkModel.appMetaData.description,
                walletName = useCase.session.wallet.name,
                networkInfo = WcNetworkInfoUM(
                    name = useCase.network.name,
                    iconRes = getActiveIconRes(useCase.network.id.value),
                ),
                isLoading = signState.domainStep == WcSignStep.Signing,
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(
                persistentListOf(
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_signature_type),
                        description = useCase.rawSdkRequest.request.method,
                    ),
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_contents),
                        description = signState.signModel.humanMsg,
                    ),
                ),
            ),
        )
    }

    private fun cancel(useCase: WcEthMessageSignUseCase) {
        useCase.cancel()
        router.pop()
    }

    private fun onCopy(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }

    private fun showTransactionRequestState() {
        _uiState.value = _uiState.value?.copy(state = State.TRANSACTION_REQUEST_INFO)
    }

    private fun showTransactionState() {
        _uiState.value = _uiState.value?.copy(state = State.TRANSACTION)
    }
}