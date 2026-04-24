package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionsConverter.toQuickActions
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.*
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addtoportfolio.AddTokenComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.ChooseNetworkComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.analytics.PortfolioAnalyticsEvent
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.UserPortfolioComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.state.UserPortfolioStateController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TOKEN_ACTIONS_DELAY = 500L

@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class AddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    designFeatureToggles: DesignFeatureToggles,
    override val dispatchers: CoroutineDispatcherProvider,
    val portfolioSelectorController: PortfolioSelectorController,
    private val callbackDelegate: AddToPortfolioCallbackDelegate,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val selectionResolver: AddToPortfolioInitialSelectionResolver,
    userPortfolioStateControllerFactory: UserPortfolioStateController.Factory,
) : Model(),
    ChooseNetworkComponent.Callbacks by callbackDelegate,
    TokenActionsComponent.Callbacks by callbackDelegate,
    AddTokenComponent.Callbacks by callbackDelegate,
    UserPortfolioComponent.Callbacks by callbackDelegate {

    private val params = paramsContainer.require<AddToPortfolioComponent.Params>()
    val navigation = StackNavigation<AddToPortfolioRoutes>()
    var currentStack = listOf<AddToPortfolioRoutes>(AddToPortfolioRoutes.Empty)

    /* Flows that hold state and provide it to child models */
    val selectedNetwork: MutableSharedFlow<SelectedNetwork> = replayMutableSharedFlow()
    val selectedPortfolio: MutableSharedFlow<SelectedPortfolio> = replayMutableSharedFlow()
    val tokenActionsData: MutableSharedFlow<CryptoCurrencyData> = replayMutableSharedFlow()

    val addToPortfolioManager: AddToPortfolioManager = params.addToPortfolioManager
    val portfolioFetcher: PortfolioFetcher = addToPortfolioManager.portfolioFetcher
    val eventBuilder: PortfolioAnalyticsEvent.EventBuilder by lazy {
        val tokenMarketParams = addToPortfolioManager.paramsFlow.replayCache.first().token
        PortfolioAnalyticsEvent.EventBuilder(
            tokenSymbol = tokenMarketParams.symbol,
            source = addToPortfolioManager.analyticsParams.source,
            category = addToPortfolioManager.analyticsParams.category,
        )
    }

    val userPortfolioStateController = userPortfolioStateControllerFactory.create(
        modelScope = modelScope,
        onTokenSelected = { result -> addToPortfolioManager.onAddedTokenClick(result) },
    )

    val featureData: Flow<AddToPortfolioManager.State> = combineFeatureData()

    private val globalSelectedWallet: UserWallet?
        get() = getSelectedWalletSyncUseCase().getOrNull()
            .takeIf { it?.isMultiCurrency == true }

    init {
        navigation.subscribe { currentStack = it.transformer.invoke(currentStack) }
        if (designFeatureToggles.isRedesignEnabled) {
            startRedesignAddToPortfolioFlow()
        } else {
            startLegacyAddToPortfolioFlow()
        }
    }

    private fun <T> replayMutableSharedFlow() = MutableSharedFlow<T>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @Suppress("LongMethod")
    private fun startLegacyAddToPortfolioFlow() {
        channelFlow<Unit> {
            fun finishSuccessFlow(result: AddToPortfolioManager.Result) {
                addToPortfolioManager.onSuccessAdded(result)
                channel.close()
            }

            val featureDataFlow: StateFlow<AvailableToAddData> = featureData
                .filterIsInstance<AddToPortfolioManager.State.Ready>()
                .map { it.availableToAddData }
                .distinctUntilChanged()
                .stateIn(this)
            val isAccountMode = portfolioSelectorController.isAccountModeSync()

            // use snapshot data, looks like we don’t need to remap at runtime
            val data = featureDataFlow.value

            // init data flows, emits on user/code selection, updates state holder
            val firstSelectedPortfolio = setupPortfolioFlow(data)
                .onEach { selectedPortfolio.emit(it) }
            val firstSelectedNetwork = setupNetworkFlow(firstSelectedPortfolio)
                .onEach { selectedNetwork.emit(it) }

            val isSinglePortfolio = data.isSinglePortfolio
            if (isSinglePortfolio) {
                val accountId = data.availableToAddWallets.values.first()
                    .availableToAddAccounts.values.first()
                    .account.account.accountId
                // force select a portfolio, triggers [selectedPortfolio]
                portfolioSelectorController.selectAccount(accountId)
            } else {
                logAccountSelector(isAccountMode)
                navigation.replaceAll(AddToPortfolioRoutes.PortfolioSelector)
            }

            val firstPartOfNavigation: Job = firstSelectedPortfolio
                .onEach { portfolio ->
                    val isSingleAvailableNetwork = portfolio.account.isSingleNetwork
                    when {
                        // force select a network, triggers [selectedNetwork]
                        isSingleAvailableNetwork -> {
                            val singleNetwork = portfolio.account.availableToAddNetworks.first()
                            callbackDelegate.onNetworkSelected(singleNetwork)
                        }
                        // it's important to control root screen, UI depends on it(close/arrow icon)
                        isSinglePortfolio -> navigation.replaceAll(routeToNetworkSelector(portfolio))
                        else -> navigation.pushNew(routeToNetworkSelector(portfolio))
                    }
                }
                .launchIn(this)

            // main flow that combine all require data
            val allRequireForAdd = combine(
                flow = firstSelectedNetwork,
                flow2 = firstSelectedPortfolio,
                transform = { a, b -> a to b },
            )

            // suspend until all required data is selected
            val firstPair = allRequireForAdd.first()
            // line of navigation to AddToken screen is finished; cancel the job, select a new root screen
            firstPartOfNavigation.cancel()

            val selectedNetworkName = firstPair.first.cryptoCurrency.network.name
            analyticsEventHandler.send(event = eventBuilder.popupToConfirm(selectedNetworkName))
            navigation.replaceAll(AddToPortfolioRoutes.AddToken)

            var middleNavigationJob: Job? = null
            // handle actions from AddToken screen
            callbackDelegate.onChangeNetworkClick.receiveAsFlow()
                .onEach {
                    middleNavigationJob?.cancel()
                    middleNavigationJob = changeNetworkNavigationFlow()
                        .launchIn(this)
                    val route = routeToNetworkSelector(selectedPortfolio.first())
                    navigation.pushNew(route)
                }
                .launchIn(this)
            // handle actions from AddToken screen
            callbackDelegate.onChangePortfolioClick.receiveAsFlow()
                .onEach {
                    middleNavigationJob?.cancel()
                    middleNavigationJob = changePortfolioNavigationFlow(data).launchIn(this)
                    logAccountSelector(isAccountMode)
                    navigation.pushNew(AddToPortfolioRoutes.PortfolioSelector)
                }
                .launchIn(this)

            // suspend until token is added
            val addedToken = callbackDelegate.onTokenAdded.receiveAsFlow().first()
            middleNavigationJob?.cancel()
            val selectedPortfolio = selectedPortfolio.first()
            analyticsEventHandler.send(eventBuilder.tokenAdded(addedToken.currency.network.name))
            if (!selectedPortfolio.account.account.account.isMainAccount) {
                analyticsEventHandler.send(eventBuilder.addToNotMainAccount())
            }
            val result = AddToPortfolioManager.Result(
                wallet = selectedPortfolio.userWallet,
                account = selectedPortfolio.account.account,
                addedCurrency = addedToken,
            )

            messageSender.send(ToastMessage(message = resourceReference(R.string.markets_token_added)))

            if (addToPortfolioManager.settings.shouldSkipTokenActionsScreen) {
                finishSuccessFlow(result)
            } else {
                setupTokenActionsFlow(selectedPortfolio, addedToken)
                    .onEach { cryptoCurrencyData ->
                        tokenActionsData.emit(cryptoCurrencyData)
                        navigation.replaceAll(AddToPortfolioRoutes.TokenActions)
                    }
                    .onEmpty { finishSuccessFlow(result) }
                    .launchIn(this)
            }

            callbackDelegate.onLaterClick.receiveAsFlow().first()
            analyticsEventHandler.send(eventBuilder.getTokenLater())
            finishSuccessFlow(result)
        }
            .catch { throwable ->
                TangemLogger.e("Error", throwable)
                addToPortfolioManager.onDismiss()
            }
            .launchIn(modelScope)
    }

    @Suppress("LongMethod")
    private fun startRedesignAddToPortfolioFlow() {
        channelFlow<Unit> {
            fun finishSuccessFlow(result: AddToPortfolioManager.Result) {
                addToPortfolioManager.onSuccessAdded(result)
                channel.close()
            }

            fun finishDismissFlow() {
                addToPortfolioManager.onDismiss()
                channel.close()
            }

            val paramsSnapshot = addToPortfolioManager.paramsFlow.first()
            val tokenMarketParams = paramsSnapshot.token

            val launchMode = addToPortfolioManager.settings.launchMode
            val initialData = featureData
                .filterIsInstance<AddToPortfolioManager.State.Ready>()
                .map { it.availableToAddData }
                .first()

            if (launchMode is AddToPortfolioManager.LaunchMode.ViaUserPortfolio &&
                initialData.hasAnyAddedCurrency(launchMode.rawCurrencyId)
            ) {
                // suspend, must prepare UM before navigate to UserPortfolio
                userPortfolioStateController.updateAndWaitNotNullState(initialData, launchMode.rawCurrencyId)
                navigation.replaceAll(AddToPortfolioRoutes.UserPortfolio)
                callbackDelegate.onContinueFromUserPortfolio.receiveAsFlow().first()
            }
            val selection = selectionResolver.resolve(
                availableToAddData = initialData,
                orderedNetworks = paramsSnapshot.networks,
                selectedWallet = globalSelectedWallet,
                tokenParams = tokenMarketParams,
            ) ?: run {
                finishDismissFlow()
                return@channelFlow
            }

            val isAccountMode = portfolioSelectorController.isAccountModeSync()
            portfolioSelectorController.selectAccount(selection.account.account.accountId)

            val firstSelectedPortfolio = SelectedPortfolio(
                isAccountMode = isAccountMode,
                userWallet = selection.userWallet,
                account = selection.account,
                isAvailableMorePortfolio = !initialData.isSinglePortfolio,
            )
            val firstSelectedNetwork = selection.toSelectedNetwork() ?: run {
                finishDismissFlow()
                return@channelFlow
            }

            selectedPortfolio.emit(firstSelectedPortfolio)
            selectedNetwork.emit(firstSelectedNetwork)

            val selectedNetworkName = firstSelectedNetwork.cryptoCurrency.network.name
            analyticsEventHandler.send(event = eventBuilder.popupToConfirm(selectedNetworkName))
            navigation.replaceAll(AddToPortfolioRoutes.AddToken)

            var middleNavigationJob: Job? = null
            callbackDelegate.onChangeNetworkClick.receiveAsFlow()
                .onEach {
                    middleNavigationJob?.cancel()
                    middleNavigationJob = changeNetworkNavigationFlow()
                        .launchIn(this)
                    val route = routeToNetworkSelector(selectedPortfolio.first())
                    navigation.pushNew(route)
                }
                .launchIn(this)

            callbackDelegate.onChangePortfolioClick.receiveAsFlow()
                .onEach {
                    middleNavigationJob?.cancel()
                    middleNavigationJob = changePortfolioNavigationNewFlow(
                        data = initialData,
                        orderedNetworks = paramsSnapshot.networks,
                        tokenParams = tokenMarketParams,
                    ).launchIn(this)
                    logAccountSelector(isAccountMode)
                    navigation.pushNew(AddToPortfolioRoutes.PortfolioSelector)
                }
                .launchIn(this)

            val addedToken = callbackDelegate.onTokenAdded.receiveAsFlow().first()
            middleNavigationJob?.cancel()
            val selectedPortfolioSnapshot = selectedPortfolio.first()
            val result = AddToPortfolioManager.Result(
                wallet = selectedPortfolioSnapshot.userWallet,
                account = selectedPortfolioSnapshot.account.account,
                addedCurrency = addedToken,
            )

            messageSender.send(ToastMessage(message = resourceReference(R.string.markets_token_added)))

            if (addToPortfolioManager.settings.shouldSkipTokenActionsScreen) {
                finishSuccessFlow(result)
                return@channelFlow
            }

            setupTokenActionsFlow(selectedPortfolioSnapshot, addedToken)
                .onEach { cryptoCurrencyData ->
                    tokenActionsData.emit(cryptoCurrencyData)
                    navigation.replaceAll(AddToPortfolioRoutes.TokenActions)
                }
                .onEmpty { finishSuccessFlow(result) }
                .launchIn(this)

            callbackDelegate.onLaterClick.receiveAsFlow().first()
            analyticsEventHandler.send(eventBuilder.getTokenLater())
            finishSuccessFlow(result)
        }
            .catch { throwable ->
                TangemLogger.e("Error", throwable)
                addToPortfolioManager.onDismiss()
            }
            .launchIn(modelScope)
    }

    private fun logAccountSelector(isAccountMode: Boolean) {
        if (isAccountMode) {
            analyticsEventHandler.send(eventBuilder.popupToChooseAccount())
        }
    }

    private fun changeNetworkNavigationFlow(): Flow<SelectedNetwork> {
        return setupNetworkFlow(selectedPortfolio)
            .onEach { newNetwork ->
                selectedNetwork.emit(newNetwork)
                navigation.popToFirst()
            }
    }

    private fun changePortfolioNavigationFlow(data: AvailableToAddData): Flow<Unit> = flow {
        val selectedPortfolioValue = selectedPortfolio.first()
        val selectedAccount = selectedPortfolioValue.account.account.account.accountId
        portfolioSelectorController.selectAccount(selectedAccount)
        val changedPortfolio = setupPortfolioFlow(data)
            .drop(1)
            .onEach { portfolio ->
                val isSingleAvailableNetwork = portfolio.account.isSingleNetwork
                if (isSingleAvailableNetwork) {
                    val singleNetwork = portfolio.account.availableToAddNetworks.first()
                    callbackDelegate.onNetworkSelected(singleNetwork)
                } else {
                    navigation.pushNew(routeToNetworkSelector(portfolio))
                }
            }
        val changedNetwork = setupNetworkFlow(changedPortfolio)
        combine(
            flow = changedPortfolio,
            flow2 = changedNetwork,
            transform = { newPortfolio, newNetwork ->
                selectedPortfolio.tryEmit(newPortfolio)
                selectedNetwork.tryEmit(newNetwork)
                navigation.popToFirst()
            },
        ).collect { emit(it) }
    }

    private fun changePortfolioNavigationNewFlow(
        data: AvailableToAddData,
        orderedNetworks: List<TokenMarketInfo.Network>,
        tokenParams: RawMarketToken,
    ): Flow<Unit> {
        return setupPortfolioFlow(data)
            // drop first selected portfolio or any selected before
            .drop(1)
            .map { newPortfolio ->
                val currentNetwork = selectedNetwork.first().selectedNetwork
                val availableToAddNetworks = newPortfolio.account.availableToAddNetworks
                val isSelectedNetworkAvailableForNewPortfolio = availableToAddNetworks
                    .any { it.networkId == currentNetwork.networkId }

                if (!isSelectedNetworkAvailableForNewPortfolio) {
                    val selection = selectionResolver.resolve(
                        availableToAddData = data,
                        orderedNetworks = orderedNetworks,
                        selectedWallet = globalSelectedWallet,
                        tokenParams = tokenParams,
                        accountToAdd = newPortfolio.account,
                    )
                    val newNetwork = selection?.toSelectedNetwork()
                    if (newNetwork != null) {
                        this.selectedNetwork.tryEmit(newNetwork)
                    }
                }
                selectedPortfolio.tryEmit(newPortfolio)
                navigation.popToFirst()
            }
    }

    private fun setupTokenActionsFlow(
        selectedPortfolio: SelectedPortfolio,
        addedToken: CryptoCurrencyStatus,
    ): Flow<CryptoCurrencyData> {
        val timeFlow = channelFlow {
            val timerJob = launch { delay(TOKEN_ACTIONS_DELAY) }
            getCryptoCurrencyActionsUseCase(
                currency = addedToken.currency,
                accountId = selectedPortfolio.account.account.account.accountId,
            ).onEach { state ->
                val requestedQuickActions = toQuickActions(state.states)
                when {
                    requestedQuickActions.isNotEmpty() -> {
                        timerJob.cancel()
                        send(state)
                    }
                    // wait any requestedQuickActions while timer active
                    timerJob.isActive -> Unit
                    else -> close()
                }
            }.collect()
        }
        return timeFlow.map { actionsState ->
            CryptoCurrencyData(
                userWallet = selectedPortfolio.userWallet,
                status = actionsState.cryptoCurrencyStatus,
                actions = actionsState.states,
            )
        }
    }

    private fun setupPortfolioFlow(data: AvailableToAddData): Flow<SelectedPortfolio> = combine(
        flow = portfolioSelectorController.isAccountMode,
        flow2 = portfolioSelectorController.selectedAccount,
        transform = { isAccountMode, selectedAccountId ->
            selectedAccountId ?: return@combine null
            val availableToAddWallets =
                data.availableToAddWallets[selectedAccountId.userWalletId] ?: return@combine null
            val availableToAddAccount =
                availableToAddWallets.availableToAddAccounts[selectedAccountId] ?: return@combine null
            if (!isAccountMode) analyticsEventHandler.send(eventBuilder.addToPortfolioWalletChanged())
            SelectedPortfolio(
                isAccountMode = isAccountMode,
                userWallet = availableToAddWallets.userWallet,
                account = availableToAddAccount,
                isAvailableMorePortfolio = !data.isSinglePortfolio,
            )
        },
    )
        .filterNotNull()

    private fun setupNetworkFlow(selectedPortfolioFlow: Flow<SelectedPortfolio>): Flow<SelectedNetwork> = combine(
        flow = selectedPortfolioFlow,
        flow2 = callbackDelegate.onNetworkSelected.receiveAsFlow(),
        transform = transform@{ selectedPortfolio, selectedNetwork ->
            SelectedNetwork(
                cryptoCurrency = createCryptoCurrency(
                    userWallet = selectedPortfolio.userWallet,
                    network = selectedNetwork,
                    account = selectedPortfolio.account,
                ) ?: return@transform null,
                selectedNetwork = selectedNetwork,
                isAvailableMoreNetwork = !selectedPortfolio.account.isSingleNetwork,
            )
        },
    )
        .filterNotNull()

    private suspend fun createCryptoCurrency(
        userWallet: UserWallet,
        network: TokenMarketInfo.Network,
        account: AvailableToAddAccount,
    ): CryptoCurrency? {
        val accountIndex = account.account.account.derivationIndex
        return getTokenMarketCryptoCurrency(
            userWalletId = userWallet.walletId,
            tokenMarketParams = addToPortfolioManager.token(),
            network = network,
            accountIndex = accountIndex,
        )
    }

    private fun routeToNetworkSelector(portfolio: SelectedPortfolio): AddToPortfolioRoutes.NetworkSelector {
        return AddToPortfolioRoutes.NetworkSelector(selectedPortfolio = portfolio)
    }

    private fun combineFeatureData() = addToPortfolioManager.state.onEach { state ->
        when (state) {
            is AddToPortfolioManager.State.Ready ->
                portfolioSelectorController.isEnabled.value = isEnabled@{ userWallet, accountStatus ->
                    val availableWallet = state.availableToAddData.availableToAddWallets[userWallet.walletId]
                        ?: return@isEnabled false
                    val isAvailableAccount =
                        availableWallet.availableToAddAccounts[accountStatus.account.accountId]
                            ?.isAvailableToAdd == true
                    return@isEnabled isAvailableAccount
                }
            AddToPortfolioManager.State.Loading,
            -> Unit
        }
    }

    private suspend fun AddToPortfolioInitialSelectionResolver.InitialSelection.toSelectedNetwork(): SelectedNetwork? {
        val crypto = createCryptoCurrency(
            userWallet = userWallet,
            network = network,
            account = account,
        ) ?: return null
        return SelectedNetwork(
            cryptoCurrency = crypto,
            selectedNetwork = network,
            isAvailableMoreNetwork = !account.isSingleNetwork,
        )
    }
}

@ModelScoped
internal class AddToPortfolioCallbackDelegate @Inject constructor() :
    ChooseNetworkComponent.Callbacks,
    TokenActionsComponent.Callbacks,
    AddTokenComponent.Callbacks,
    UserPortfolioComponent.Callbacks {

    val onNetworkSelected = Channel<TokenMarketInfo.Network>()
    val onLaterClick = Channel<Unit>()
    val onChangeNetworkClick = Channel<Unit>()
    val onChangePortfolioClick = Channel<Unit>()
    val onTokenAdded = Channel<CryptoCurrencyStatus>()
    val onContinueFromUserPortfolio = Channel<Unit>()

    override fun onNetworkSelected(network: TokenMarketInfo.Network) {
        onNetworkSelected.trySend(network)
    }

    override fun onLaterClick() {
        onLaterClick.trySend(Unit)
    }

    override fun onChangeNetworkClick() {
        onChangeNetworkClick.trySend(Unit)
    }

    override fun onChangePortfolioClick() {
        onChangePortfolioClick.trySend(Unit)
    }

    override fun onTokenAdded(status: CryptoCurrencyStatus) {
        onTokenAdded.trySend(status)
    }

    override fun onContinueFromUserPortfolio() {
        onContinueFromUserPortfolio.trySend(Unit)
    }
}

private fun AvailableToAddData.hasAnyAddedCurrency(rawCurrencyId: CryptoCurrency.RawID): Boolean {
    return availableToAddWallets.values.any { wallet ->
        wallet.accounts.filterCryptoPortfolio().any { accountStatus ->
            accountStatus.tokenList.flattenCurrencies().any { status ->
                val id = status.currency.id.rawCurrencyId ?: return@any false
                getTokenIdIfL2Network(id.value) == rawCurrencyId.value
            }
        }
    }
}