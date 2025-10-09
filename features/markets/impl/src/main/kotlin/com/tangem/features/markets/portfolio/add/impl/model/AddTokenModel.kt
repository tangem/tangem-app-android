package com.tangem.features.markets.portfolio.add.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.markets.SaveMarketTokensUseCase
import com.tangem.domain.wallets.usecase.ColdWalletAndHasMissedDerivationsUseCase
import com.tangem.features.markets.portfolio.add.impl.AddTokenComponent
import com.tangem.features.markets.portfolio.add.impl.model.AddTokenUiBuilder.Companion.toggleProgress
import com.tangem.features.markets.portfolio.add.impl.ui.state.AddTokenUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AddTokenModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val saveMarketTokensUseCase: SaveMarketTokensUseCase,
    private val uiBuilder: AddTokenUiBuilder,
    private val coldWalletAndHasMissedDerivationsUseCase: ColdWalletAndHasMissedDerivationsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<AddTokenComponent.Params>()
    private val selectedNetwork get() = params.selectedNetwork.value
    private val selectedPortfolio get() = params.selectedPortfolio.value

    val uiState: StateFlow<AddTokenUM>
        field = MutableStateFlow(value = uiBuilder.getInitialState())

    init {
        combine(
            flow = params.selectedNetwork,
            flow2 = params.selectedPortfolio,
            flow3 = flow { emit(needColdWalletInteraction()) },
            transform = { selectedNetwork, selectedPortfolio, isTangemIconVisible ->
                val newUI = uiBuilder.updateContent(
                    current = uiState.value,
                    selectedPortfolio = selectedPortfolio,
                    selectedNetwork = selectedNetwork,
                    isTangemIconVisible = isTangemIconVisible,
                    onConfirmClick = { onAddClick() },
                )
                uiState.update { newUI }
            },
        ).flowOn(dispatchers.default).launchIn(modelScope)
    }

    private fun onAddClick() = modelScope.launch {
        uiState.update { it.toggleProgress(true) }
        // todo account
        saveMarketTokensUseCase(
            userWalletId = selectedPortfolio.userWallet.walletId,
            tokenMarketParams = params.marketParams,
            addedNetworks = setOf(selectedNetwork.selectedNetwork),
            removedNetworks = setOf(),
        )
        uiState.update { it.toggleProgress(false) }
        params.onTokenAdded()
    }

    private suspend fun needColdWalletInteraction(): Boolean = coldWalletAndHasMissedDerivationsUseCase.invoke(
        userWalletId = selectedPortfolio.userWallet.walletId,
        networksWithDerivationPath = mapOf(selectedNetwork.selectedNetwork.networkId to null),
    )
}