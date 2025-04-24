package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthMessageSignUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.*
import com.tangem.features.walletconnect.transaction.entity.WcEthereumMessageSignRequestUM.State
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcEthereumMessageSignRequestModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    // private val requestConverter: WcRequestToUseCaseConverter,
    // private val request: WcSdkSessionRequest,
) : Model() {

    private val _uiState = MutableStateFlow<WcEthereumMessageSignRequestUM?>(null)
    val uiState: StateFlow<WcEthereumMessageSignRequestUM?> = _uiState

    init {
        modelScope.launch(dispatchers.io) {
            // requestConverter.toUseCase(request).collectLatest { useCase ->
            //     useCase().collectLatest {
            //         domainToUM(wcEthMessageSignUseCase, it.signModel)
            //     }
            // }
        }
    }

    private fun domainToUM(
        useCase: WcEthMessageSignUseCase,
        signModel: WcEthMessageSignUseCase.SignModel
    ): WcEthereumMessageSignRequestUM {
        return WcEthereumMessageSignRequestUM(
            startIconRes = R.drawable.ic_back_24,
            endIconRes = R.drawable.ic_close_24,
            state = State.TRANSACTION,
            actions = WcTransactionActionsUM(
                onDismiss = {}, // wcEthMessageSignUseCase::cancel,
                onBack = ::showTransactionState,
                onSign = {}, // wcEthMessageSignUseCase::sign,
                onCopy = { onCopy(signModel.rawMsg) },
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
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(
                persistentListOf(
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_signature_type),
                        description = useCase.rawSdkRequest.request.method,
                    ),
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_contents),
                        description = signModel.humanMsg,
                    ),
                ),
            ),
        )
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