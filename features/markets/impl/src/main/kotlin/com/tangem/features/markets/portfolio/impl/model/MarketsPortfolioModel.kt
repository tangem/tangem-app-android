package com.tangem.features.markets.portfolio.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
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
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.managetokens.CheckCurrencyUnsupportedUseCase
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.markets.SaveMarketTokensUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.ArtworkModel
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.GetViewedTokenReceiveWarningUseCase
import com.tangem.domain.transaction.usecase.GetEnsNameUseCase
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.HasMissedDerivationsUseCase
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.analytics.PortfolioAnalyticsEvent
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.loader.PortfolioDataLoader
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.features.tokenreceive.TokenReceiveFeatureToggle
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

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
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val saveMarketTokensUseCase: SaveMarketTokensUseCase,
    private val getCardImageUseCase: GetCardImageUseCase,
    private val addToPortfolioManager: AddToPortfolioManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val tokenReceiveFeatureToggle: TokenReceiveFeatureToggle,
    private val getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase,
    private val getEnsNameUseCase: GetEnsNameUseCase,
) : Model() {

    val state: StateFlow<MyPortfolioUM> get() = _state
    private val _state: MutableStateFlow<MyPortfolioUM> = MutableStateFlow(value = MyPortfolioUM.Loading)

    private val loadedArtworks: HashMap<UserWalletId, ArtworkModel> = hashMapOf()
    private val artworksState: MutableStateFlow<HashMap<UserWalletId, ArtworkModel>> = MutableStateFlow(hashMapOf())
    private val loadArtworksMutex = Mutex()

    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()
    private val analyticsEventBuilder = PortfolioAnalyticsEvent.EventBuilder(
        token = params.token,
        source = params.analyticsParams?.source,
    )

    /** Multi-wallet [UserWalletId] that user uses to add new tokens in AddToPortfolio bottom sheet */
    private val selectedMultiWalletIdFlow = MutableStateFlow<UserWalletId?>(value = null)

    private val portfolioBSVisibilityModelFlow = MutableStateFlow(value = PortfolioBSVisibilityModel())

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()

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
            onAnotherWalletSelect = {
                onWalletSelect(it)
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
        tokenActionsHandler = tokenActionsIntentsFactory.create(
            currentAppCurrency = Provider { currentAppCurrency.value },
            updateTokenReceiveBSConfig = { updateBlock ->
                if (tokenReceiveFeatureToggle.isNewTokenReceiveEnabled.not()) {
                    updateTokensState {
                        it.copy(tokenReceiveBSConfig = updateBlock(it.tokenReceiveBSConfig))
                    }
                }
            },
            onHandleQuickAction = { handledAction ->
                analyticsEventHandler.send(
                    analyticsEventBuilder.quickActionClick(
                        actionUM = handledAction.action,
                        blockchainName = handledAction.cryptoCurrencyData.status.currency.network.name,
                    ),
                )
                if (tokenReceiveFeatureToggle.isNewTokenReceiveEnabled) {
                    configureReceiveAddresses(handledAction)
                }
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
            flow = loadPortfolioData(params.token.id),
            flow2 = getPortfolioUIDataFlow(),
            flow3 = artworksState,
            transform = factory::create,
        )
            .onEach { _state.value = it }
            .launchIn(modelScope)
    }

    private fun loadPortfolioData(currencyRawId: CryptoCurrency.RawID): Flow<PortfolioData> {
        portfolioDataLoader.load(currencyRawId).onEach {
            loadArtworks(it.walletsWithCurrencies.keys.toList())
        }.also { return it }
    }

    private fun loadArtworks(wallets: List<UserWallet>) {
        modelScope.launch {
            loadArtworksMutex.withLock {
                wallets.filterIsInstance<UserWallet.Cold>().forEach { wallet ->
                    if (!loadedArtworks.containsKey(wallet.walletId)) {
                        val artwork = getCardImageUseCase(
                            cardId = wallet.cardId,
                            manufacturerName = wallet.scanResponse.card.manufacturer.name,
                            firmwareVersion = wallet.scanResponse.card.firmwareVersion.toSdkFirmwareVersion(),
                            cardPublicKey = wallet.scanResponse.card.cardPublicKey,
                        )
                        loadedArtworks[wallet.walletId] = artwork
                        artworksState.emit(loadedArtworks)
                    }
                }
            }
        }
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
        ).getOrElse {
            Timber.e(
                it,
                """
                    Failed to check currency unsupported state
                    |- User wallet ID: $userWalletId
                    |- Network ID: $rawNetworkId
                    |- Is main network: $isMainNetwork
                """.trimIndent(),
            )

            val message = SnackbarMessage(
                message = it.localizedMessage
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

    private fun configureReceiveAddresses(quickAction: TokenActionsHandler.HandledQuickAction) {
        when (quickAction.action) {
            TokenActionsBSContentUM.Action.Receive -> {
                val addresses = quickAction.cryptoCurrencyData.status.value.networkAddress ?: return
                val cryptoCurrency = quickAction.cryptoCurrencyData.status.currency
                modelScope.launch {
                    val ensName = getEnsNameUseCase.invoke(
                        userWalletId = quickAction.cryptoCurrencyData.userWallet.walletId,
                        network = cryptoCurrency.network,
                        address = addresses.defaultAddress.value,
                    )

                    val receiveAddresses = buildList {
                        ensName?.let { ens ->
                            add(
                                ReceiveAddressModel(
                                    nameService = ReceiveAddressModel.NameService.Ens,
                                    value = ens,
                                    displayName = ens,
                                ),
                            )
                        }
                        addresses.availableAddresses.map { address ->
                            add(
                                ReceiveAddressModel(
                                    nameService = ReceiveAddressModel.NameService.Default,
                                    value = address.value,
                                    displayName = "${cryptoCurrency.name} (${cryptoCurrency.symbol})",
                                ),
                            )
                        }
                    }
                    val tokenConfig = TokenReceiveConfig(
                        shouldShowWarning = cryptoCurrency.name !in getViewedTokenReceiveWarningUseCase(),
                        cryptoCurrency = cryptoCurrency,
                        userWalletId = quickAction.cryptoCurrencyData.userWallet.walletId,
                        showMemoDisclaimer = cryptoCurrency.network.transactionExtrasType != Network
                            .TransactionExtrasType.NONE,
                        receiveAddress = receiveAddresses,
                    )
                    bottomSheetNavigation.activate(tokenConfig)
                }
            }
            else -> Unit
        }
    }
}