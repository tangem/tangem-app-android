package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.QuickActionsConverter.toQuickActions
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
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
    override val dispatchers: CoroutineDispatcherProvider,
    val portfolioSelectorController: PortfolioSelectorController,
    private val designFeatureToggles: DesignFeatureToggles,
    private val callbackDelegate: AddToPortfolioCallbackDelegate,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val selectionResolver: AddToPortfolioInitialSelectionResolver,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
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

    val paramsSnapshot: AddToPortfolioManager.Params by lazy {
        addToPortfolioManager.paramsFlow.replayCache.first()
    }
    val eventBuilder: PortfolioAnalyticsEvent.EventBuilder by lazy {
        val tokenMarketParams = paramsSnapshot.token
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

    private val globalSelectedWallet: UserWallet?
        get() = getSelectedWalletSyncUseCase().getOrNull()
            .takeIf { it?.isMultiCurrency == true }

    init {
        navigation.subscribe { currentStack = it.transformer.invoke(currentStack) }
        startRedesignAddToPortfolioFlow()
    }

    override fun onQuickActionClick(action: TokenActionsBSContentUM.Action) {
        analyticsEventHandler.send(eventBuilder.getTokenActionClick(actionUM = action))
    }

    private fun <T> replayMutableSharedFlow() = MutableSharedFlow<T>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private fun lineNavigationFlowToAddTokenScreen(
        isAccountMode: Boolean,
        data: AvailableToAddData,
        firstSelectedPortfolioFlow: Flow<SelectedPortfolio>,
    ): Flow<SelectedPortfolio> {
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

        return firstSelectedPortfolioFlow.onEach { portfolio ->
            val isAvailableToAdd = portfolio.account.isAvailableToAdd
            val isSingleAvailableNetwork = portfolio.account.isSingleNetwork
            when {
                // force select an added network, not allowed to add
                // but its call AddToPortfolioManager.onAddedTokenClick callback
                !isAvailableToAdd -> {
                    val singleNetwork = portfolio.account.addedMarketNetworks.first()
                    callbackDelegate.onNetworkSelected(singleNetwork)
                }
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
    }

    @Suppress("LongMethod")
    private fun startRedesignAddToPortfolioFlow() {
        channelFlow<Unit> {
            fun finishSuccessFlow(result: AddToPortfolioManager.Result) {
                addToPortfolioManager.onSuccessAdded(result)
                channel.close()
            }

            fun finishOnAddedTokenClick(result: AddToPortfolioManager.Result) {
                addToPortfolioManager.onAddedTokenClick(result)
                channel.close()
            }

            fun finishDismissFlow() {
                addToPortfolioManager.onDismiss()
                channel.close()
            }

            val tokenMarketParams = paramsSnapshot.token

            val launchMode = paramsSnapshot.launchMode
            val isAccountMode = portfolioSelectorController.isAccountModeSync()
            val initialData: AvailableToAddData = addToPortfolioManager.state
                .filterIsInstance<AddToPortfolioManager.State.Ready>()
                .map { it.availableToAddData }
                .first()

            setupPortfolioSelector(initialData, launchMode)

            val shouldShowUserPortfolio = designFeatureToggles.isRedesignEnabled &&
                launchMode is AddToPortfolioManager.LaunchMode.ViaUserPortfolio &&
                initialData.hasAnyAddedCurrency(tokenMarketParams.id)

            if (shouldShowUserPortfolio) {
                // suspend, must prepare UM before navigate to UserPortfolio
                userPortfolioStateController.updateAndWaitNotNullState(
                    allAvailableData = initialData,
                    rawCurrencyId = tokenMarketParams.id,
                )
                navigation.replaceAll(AddToPortfolioRoutes.UserPortfolio)
                callbackDelegate.onContinueFromUserPortfolio.receiveAsFlow().first()
            }

            val initialSelection: AddToPortfolioInitialSelectionResolver.InitialSelection? = when (launchMode) {
                AddToPortfolioManager.LaunchMode.Preselected -> null
                is AddToPortfolioManager.LaunchMode.ViaUserPortfolio,
                AddToPortfolioManager.LaunchMode.DirectAdd,
                -> if (designFeatureToggles.isRedesignEnabled) {
                    getInitialSelection(initialData)
                } else {
                    null
                }
            }

            val firstSelectedPortfolioFlow: Flow<SelectedPortfolio> =
                setupPortfolioFlow(initialData).onEach { selectedPortfolio.emit(it) }
            val firstSelectedNetworkFlow: Flow<SelectedNetwork> =
                setupNetworkFlow(firstSelectedPortfolioFlow).onEach { selectedNetwork.emit(it) }

            // main flow that combine all require data
            val allRequireForAdd = combine(
                flow = firstSelectedNetworkFlow,
                flow2 = firstSelectedPortfolioFlow,
                transform = { a, b -> a to b },
            )

            var firstPartOfNavigation: Job? = null

            if (initialSelection != null) {
                launch {
                    portfolioSelectorController.selectAccount(initialSelection.account.account.accountId)
                    callbackDelegate.onNetworkSelected.send(initialSelection.network)
                }
            } else {
                firstPartOfNavigation = lineNavigationFlowToAddTokenScreen(
                    isAccountMode = isAccountMode,
                    data = initialData,
                    firstSelectedPortfolioFlow = firstSelectedPortfolioFlow,
                ).launchIn(this)
            }

            // suspend until all required data is selected
            val (firstSelectedNetwork, firstSelectedPortfolio) = allRequireForAdd.first()
            // line navigation to AddToken screen is finished; cancel the job if exists
            firstPartOfNavigation?.cancel()

            val alreadyAddedToken = getAccountCurrencyStatusUseCase.invokeSync(
                userWalletId = firstSelectedPortfolio.userWallet.walletId,
                currency = firstSelectedNetwork.cryptoCurrency,
            ).getOrNull()

            if (alreadyAddedToken != null) {
                val result = AddToPortfolioManager.Result(
                    wallet = firstSelectedPortfolio.userWallet,
                    account = firstSelectedPortfolio.account.account,
                    addedCurrency = alreadyAddedToken.status,
                )
                finishOnAddedTokenClick(result)
                return@channelFlow
            }

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
                    middleNavigationJob = if (designFeatureToggles.isRedesignEnabled) {
                        changePortfolioNavigationNewFlow(
                            data = initialData,
                            orderedNetworks = paramsSnapshot.networks,
                            tokenParams = tokenMarketParams,
                        )
                    } else {
                        changePortfolioNavigationFlow(initialData)
                    }.launchIn(this)
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

            callbackDelegate.onChooseTokenBottomActionClick.receiveAsFlow().first()
            analyticsEventHandler.send(eventBuilder.getTokenLater())
            finishSuccessFlow(result)
        }
            .catch { throwable ->
                TangemLogger.e("Error", throwable)
                addToPortfolioManager.onDismiss()
            }
            .launchIn(modelScope)
    }

    private suspend fun getInitialSelection(
        initialData: AvailableToAddData,
    ): AddToPortfolioInitialSelectionResolver.InitialSelection? {
        return selectionResolver.resolve(
            availableToAddData = initialData,
            orderedNetworks = paramsSnapshot.networks,
            selectedWallet = globalSelectedWallet,
            tokenParams = paramsSnapshot.token,
        )
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
                val rebuiltSelectedNetwork = selectionResolver.resolve(
                    availableToAddData = data,
                    orderedNetworks = orderedNetworks,
                    selectedWallet = globalSelectedWallet,
                    tokenParams = tokenParams,
                    accountToAdd = newPortfolio.account,
                    preferredNetwork = selectedNetwork.first().selectedNetwork,
                )?.toSelectedNetwork()

                if (rebuiltSelectedNetwork != null) {
                    this.selectedNetwork.tryEmit(rebuiltSelectedNetwork)
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
                val requestedQuickActions = toQuickActions(state.states, designFeatureToggles.isRedesignEnabled)
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
                isAccountMode = selectedPortfolio.isAccountMode,
                account = selectedPortfolio.account.account,
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

    private fun setupPortfolioSelector(data: AvailableToAddData, launchMode: AddToPortfolioManager.LaunchMode) {
        portfolioSelectorController.isEnabled.value = isEnabled@{ userWallet, accountStatus ->
            when (launchMode) {
                AddToPortfolioManager.LaunchMode.Preselected -> return@isEnabled true
                AddToPortfolioManager.LaunchMode.DirectAdd,
                is AddToPortfolioManager.LaunchMode.ViaUserPortfolio,
                -> {
                    val availableWallet = data.availableToAddWallets[userWallet.walletId]
                        ?: return@isEnabled false
                    val isAvailableAccount = availableWallet
                        .availableToAddAccounts[accountStatus.account.accountId]
                        ?.isAvailableToAdd == true
                    return@isEnabled isAvailableAccount
                }
            }
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
    val onChooseTokenBottomActionClick = Channel<Unit>()
    val onChangeNetworkClick = Channel<Unit>()
    val onChangePortfolioClick = Channel<Unit>()
    val onTokenAdded = Channel<CryptoCurrencyStatus>()
    val onContinueFromUserPortfolio = Channel<Unit>()

    override fun onNetworkSelected(network: TokenMarketInfo.Network) {
        onNetworkSelected.trySend(network)
    }

    override fun onBottomActionClick() {
        onChooseTokenBottomActionClick.trySend(Unit)
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