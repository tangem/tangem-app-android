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
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

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

            // you must control it via [AddToPortfolioComponent.state]
            if (!data.availableToAdd) {
                finishFlow()
                return@channelFlow
            }

            // launch data flows, emits on user/code selection, updates state holder
            setupPortfolioFlow(data)
                .onEach { selectedPortfolio.emit(it) }
                .launchIn(this)
            setupNetworkFlow(selectedPortfolio)
                .onEach { selectedNetwork.emit(it) }
                .launchIn(this)

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

            val firstPartOfNavigation = selectedPortfolio
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
                flow = selectedNetwork,
                flow2 = selectedPortfolio,
                transform = { a, b -> a to b },
            ).shareIn(
                scope = this,
                started = SharingStarted.Eagerly,
                replay = 1,
            )

            // suspend until all required data is selected
            allRequireForAdd.first()
            // line of navigation to AddToken screen is finished; cancel the job, select a new root screen
            firstPartOfNavigation.cancel()
            navigation.replaceAll(AddToPortfolioRoutes.AddToken)

            // handle actions from AddToken screen
            callbackDelegate.onChangeNetworkClick.receiveAsFlow()
                .map { routeToNetworkSelector(selectedPortfolio.first()) }
                .onEach { route -> navigation.pushNew(route) }
                .launchIn(this)
            // handle actions from AddToken screen
            callbackDelegate.onChangePortfolioClick.receiveAsFlow()
                .onEach { navigation.pushNew(AddToPortfolioRoutes.PortfolioSelector) }
                .launchIn(this)

            allRequireForAdd
                .onEach { (network, portfolio) ->
                    // after selecting a new Portfolio, must verify previous selected Network
                    // if it’s unavailable, navigate to NetworkSelector
                    val isAvailableSelectedNetwork = portfolio.account.availableToAddNetworks
                        .any { it.networkId == network.selectedNetwork.networkId }
                    if (isAvailableSelectedNetwork) {
                        navigation.popToFirst()
                    } else {
                        navigation.pushNew(routeToNetworkSelector(portfolio))
                    }
                }
                .launchIn(this)

            // suspend until token is added
            val addedToken = callbackDelegate.onTokenAdded.receiveAsFlow().first()
            val selectedPortfolio = selectedPortfolio.first()

            messageSender.send(ToastMessage(message = resourceReference(R.string.markets_token_added)))

            setupTokenActionsFlow(selectedPortfolio, addedToken)
                .onEach { tokenActionsData.emit(it) }
                .onEach {
                    if (it.actions.isNotEmpty()) {
                        navigation.replaceAll(AddToPortfolioRoutes.TokenActions)
                    } else {
                        finishFlow()
                    }
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

    private fun setupTokenActionsFlow(
        selectedPortfolio: SelectedPortfolio,
        addedToken: CryptoCurrencyStatus,
    ): Flow<PortfolioData.CryptoCurrencyData> = getCryptoCurrencyActionsUseCase(
        currency = addedToken.currency,
        accountId = selectedPortfolio.account.account.account.accountId,
    )
        .map {
            PortfolioData.CryptoCurrencyData(
                userWallet = selectedPortfolio.userWallet,
                status = addedToken,
                actions = it.states,
            )
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
        .onEach { selectedPortfolio.emit(it) }

    private fun setupNetworkFlow(selectedPortfolioFlow: SharedFlow<SelectedPortfolio>): Flow<SelectedNetwork> = combine(
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
    ): CryptoCurrency? = getTokenMarketCryptoCurrency(
        userWalletId = userWallet.walletId,
        tokenMarketParams = addToPortfolioManager.token,
        network = network,
        accountIndex = (account.account as? AccountStatus.CryptoPortfolio)?.account?.derivationIndex,
    )

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