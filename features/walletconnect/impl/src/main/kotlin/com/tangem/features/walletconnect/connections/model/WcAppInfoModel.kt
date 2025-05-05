package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.walletconnect.connections.components.WcAppInfoContainerComponent
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.features.walletconnect.connections.model.transformers.WcAppInfoTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcConnectButtonProgressTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.utils.transformer.update as transformerUpdate

@Stable
@ModelScoped
internal class WcAppInfoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    wcPairUseCaseFactory: WcPairUseCase.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: WcAppInfoContainerComponent.Params = paramsContainer.require()
    private val wcPairUseCase = wcPairUseCaseFactory.create(WcPairRequest(uri = params.wcUrl, source = params.source))
    val appInfoUiState: StateFlow<WcAppInfoUM>
    field = MutableStateFlow<WcAppInfoUM>(createLoadingState())

    // TODO(wc) Doston: Temp solution, will be fixed in next PR`s
    private var userWallet by Delegates.notNull<UserWallet>()
    private var proposalNetwork by Delegates.notNull<WcSessionProposal.ProposalNetwork>()

    init {
        loadDAppInfo()
    }

    private fun loadDAppInfo() {
        wcPairUseCase()
            .onEach { state ->
                when (state) {
                    is WcPairState.Approving.Loading -> appInfoUiState.transformerUpdate(
                        WcConnectButtonProgressTransformer(showProgress = true),
                    )
                    is WcPairState.Approving.Result -> {
                        appInfoUiState.transformerUpdate(
                            WcConnectButtonProgressTransformer(showProgress = false),
                        )
                        params.onDismiss()
                    }
                    is WcPairState.Error -> {
                        // TODO: wc show toast/snackbar/alert?
                    }
                    is WcPairState.Loading -> appInfoUiState.update { createLoadingState() }
                    is WcPairState.Proposal -> {
                        userWallet = state.dAppSession.proposalNetwork.keys.first { !it.isLocked }
                        proposalNetwork = state.dAppSession.proposalNetwork.getValue(userWallet)
                        appInfoUiState.transformerUpdate(
                            WcAppInfoTransformer(
                                dAppSession = state.dAppSession,
                                onDismiss = ::dismiss,
                                onConnect = ::onConnect,
                                userWallet = userWallet,
                                proposalNetwork = proposalNetwork,
                            ),
                        )
                    }
                }
            }
            .launchIn(modelScope)
    }

    fun dismiss() {
        wcPairUseCase.reject()
        params.onDismiss()
    }

    private fun onConnect() {
        wcPairUseCase.approve(WcSessionApprove(wallet = userWallet, network = proposalNetwork.required.toList()))
    }

    private fun createLoadingState(): WcAppInfoUM.Loading {
        return WcAppInfoUM.Loading(
            onDismiss = ::dismiss,
            connectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = false, onClick = ::onConnect),
        )
    }
}