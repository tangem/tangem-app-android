package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.card.common.extensions.hotWalletExcludedBlockchains
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.impl.choosetoken.AddToPortfolioRoute
import com.tangem.features.commonfeatures.impl.choosetoken.market.MarketsListBatchFlowManager
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.orEmpty

@Suppress("LongParameterList")
internal class MarketBlockDelegate @AssistedInject constructor(
    private val marketsListBatchFlowManagerFactory: MarketsListBatchFlowManager.Factory,
    private val excludedBlockchains: ExcludedBlockchains,
    private val getUserWalletsUseCase: GetWalletsUseCase,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val searchQueryState: StateFlow<SearchQuery>,
    @Assisted private val screensSourcesName: String,
) {

    private val visibleMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())
    private val visibleDefaultMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    val addToPortfolioSlot: SlotNavigation<AddToPortfolioRoute> = SlotNavigation()
    val addToPortfolioManager: AddToPortfolioManager = addToPortfolioManagerFactory.create(
        scope = modelScope,
        settings = AddToPortfolioManager.Settings.ChooseToken,
        analyticsParams = AddToPortfolioManager.AnalyticsParams(source = screensSourcesName),
    )

    val marketsStateFlow: Flow<SwapMarketState> = searchQueryState
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

    private val defaultMarketsListManager by lazy {
        marketsListBatchFlowManagerFactory.create(
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
            order = TokenMarketListConfig.Order.Trending,
            currentSearchText = Provider { null },
            modelScope = modelScope,
        )
    }

    private val searchMarketsListManager by lazy {
        marketsListBatchFlowManagerFactory.create(
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
            order = TokenMarketListConfig.Order.ByRating,
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

        // Initial load of default markets
        defaultMarketsListManager.reload()

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

    private fun createDefaultMarketsFlow(): Flow<SwapMarketState> {
        val marketsTitle = TextReference.Res(R.string.feed_trending_now)
        return combine(
            defaultMarketsListManager.uiItems,
            defaultMarketsListManager.isInInitialLoadingErrorState,
            defaultMarketsListManager.totalCount,
        ) { uiItems, isError, total ->
            when {
                isError -> SwapMarketState.LoadingError(
                    onRetryClicked = { defaultMarketsListManager.reload() },
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                )
                uiItems.isEmpty() -> SwapMarketState.DefaultLoading
                else -> SwapMarketState.Content(
                    items = uiItems,
                    loadMore = { defaultMarketsListManager.loadMore() },
                    onItemClick = { item -> addToPortfolioItem(item) },
                    visibleIdsChanged = { visibleDefaultMarketItemIds.value = it },
                    total = total ?: uiItems.size,
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                )
            }
        }
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
            screensSourcesName: String,
        ): MarketBlockDelegate
    }
}