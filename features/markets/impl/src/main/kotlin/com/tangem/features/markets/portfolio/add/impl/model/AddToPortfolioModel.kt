package com.tangem.features.markets.portfolio.add.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.add.api.*
import com.tangem.features.markets.portfolio.add.impl.AddTokenComponent
import com.tangem.features.markets.portfolio.add.impl.ChooseNetworkComponent
import com.tangem.features.markets.portfolio.add.impl.TokenActionsComponent
import com.tangem.features.markets.portfolio.impl.analytics.PortfolioAnalyticsEvent
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.model.PortfolioTokenUMConverter.Companion.toQuickActions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val TOKEN_ACTIONS_DELAY = 500L

@ModelScoped
@Suppress("LongParameterList")
internal class AddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val callbackDelegate: AddToPortfolioCallbackDelegate,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val messageSender: UiMessageSender,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model(),
    ChooseNetworkComponent.Callbacks by callbackDelegate,
    TokenActionsComponent.Callbacks by callbackDelegate,
    AddTokenComponent.Callbacks by callbackDelegate {

    private val params = paramsContainer.require<AddToPortfolioComponent.Params>()
    val navigation = StackNavigation<AddToPortfolioRoutes>()
    var currentStack = listOf<AddToPortfolioRoutes>(AddToPortfolioRoutes.Empty)

    /* Flows that hold state and provide it to child models */
    val selectedNetwork: MutableSharedFlow<SelectedNetwork> = replayMutableSharedFlow()
    val selectedPortfolio: MutableSharedFlow<SelectedPortfolio> = replayMutableSharedFlow()
    val tokenActionsData: MutableSharedFlow<PortfolioData.CryptoCurrencyData> = replayMutableSharedFlow()

    private val addToPortfolioManager = params.addToPortfolioManager
    val portfolioFetcher = addToPortfolioManager.portfolioFetcher
    val eventBuilder = PortfolioAnalyticsEvent.EventBuilder(
        token = addToPortfolioManager.token,
        source = addToPortfolioManager.analyticsParams?.source,
    )

    val featureData: Flow<AddToPortfolioManager.State> = combineFeatureData()

    init {
        navigation.subscribe { currentStack = it.transformer.invoke(currentStack) }
        startAddToPortfolioFlow()
    }

    private fun <T> replayMutableSharedFlow() = MutableSharedFlow<T>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @Suppress("LongMethod")
    private fun startAddToPortfolioFlow() {
        channelFlow<Unit> {
            fun finishFlow() {
                params.callback.onDismiss()
                channel.close()
            }
            val featureDataFlow: StateFlow<AvailableToAddData> = featureData
                .filterIsInstance<AddToPortfolioManager.State.AvailableToAdd>()
                .map { it.availableToAddData }
                .distinctUntilChanged()
                .stateIn(this)

            // use snapshot data, looks like we don’t need to remap at runtime
            val data = featureDataFlow.value

            // you must control it via [AddToPortfolioManager.state]
            if (!data.availableToAdd) {
                finishFlow()
                return@channelFlow
            }

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
            allRequireForAdd.first()
            // line of navigation to AddToken screen is finished; cancel the job, select a new root screen
            firstPartOfNavigation.cancel()
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
                    navigation.pushNew(AddToPortfolioRoutes.PortfolioSelector)
                }
                .launchIn(this)

            // suspend until token is added
            val addedToken = callbackDelegate.onTokenAdded.receiveAsFlow().first()
            middleNavigationJob?.cancel()
            val selectedPortfolio = selectedPortfolio.first()

            messageSender.send(ToastMessage(message = resourceReference(R.string.markets_token_added)))

            setupTokenActionsFlow(selectedPortfolio, addedToken)
                .onEach {
                    tokenActionsData.emit(it)
                    navigation.replaceAll(AddToPortfolioRoutes.TokenActions)
                }
                .onEmpty { finishFlow() }
                .launchIn(this)

            callbackDelegate.onLaterClick.receiveAsFlow().first()
            finishFlow()
        }
            .catch {
                Timber.e(it)
                params.callback.onDismiss()
            }
            .launchIn(modelScope)
    }

    private fun changeNetworkNavigationFlow(): Flow<SelectedNetwork> {
        return setupNetworkFlow(selectedPortfolio)
            .onEach { newNetwork ->
                selectedNetwork.emit(newNetwork)
                navigation.popToFirst()
            }
    }

    private suspend fun changePortfolioNavigationFlow(data: AvailableToAddData): Flow<Unit> {
        val selectedAccount = selectedPortfolio.first().account.account.account.accountId
        portfolioSelectorController.selectAccount(selectedAccount)
        val changedPortfolio = setupPortfolioFlow(data)
            .drop(1)
            .onEach { portfolio -> navigation.pushNew(routeToNetworkSelector(portfolio)) }
        val changedNetwork = setupNetworkFlow(changedPortfolio)
        return combine(
            flow = changedPortfolio,
            flow2 = changedNetwork,
            transform = { newPortfolio, newNetwork ->
                selectedPortfolio.tryEmit(newPortfolio)
                selectedNetwork.tryEmit(newNetwork)
                navigation.popToFirst()
            },
        )
    }

    private fun setupTokenActionsFlow(
        selectedPortfolio: SelectedPortfolio,
        addedToken: CryptoCurrencyStatus,
    ): Flow<PortfolioData.CryptoCurrencyData> {
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
            PortfolioData.CryptoCurrencyData(
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
            SelectedPortfolio(
                isAccountMode = isAccountMode,
                userWallet = availableToAddWallets.userWallet,
                account = availableToAddAccount,
                availableMorePortfolio = !data.isSinglePortfolio,
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
                availableMoreNetwork = !selectedPortfolio.account.isSingleNetwork,
            )
        },
    )
        .filterNotNull()

    private suspend fun createCryptoCurrency(
        userWallet: UserWallet,
        network: TokenMarketInfo.Network,
        account: AvailableToAddAccount,
    ): CryptoCurrency? {
        val accountIndex = when (account.account) {
            is AccountStatus.CryptoPortfolio -> account.account.account.derivationIndex
        }
        return getTokenMarketCryptoCurrency(
            userWalletId = userWallet.walletId,
            tokenMarketParams = addToPortfolioManager.token,
            network = network,
            accountIndex = accountIndex,
        )
    }

    private fun routeToNetworkSelector(portfolio: SelectedPortfolio): AddToPortfolioRoutes.NetworkSelector {
        return AddToPortfolioRoutes.NetworkSelector(selectedPortfolio = portfolio)
    }

    private fun combineFeatureData() = addToPortfolioManager.state.onEach { state ->
        when (state) {
            is AddToPortfolioManager.State.AvailableToAdd ->
                portfolioSelectorController.isEnabled.value = isEnabled@{ userWallet, accountStatus ->
                    val availableWallet = state.availableToAddData.availableToAddWallets[userWallet.walletId]
                        ?: return@isEnabled false
                    val availableAccount =
                        availableWallet.availableToAddAccounts[accountStatus.account.accountId]
                    return@isEnabled availableAccount != null
                }
            AddToPortfolioManager.State.Init,
            AddToPortfolioManager.State.NothingToAdd,
            -> Unit
        }
    }
}

@ModelScoped
internal class AddToPortfolioCallbackDelegate @Inject constructor() :
    ChooseNetworkComponent.Callbacks,
    TokenActionsComponent.Callbacks,
    AddTokenComponent.Callbacks {

    val onNetworkSelected = Channel<TokenMarketInfo.Network>()
    val onLaterClick = Channel<Unit>()
    val onChangeNetworkClick = Channel<Unit>()
    val onChangePortfolioClick = Channel<Unit>()
    val onTokenAdded = Channel<CryptoCurrencyStatus>()

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
}