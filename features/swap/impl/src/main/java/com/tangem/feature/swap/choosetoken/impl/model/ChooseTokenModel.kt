package com.tangem.feature.swap.choosetoken.impl.model

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.swap.choosetoken.api.*
import com.tangem.feature.swap.choosetoken.impl.converter.SearchBarToggleTransformer
import com.tangem.feature.swap.choosetoken.impl.converter.SearchBarUpdateQueryTransformer
import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenFullUM
import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenInitialUM
import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenUM
import com.tangem.feature.swap.choosetoken.impl.ui.WalletListUM
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

internal val String.isSearchingState: Boolean get() = this.isNotBlank()
internal val StateFlow<String>.isSearchingState: Boolean get() = this.value.isSearchingState

@Suppress("LongParameterList")
@ModelScoped
internal class ChooseTokenModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val settingContextUseCase: SettingContextUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    portfolioListBlockDelegateFactory: PortfolioListBlockDelegate.Factory,
    marketBlockDelegateFactory: MarketBlockDelegate.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<ChooseTokenComponent.Params>()
    private val bridge: ChooseTokenBridge = params.bridge

    private val searchQueryState: StateFlow<String> = bridge.searchQueryState
    private val isSearchingState: Boolean get() = bridge.searchQueryState.isSearchingState
    private val marketBlockDelegate: MarketBlockDelegate = marketBlockDelegateFactory.create(
        modelScope = modelScope,
        searchQueryState = searchQueryState,
        screensSourcesName = params.analyticsPayload
            .filterIsInstance<ChooseTokenAnalyticsPayload.ScreensSources>()
            .firstOrNull()?.value.orEmpty(),
    )
    private val portfolioListBlockDelegate: PortfolioListBlockDelegate = portfolioListBlockDelegateFactory.create(
        modelScope = modelScope,
        searchQueryState = searchQueryState,
    )

    val bottomSheetNavigation get() = marketBlockDelegate.addToPortfolioSlot
    val addToPortfolioManager get() = marketBlockDelegate.addToPortfolioManager
    private val marketsStateFlow: Flow<SwapMarketState?> = if (params.settings.isShowMarketBlock) {
        marketBlockDelegate.marketsStateFlow
    } else {
        flowOf(null)
    }

    private val expandedAccountsFlow: MutableStateFlow<Map<AccountId, Boolean>> = MutableStateFlow(emptyMap())
    private val onWalletSelected = Channel<UserWalletId>()

    // todo swap call new api result
    val addToPortfolioCallback = object : AddToPortfolioComponent.Callback {
        override fun onDismiss() = marketBlockDelegate.addToPortfolioSlot.dismiss()

        override fun onSuccess(addedToken: CryptoCurrency) {
            val newToken = addedToken to ChooseTokenAnalyticsPayload.IsSearched(isSearchingState)
            bridge.onNewTokenAdded(newToken)
            marketBlockDelegate.addToPortfolioSlot.dismiss()
        }
    }

    val stateOld: StateFlow<SwapSelectTokenStateHolder?> = combineUIOld()

    private val contentState: StateFlow<ChooseTokenUM?> = combineUI()
    private val initialState: MutableStateFlow<ChooseTokenInitialUM> = MutableStateFlow(getInitState())
    val state: StateFlow<ChooseTokenFullUM> = combine(
        flow = initialState,
        flow2 = contentState,
        transform = { initial, content ->
            ChooseTokenFullUM(
                initialUM = initial,
                contentUM = content,
            )
        },
    ).stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChooseTokenFullUM(initialState.value, contentState.value),
    )

    @Suppress("UnusedPrivateMember")
    private fun combineUIOld(): StateFlow<SwapSelectTokenStateHolder?> = combine(
        flow = bridge.currenciesGroup,
        flow2 = settingContextUseCase.invoke(),
        flow3 = marketsStateFlow,
        flow4 = expandedAccountsFlow,
        transform = { currenciesGroup, settingContext, marketState, expandedAccounts ->
            val isAccountsMode = settingContext.isAccountsMode
            val appCurrency = settingContext.appCurrency
            val isBalanceHidden = settingContext.isBalanceHidden

            TokensDataConverter(
                onSearchEntered = { query -> bridge.onSearchQuery(query) },
                onTokenClick = { tokenId ->
                    val selected = tokenId to ChooseTokenAnalyticsPayload
                        .IsSearched(isSearchingState)
                    bridge.onTokenSelected(selected)
                },
                onAccountClick = { account ->
                    expandedAccountsFlow.update { expandedList ->
                        val hasSavedAccount = expandedList[account.accountId]
                        val isExpanded = hasSavedAccount == true
                        expandedList + (account.accountId to !isExpanded)
                    }
                },
                expandedAccounts = expandedAccounts,
                tokensDataState = currenciesGroup,
                isBalanceHidden = isBalanceHidden,
                isAccountsMode = isAccountsMode,
                appCurrency = appCurrency,
                marketState = requireNotNull(marketState),
            ).transform()
        },
    )
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = null)

    @Suppress("LongMethod")
    private fun combineUI(): StateFlow<ChooseTokenUM?> = channelFlow {
        val allWalletsFlow: StateFlow<LinkedHashMap<UserWalletId, UserWallet>> =
            getWalletsUseCase.invokeAsMap().stateIn(this)

        val selectedWalletFlow: StateFlow<UserWallet> =
            onWalletSelected.receiveAsFlow()
                .mapNotNull { walletId -> allWalletsFlow.value[walletId] }
                .stateIn(this, SharingStarted.Eagerly, allWalletsFlow.value.values.first())

        val selectedWalletTokensData: Flow<TokenListUMData> = combine(
            flow = selectedWalletFlow.map { wallet -> wallet.walletId }.distinctUntilChanged(),
            flow2 = portfolioListBlockDelegate.portfolioList,
            transform = { selectedWalletId, allPortfoliosData -> allPortfoliosData[selectedWalletId] },
        )
            .filterNotNull()
            .distinctUntilChanged()

        val walletListUmFlow = combine(
            flow = selectedWalletFlow,
            flow2 = allWalletsFlow,
            transform = { selectedWallet, allWallets ->
                allWallets.entries
                    .map { (walletId, wallet) ->
                        val type = if (selectedWallet.walletId == walletId) {
                            TangemButtonType.Primary
                        } else {
                            TangemButtonType.Secondary
                        }
                        TangemButtonUM(
                            text = stringReference(wallet.name),
                            onClick = { onWalletSelected.trySend(walletId) },
                            type = type,
                        )
                    }
            },
        )
            .distinctUntilChanged()

        portfolioListBlockDelegate.onTokenItemClick.receiveAsFlow()
            .onEach { (account, currencyStatus) ->
                onTokenItemClick(
                    wallet = allWalletsFlow.value[account.accountId.userWalletId] ?: return@onEach,
                    account = account,
                    currencyStatus = currencyStatus,
                )
            }
            .launchIn(this)

        combine(
            flow = selectedWalletTokensData,
            flow2 = settingContextUseCase.invoke(),
            flow3 = marketsStateFlow,
            flow4 = walletListUmFlow,
            transform = { tokensData, settings, marketsData, walletList ->
                val walletsUM = if (walletList.size != 1) {
                    WalletListUM(walletList.toPersistentList())
                } else {
                    WalletListUM(persistentListOf())
                }
                ChooseTokenUM(
                    walletList = walletsUM,
                    isBalanceHidden = settings.isBalanceHidden,
                    isSearching = isSearchingState,
                    tokensListData = tokensData,
                    marketsState = marketsData,
                )
            },
        )
            .distinctUntilChanged()
            .collect { newUM -> channel.send(newUM) }
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = null)

    private fun onTokenItemClick(wallet: UserWallet, account: AccountStatus, currencyStatus: CryptoCurrencyStatus) {
        val analyticsPayload = setOf(
            ChooseTokenAnalyticsPayload.IsSearched(isSearchingState),
        )
        val result = ChooseTokenResult(
            account = account,
            currency = currencyStatus,
            wallet = wallet,
            analyticsPayload = analyticsPayload,
        )
        bridge.onCurrencyChosen(result)
    }

    fun onBackClicked() {
        bridge.onClose()
    }

    private fun getInitialSearchBar(): SearchBarUM = SearchBarUM(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        isActive = false,
        onQueryChange = { query ->
            initialState.update { prevState -> SearchBarUpdateQueryTransformer(query).transform(prevState) }
            bridge.onSearchQuery(query)
        },
        onActiveChange = { isActive ->
            initialState.update { prevState -> SearchBarToggleTransformer(isActive).transform(prevState) }
        },
    )

    private fun getInitState() = ChooseTokenInitialUM(
        screenTitle = params.settings.title,
        onCloseClick = ::onBackClicked,
        searchBar = getInitialSearchBar(),
    )

    companion object {
        const val DEBOUNCE_SEARCH_DELAY = 500L
    }
}