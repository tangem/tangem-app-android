package com.tangem.features.feed.components.market.details.portfolio.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.managetokens.CheckCurrencyUnsupportedUseCase
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.markets.SaveMarketTokensUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.domain.wallets.usecase.ColdWalletAndHasMissedDerivationsUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.impl.analytics.PortfolioAnalyticsEvent
import com.tangem.features.feed.components.market.details.portfolio.impl.loader.PortfolioData
import com.tangem.features.feed.components.market.details.portfolio.impl.loader.PortfolioDataLoader
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.features.feed.impl.R
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager as NewAddToPortfolioManager

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    tokenActionsIntentsFactory: TokenActionsHandler.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val messageSender: UiMessageSender,
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val portfolioDataLoader: PortfolioDataLoader,
    private val coldWalletAndHasMissedDerivationsUseCase: ColdWalletAndHasMissedDerivationsUseCase,
    private val saveMarketTokensUseCase: SaveMarketTokensUseCase,
    private val addToPortfolioManager: AddToPortfolioManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val userWalletImageFetcher: UserWalletImageFetcher,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    accountsFeatureToggles: AccountsFeatureToggles,
    newAddToPortfolioManagerFactory: NewAddToPortfolioManager.Factory,
    newMarketsPortfolioDelegateFactory: NewMarketsPortfolioDelegate.Factory,
) : Model() {

    private val _state: MutableStateFlow<MyPortfolioUM> = MutableStateFlow(value = MyPortfolioUM.Loading)
    val state: StateFlow<MyPortfolioUM> get() = _state

    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()
    private val analyticsEventBuilder = PortfolioAnalyticsEvent.EventBuilder(
        tokenSymbol = params.token.symbol,
        source = params.analyticsParams?.source,
    )

    val newAddToPortfolioManager: NewAddToPortfolioManager?
    val newMarketsPortfolioDelegate: NewMarketsPortfolioDelegate?

    /** Multi-wallet [UserWalletId] that user uses to add new tokens in AddToPortfolio bottom sheet */
    private val selectedMultiWalletIdFlow = MutableStateFlow<UserWalletId?>(value = null)

    private val portfolioBSVisibilityModelFlow = MutableStateFlow(value = PortfolioBSVisibilityModel())

    val bottomSheetNavigation: SlotNavigation<MarketsPortfolioRoute> = SlotNavigation()
    val addToPortfolioCallback = object : AddToPortfolioComponent.Callback {
        override fun onDismiss() = bottomSheetNavigation.dismiss()
        override fun onSuccess(addedToken: CryptoCurrency) = bottomSheetNavigation.dismiss()
    }

    private val currentAppCurrency = getSelectedAppCurrencyUseCase()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val tokenActionsHandler = tokenActionsIntentsFactory.create(
        currentAppCurrency = Provider { currentAppCurrency.value },
        onHandleQuickAction = { handledAction ->
            analyticsEventHandler.send(
                analyticsEventBuilder.quickActionClick(
                    actionUM = handledAction.action,
                    blockchainName = handledAction
                        .cryptoCurrencyData
                        .status
                        .currency
                        .network
                        .name,
                ),
            )
            configureReceiveAddresses(handledAction)
        },
    )

    private val factory = MyPortfolioUMFactory(
        onAddClick = {
            onAddToPortfolioBSVisibilityChange(isShow = true)
            // === Analytics ===
            analyticsEventHandler.send(
                analyticsEventBuilder.addToPortfolioClicked(),
            )
        },
        addToPortfolioBSContentUMFactory = AddToPortfolioBSContentUMFactory(
            addToPortfolioManager = addToPortfolioManager,
            token = params.token,
            onAddToPortfolioVisibilityChange = ::onAddToPortfolioBSVisibilityChange,
            onWalletSelectorVisibilityChange = ::onWalletSelectorVisibilityChange,
            onNetworkSwitchClick = ::onNetworkSwitchClick,
            onAnotherWalletSelect = { walletId ->
                onWalletSelect(walletId)
                // === Analytics ===
                analyticsEventHandler.send(
                    analyticsEventBuilder.addToPortfolioWalletChanged(),
                )
            },
            onContinueClick = { selectedWalletId, addedNetworks ->
                onContinueClick(selectedWalletId, addedNetworks)

                // === Analytics ===
                analyticsEventHandler.send(
                    analyticsEventBuilder.addToPortfolioContinue(
                        blockchainNames = addedNetworks.mapNotNull {
                            BlockchainUtils.getNetworkInfo(it.networkId)?.name
                        },
                    ),
                )
            },
        ),
        currentState = Provider { _state.value },
        tokenActionsHandler = tokenActionsHandler,
        updateTokens = { updateBlock ->
            updateTokensState { state ->
                state.copy(tokens = updateBlock(state.tokens))
            }
        },
    )

    init {
        if (accountsFeatureToggles.isFeatureEnabled) {
            newAddToPortfolioManager = newAddToPortfolioManagerFactory
                .create(
                    modelScope,
                    params.token,
                    params.analyticsParams?.source?.let { NewAddToPortfolioManager.AnalyticsParams(it) },
                )
            newMarketsPortfolioDelegate = newMarketsPortfolioDelegateFactory.create(
                scope = modelScope,
                token = params.token,
                tokenActionsHandler = tokenActionsHandler,
                buttonState = newAddToPortfolioManager.state.map { state ->
                    when (state) {
                        is NewAddToPortfolioManager.State.AvailableToAdd -> {
                            MyPortfolioUM.Tokens.AddButtonState.Available
                        }
                        NewAddToPortfolioManager.State.Init -> MyPortfolioUM.Tokens.AddButtonState.Loading
                        NewAddToPortfolioManager.State.NothingToAdd -> MyPortfolioUM.Tokens.AddButtonState.Unavailable
                    }
                },
                onAddClick = {
                    analyticsEventHandler.send(analyticsEventBuilder.addToPortfolioClicked())
                    bottomSheetNavigation.activate(MarketsPortfolioRoute.AddToPortfolio)
                },
            )
            newMarketsPortfolioDelegate.combineData()
                .onEach { _state.value = it }
                .flowOn(dispatchers.default)
                .launchIn(modelScope)
        } else {
            newAddToPortfolioManager = null
            newMarketsPortfolioDelegate = null
            // Subscribe on selected wallet flow to support actual selected wallet
            subscribeOnSelectedMultiWalletUpdates()

            subscribeOnStateUpdates()
        }
    }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        addToPortfolioManager.setAvailableNetworks(networks)
        newAddToPortfolioManager?.setTokenNetworks(networks)
        newMarketsPortfolioDelegate?.setTokenNetworks(networks)
    }

    fun setNoNetworksAvailable() {
        addToPortfolioManager.setAvailableNetworks(emptyList())
        newAddToPortfolioManager?.setTokenNetworks(emptyList())
        newMarketsPortfolioDelegate?.setTokenNetworks(emptyList())
    }

    private fun subscribeOnSelectedMultiWalletUpdates() {
        getSelectedWalletUseCase()
            .getOrElse { e ->
                Timber.e("Failed to load selected wallet: $e")
                error("Failed to load selected wallet")
            }
            .onEach { userWallet ->
                selectedMultiWalletIdFlow.value = userWallet.takeIf { it.isMultiCurrency }?.walletId
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnStateUpdates() {
        combine(
            flow = loadPortfolioDataWithArtworks(params.token.id),
            flow2 = getPortfolioUIDataFlow(),
            transform = { pair, portfolioUIData ->
                val (portfolioData, artworks) = pair
                factory.create(portfolioData, portfolioUIData, artworks)
            },
        )
            .onEach { _state.value = it }
            .launchIn(modelScope)
    }

    private fun loadPortfolioDataWithArtworks(
        currencyRawId: CryptoCurrency.RawID,
    ): Flow<Pair<PortfolioData, Map<UserWalletId, UserWalletItemUM.ImageState>>> {
        val wallets = Channel<Set<UserWallet>>()
        val portfolioFlow = portfolioDataLoader
            .load(currencyRawId)
            .onEach { wallets.trySend(it.walletsWithCurrencies.keys) }

        val artworksFlow = wallets.receiveAsFlow()
            .distinctUntilChanged()
            .flatMapLatest { userWalletImageFetcher.walletsImage(wallets = it, size = ArtworkSize.SMALL) }

        return combine(
            flow = portfolioFlow,
            flow2 = artworksFlow,
        ) { portfolioData, artworks -> portfolioData to artworks }
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
                    isNeededColdWalletInteraction = isNeededColdWalletInteraction(selectedWalletId, addToPortfolioData),
                )
            },
        )
    }

    private suspend fun isNeededColdWalletInteraction(
        selectedWalletId: UserWalletId?,
        addToPortfolioData: AddToPortfolioManager.AddToPortfolioData,
    ): Boolean {
        return if (selectedWalletId != null) {
            coldWalletAndHasMissedDerivationsUseCase.invoke(
                userWalletId = selectedWalletId,
                networksWithDerivationPath = addToPortfolioData.addedNetworks[selectedWalletId].orEmpty()
                    .associate { it.networkId to null },
            )
        } else {
            false
        }
    }

    private fun onNetworkSwitchClick(blockchainRowUM: BlockchainRowUM, isChecked: Boolean) {
        val selectedWalletId = selectedMultiWalletIdFlow.value

        if (selectedWalletId == null) {
            Timber.e("Impossible to switch network when selected wallet is null")
            return
        }

        if (isChecked) {
            modelScope.launch {
                val unsupportedState = checkCurrencyUnsupportedState(
                    userWalletId = selectedWalletId,
                    rawNetworkId = blockchainRowUM.id,
                    isMainNetwork = blockchainRowUM.isMainNetwork,
                )
                if (unsupportedState != null) {
                    showUnsupportedWarning(unsupportedState)
                } else {
                    addToPortfolioManager.addNetwork(userWalletId = selectedWalletId, networkId = blockchainRowUM.id)
                }
            }
        } else {
            addToPortfolioManager.removeNetwork(userWalletId = selectedWalletId, networkId = blockchainRowUM.id)
        }
    }

    private suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        rawNetworkId: String,
        isMainNetwork: Boolean,
    ): CurrencyUnsupportedState? {
        return checkCurrencyUnsupportedUseCase(
            userWalletId = userWalletId,
            networkId = rawNetworkId,
            isMainNetwork = isMainNetwork,
        ).getOrElse { throwable ->
            Timber.e(
                throwable,
                """
                    Failed to check currency unsupported state
                    |- User wallet ID: $userWalletId
                    |- Network ID: $rawNetworkId
                    |- Is main network: $isMainNetwork
                """.trimIndent(),
            )

            val message = SnackbarMessage(
                message = throwable.localizedMessage
                    ?.let(::stringReference)
                    ?: resourceReference(R.string.common_error),
            )
            messageSender.send(message)

            null
        }
    }

    private fun showUnsupportedWarning(unsupportedState: CurrencyUnsupportedState) {
        val message = DialogMessage(
            message = when (unsupportedState) {
                is CurrencyUnsupportedState.Token.NetworkTokensUnsupported -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.Token.UnsupportedCurve -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.UnsupportedNetwork -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
            },
        )

        messageSender.send(message)
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
            it.copy(isAddToPortfolioBSVisible = isShow, isWalletSelectorBSVisible = false)
        }
    }

    private fun onWalletSelectorVisibilityChange(isShow: Boolean) {
        portfolioBSVisibilityModelFlow.update {
            it.copy(isAddToPortfolioBSVisible = true, isWalletSelectorBSVisible = isShow)
        }
    }

    private fun updateTokensState(block: (MyPortfolioUM.Tokens) -> MyPortfolioUM) {
        _state.update { stateToUpdate ->
            val tokensState = stateToUpdate as? MyPortfolioUM.Tokens ?: return@update stateToUpdate
            block(tokensState)
        }
    }

    private fun configureReceiveAddresses(quickAction: TokenActionsHandler.HandledQuickAction) {
        val isNewReceive = quickAction.action == TokenActionsBSContentUM.Action.Receive
        if (isNewReceive) {
            modelScope.launch {
                val tokenConfig = receiveAddressesFactory.create(
                    status = quickAction.cryptoCurrencyData.status,
                    userWalletId = quickAction.cryptoCurrencyData.userWallet.walletId,
                ) ?: return@launch
                bottomSheetNavigation.activate(MarketsPortfolioRoute.TokenReceive(tokenConfig))
            }
        }
    }
}