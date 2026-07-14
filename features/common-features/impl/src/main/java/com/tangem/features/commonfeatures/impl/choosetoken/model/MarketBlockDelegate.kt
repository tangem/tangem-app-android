package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.card.common.extensions.hotWalletExcludedBlockchains
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.impl.choosetoken.AddToPortfolioRoute
import com.tangem.features.commonfeatures.impl.choosetoken.market.MarketsListBatchFlowManager
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketCategoriesUM
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketCategory
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

private const val MARKET_PULSE_ITEM_LIMIT = 5

@Suppress("LongParameterList")
internal class MarketBlockDelegate @AssistedInject constructor(
    private val marketsListBatchFlowManagerFactory: MarketsListBatchFlowManager.Factory,
    private val excludedBlockchains: ExcludedBlockchains,
    private val getUserWalletsUseCase: GetWalletsUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val searchQueryState: StateFlow<SearchQuery>,
    @Assisted private val selectedWalletFlow: SharedFlow<UserWallet>,
    @Assisted private val shouldShowSingleCurrencyWallets: Boolean,
    @Assisted private val addToPortfolioManager: AddToPortfolioManager,
    @Assisted private val addToPortfolioSlot: SlotNavigation<AddToPortfolioRoute>,
) {

    private val visibleMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())
    private val visibleDefaultMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    private val selectedCategoryFlow = MutableStateFlow(SwapMarketCategory.MarketCap)

    private val baseMarketsStateFlow: Flow<SwapMarketState> = searchQueryState
        // Switch between default and search market flows
        .map { it.value.isEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { isDefaultMode ->
            if (isDefaultMode) {
                visibleMarketItemIds.value = emptyList()
                createDefaultMarketsFlow()
            } else {
                visibleDefaultMarketItemIds.value = emptyList()
                createSearchMarketsFlow()
            }
        }

    /**
     * Market block constrained by the currently selected wallet:
     * - single-currency wallet: hidden entirely (`null`) - no market tokens can be added;
     * - single-currency-with-token wallet (e.g. NODL): items filtered to the wallet's network,
     *   block hidden when nothing remains;
     * - multi-currency wallet: shown as is.
     *
     * When single-currency wallets aren't selectable here (e.g. swap), the wallet is always
     * multi-currency, so we skip the per-wallet logic entirely and return [baseMarketsStateFlow].
     */
    private val walletAwareMarketsStateFlow: Flow<SwapMarketState?> = if (!shouldShowSingleCurrencyWallets) {
        baseMarketsStateFlow
    } else {
        selectedWalletFlow
            .flatMapLatest(::marketsFlowForWallet)
            .distinctUntilChanged()
    }

    val marketsStateFlow: Flow<SwapMarketState?> = walletAwareMarketsStateFlow
        .map { it.limitMarketPulseItems() }
        .distinctUntilChanged()

    private val defaultMarketsListManager by lazy {
        marketsListBatchFlowManagerFactory.create(
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
            currentOrder = Provider { selectedCategoryFlow.value.order },
            currentSearchText = Provider { null },
            modelScope = modelScope,
        )
    }

    private val searchMarketsListManager by lazy {
        marketsListBatchFlowManagerFactory.create(
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
            currentOrder = Provider { TokenMarketListConfig.Order.ByRating },
            currentSearchText = Provider { searchQueryState.value.value },
            modelScope = modelScope,
        )
    }

    init {
        // Reload search markets when query changes
        searchQueryState
            .onEach { searchQuery ->
                if (searchQuery.isSearchingState) {
                    searchMarketsListManager.reload(searchQuery.value)
                }
            }
            .launchIn(modelScope)

        visibleMarketItemIds
            .mapNotNull { rawIDS ->
                if (rawIDS.isNotEmpty()) {
                    searchMarketsListManager.getBatchKeysByItemIds(rawIDS)
                } else {
                    null
                }
            }
            .distinctUntilChanged()
            .transformLatest<Set<Int>, Unit> { visibleBatchKeys ->
                searchMarketsListManager.loadCharts(visibleBatchKeys)
            }
            .launchIn(modelScope)

        visibleDefaultMarketItemIds
            .mapNotNull { rawIds ->
                if (rawIds.isNotEmpty()) {
                    defaultMarketsListManager.getBatchKeysByItemIds(rawIds)
                } else {
                    null
                }
            }.distinctUntilChanged()
            .transformLatest<Set<Int>, Unit> { visibleBatchKeys ->
                defaultMarketsListManager.loadCharts(visibleBatchKeys)
            }
            .launchIn(modelScope)
    }

    /**
     * Starts the initial load of the default markets list. Not invoked in [init] on purpose:
     * the caller decides *if* (market block may be disabled entirely, e.g. Transfer flow) and
     * *when* (deferred past the bottom sheet entrance animation, [REDACTED_TASK_KEY]) to trigger it.
     */
    fun loadDefaultMarkets() {
        defaultMarketsListManager.reload()
    }

    private fun createDefaultMarketsFlow(): Flow<SwapMarketState> {
        val marketsTitle = TextReference.Res(R.string.markets_pulse_common_title)
        return combine(
            flow = defaultMarketsListManager.uiItems,
            flow2 = defaultMarketsListManager.isInInitialLoadingErrorState,
            flow3 = selectedCategoryFlow,
        ) { uiItems, isError, selectedCategory ->
            val categories = buildCategoriesUM(selectedCategory)
            when {
                isError -> SwapMarketState.LoadingError(
                    onRetryClicked = { defaultMarketsListManager.reload() },
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                    categories = categories,
                )
                uiItems.isEmpty() -> SwapMarketState.Loading(
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                    categories = categories,
                )
                else -> SwapMarketState.Content(
                    items = uiItems,
                    loadMore = {},
                    onItemClick = { item -> addToPortfolioItem(item) },
                    visibleIdsChanged = { visibleDefaultMarketItemIds.value = it },
                    total = uiItems.size,
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                    categories = categories,
                )
            }
        }
    }

    private fun buildCategoriesUM(selected: SwapMarketCategory): SwapMarketCategoriesUM = SwapMarketCategoriesUM(
        items = SwapMarketCategory.entries.toImmutableList(),
        selected = selected,
        onCategoryClick = ::onCategorySelected,
    )

    private fun onCategorySelected(category: SwapMarketCategory) {
        if (selectedCategoryFlow.value == category) return
        selectedCategoryFlow.value = category
        defaultMarketsListManager.reload()
    }

    private fun createSearchMarketsFlow(): Flow<SwapMarketState> {
        val marketsTitle = TextReference.Res(R.string.markets_common_title)
        return combine(
            flow = searchMarketsListManager.uiItems,
            flow2 = searchMarketsListManager.isInInitialLoadingErrorState,
            flow3 = searchMarketsListManager.isSearchNotFoundState,
            flow4 = searchMarketsListManager.totalCount,
        ) { uiItems, isError, isSearchNotFound, total ->
            when {
                isError -> SwapMarketState.LoadingError(
                    onRetryClicked = { searchMarketsListManager.reload(searchQueryState.value.value) },
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = true,
                )
                isSearchNotFound -> SwapMarketState.SearchNothingFound
                uiItems.isEmpty() -> SwapMarketState.SearchLoading
                else -> SwapMarketState.Content(
                    items = uiItems,
                    loadMore = { searchMarketsListManager.loadMore() },
                    onItemClick = { item -> addToPortfolioItem(item) },
                    visibleIdsChanged = { visibleMarketItemIds.value = it },
                    total = total ?: uiItems.size,
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = true,
                )
            }
        }
    }

    private fun marketsFlowForWallet(wallet: UserWallet): Flow<SwapMarketState?> {
        if (wallet !is UserWallet.Cold) return baseMarketsStateFlow
        val resolver = wallet.scanResponse.cardTypesResolver
        return when {
            // Single-currency wallet can't hold market tokens - hide the whole block.
            resolver.isSingleWallet() -> flowOf(null)
            // Single-currency-with-token wallet (NODL) - keep only tokens available on the wallet's network(s).
            resolver.isSingleWalletWithToken() -> combine(
                baseMarketsStateFlow,
                singleAccountStatusListSupplier(wallet.walletId),
            ) { state, accountStatusList ->
                filterStateByNetwork(state, accountStatusList.allowedNetworkIds())
            }
            // Multi-currency wallet - the common case, no filtering needed.
            else -> baseMarketsStateFlow
        }
    }

    private fun AccountStatusList.allowedNetworkIds(): Set<String> =
        flattenCurrencies().mapTo(hashSetOf()) { it.currency.network.rawId }

    private fun filterStateByNetwork(state: SwapMarketState, allowedNetworkIds: Set<String>): SwapMarketState? {
        if (state !is SwapMarketState.Content) return state
        if (allowedNetworkIds.isEmpty()) return null

        val filteredItems = state.items.filter { item ->
            val tokenMarket = defaultMarketsListManager.getTokenMarketById(item.id)
                ?: searchMarketsListManager.getTokenMarketById(item.id)
            tokenMarket?.networks?.any { allowedNetworkIds.contains(it.networkId) } == true
        }.toImmutableList()

        return if (filteredItems.isEmpty()) {
            null
        } else {
            state.copy(items = filteredItems, total = filteredItems.size)
        }
    }

    private fun SwapMarketState?.limitMarketPulseItems(): SwapMarketState? {
        if (this !is SwapMarketState.Content || shouldAssetsCount) return this
        if (items.size <= MARKET_PULSE_ITEM_LIMIT) return this
        val limitedItems = items.take(MARKET_PULSE_ITEM_LIMIT).toImmutableList()
        return copy(items = limitedItems, total = limitedItems.size)
    }

    private fun addToPortfolioItem(item: MarketsListItemUM) {
        val tokenMarket = defaultMarketsListManager.getTokenMarketById(item.id)
            ?: searchMarketsListManager.getTokenMarketById(item.id) ?: return

        val param = tokenMarket.toSerializableParam()
        val hasOnlyHotWallets = getUserWalletsUseCase.invokeSync().all { it is UserWallet.Hot }

        val networks = tokenMarket.networks?.filter { network ->
            BlockchainUtils.isSupportedNetworkId(
                networkId = network.networkId,
                coinId = tokenMarket.id.value,
                contractAddress = network.contractAddress,
                excludedBlockchains = excludedBlockchains,
                hotExcludedBlockchains = hotWalletExcludedBlockchains,
                hasOnlyHotWallets = hasOnlyHotWallets,
            )
        }?.map { network ->
            TokenMarketInfo.Network(
                networkId = network.networkId,
                isExchangeable = false,
                contractAddress = network.contractAddress,
                decimalCount = network.decimalCount,
            )
        }.orEmpty()

        addToPortfolioManager.setTokenNetworks(networks)
        addToPortfolioManager.setTokenParams(param)

        addToPortfolioSlot.activate(AddToPortfolioRoute)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            searchQueryState: StateFlow<SearchQuery>,
            modelScope: CoroutineScope,
            selectedWalletFlow: SharedFlow<UserWallet>,
            shouldShowSingleCurrencyWallets: Boolean,
            addToPortfolioManager: AddToPortfolioManager,
            addToPortfolioSlot: SlotNavigation<AddToPortfolioRoute>,
        ): MarketBlockDelegate
    }
}