package com.tangem.features.markets.portfolio.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.HasMissedDerivationsUseCase
import com.tangem.domain.markets.SaveMarketTokensUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.loader.PortfolioDataLoader
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ComponentScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    tokenActionsIntentsFactory: TokenActionsHandler.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val portfolioDataLoader: PortfolioDataLoader,
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val saveMarketTokensUseCase: SaveMarketTokensUseCase,
    private val addToPortfolioManager: AddToPortfolioManager,
) : Model() {

    val state: StateFlow<MyPortfolioUM> get() = _state
    private val _state: MutableStateFlow<MyPortfolioUM> = MutableStateFlow(value = MyPortfolioUM.Loading)

    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()

    /** Multi-wallet [UserWalletId] that user uses to add new tokens in AddToPortfolio bottom sheet */
    private val selectedMultiWalletIdFlow = MutableStateFlow<UserWalletId?>(value = null)

    private val portfolioBSVisibilityModelFlow = MutableStateFlow(value = PortfolioBSVisibilityModel())

    private val currentAppCurrency = getSelectedAppCurrencyUseCase()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val factory = MyPortfolioUMFactory(
        onAddClick = { onAddToPortfolioBSVisibilityChange(isShow = true) },
        addToPortfolioBSContentUMFactory = AddToPortfolioBSContentUMFactory(
            token = params.token,
            onAddToPortfolioVisibilityChange = ::onAddToPortfolioBSVisibilityChange,
            onWalletSelectorVisibilityChange = ::onWalletSelectorVisibilityChange,
            onNetworkSwitchClick = ::onNetworkSwitchClick,
            onWalletSelect = ::onWalletSelect,
            onContinueClick = ::onContinueClick,
        ),
        currentState = Provider { _state.value },
        tokenActionsHandler = tokenActionsIntentsFactory.create(
            currentAppCurrency = Provider { currentAppCurrency.value },
            updateTokenReceiveBSConfig = { updateBlock ->
                updateTokensState { it.copy(tokenReceiveBSConfig = updateBlock(it.tokenReceiveBSConfig)) }
            },
        ),
        updateTokens = { updateBlock ->
            updateTokensState { state ->
                state.copy(tokens = updateBlock(state.tokens))
            }
        },
    )

    init {
        // Subscribe on selected wallet flow to support actual selected wallet
        subscribeOnSelectedMultiWalletUpdates()

        subscribeOnStateUpdates()
    }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        addToPortfolioManager.setAvailableNetworks(networks)
    }

    fun setNoNetworksAvailable() {
        addToPortfolioManager.setAvailableNetworks(emptyList())
    }

    private fun subscribeOnSelectedMultiWalletUpdates() {
        getSelectedWalletUseCase()
            .getOrElse { e ->
                Timber.e("Failed to load selected wallet: $e")
                error("Failed to load selected wallet")
            }
            .onEach {
                selectedMultiWalletIdFlow.value = it.takeIf { it.isMultiCurrency }?.walletId
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnStateUpdates() {
        combine(
            flow = portfolioDataLoader.load(params.token.id),
            flow2 = getPortfolioUIDataFlow(),
            transform = factory::create,
        )
            .onEach { _state.value = it }
            .launchIn(modelScope)
    }

    private fun getPortfolioUIDataFlow(): Flow<PortfolioUIData> {
        return combine(
            flow = portfolioBSVisibilityModelFlow,
            flow2 = selectedMultiWalletIdFlow,
            flow3 = addToPortfolioManager.getAddToPortfolioData(),
            transform = { portfolioBSVisibilityModel, selectedWalletId, addToPortfolioData ->
                PortfolioUIData(
                    portfolioBSVisibilityModel = portfolioBSVisibilityModel,
                    selectedWalletId = selectedWalletId,
                    addToPortfolioData = addToPortfolioData,
                    hasMissedDerivations = hasMissedDerivations(selectedWalletId, addToPortfolioData),
                )
            },
        )
    }

    private suspend fun hasMissedDerivations(
        selectedWalletId: UserWalletId?,
        addToPortfolioData: AddToPortfolioManager.AddToPortfolioData,
    ): Boolean {
        return if (selectedWalletId != null) {
            hasMissedDerivationsUseCase.invoke(
                userWalletId = selectedWalletId,
                networksWithDerivationPath = addToPortfolioData.addedNetworks[selectedWalletId].orEmpty()
                    .associate { Network.ID(it.networkId) to null },
            )
        } else {
            false
        }
    }

    private fun onNetworkSwitchClick(networkId: String, isChecked: Boolean) {
        val selectedWalletId = selectedMultiWalletIdFlow.value

        if (selectedWalletId == null) {
            Timber.e("Impossible to switch network when selected wallet is null")
            return
        }

        if (isChecked) {
            addToPortfolioManager.addNetwork(userWalletId = selectedWalletId, networkId = networkId)
        } else {
            addToPortfolioManager.removeNetwork(userWalletId = selectedWalletId, networkId = networkId)
        }
    }

    private fun onWalletSelect(userWalletId: UserWalletId) {
        selectedMultiWalletIdFlow.update { prevUserWalletId ->
            prevUserWalletId?.let(addToPortfolioManager::removeAllChanges)

            userWalletId
        }
    }

    private fun onContinueClick(userWalletId: UserWalletId, addedNetworks: Set<TokenMarketInfo.Network>) {
        modelScope.launch {
            saveMarketTokensUseCase(
                userWalletId = userWalletId,
                tokenMarketParams = params.token,
                addedNetworks = addedNetworks,
                removedNetworks = emptySet(),
            )

            onAddToPortfolioBSVisibilityChange(isShow = false)

            addToPortfolioManager.removeAllChanges(userWalletId)
        }
    }

    private fun onAddToPortfolioBSVisibilityChange(isShow: Boolean) {
        portfolioBSVisibilityModelFlow.update {
            it.copy(addToPortfolioBSVisibility = isShow, walletSelectorBSVisibility = false)
        }
    }

    private fun onWalletSelectorVisibilityChange(isShow: Boolean) {
        portfolioBSVisibilityModelFlow.update {
            it.copy(addToPortfolioBSVisibility = true, walletSelectorBSVisibility = isShow)
        }
    }

    private fun updateTokensState(block: (MyPortfolioUM.Tokens) -> MyPortfolioUM) {
        _state.update { stateToUpdate ->
            val tokensState = stateToUpdate as? MyPortfolioUM.Tokens ?: return@update stateToUpdate
            block(tokensState)
        }
    }
}