package com.tangem.features.nft.collections.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.nft.FetchNFTCollectionAssetsUseCase
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.nft.RefreshAllNFTUseCase
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.features.nft.collections.NFTCollectionsComponent
import com.tangem.features.nft.collections.entity.NFTCollectionsStateUM
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.collections.entity.transformer.*
import com.tangem.features.nft.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class NFTCollectionsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val searchManager: InputManager,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val fetchNFTCollectionAssetsUseCase: FetchNFTCollectionAssetsUseCase,
    private val refreshAllNFTUseCase: RefreshAllNFTUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: NFTCollectionsComponent.Params = paramsContainer.require()

    private val _state = MutableStateFlow(
        value = NFTCollectionsStateUM(
            onBackClick = params.onBackClick,
            content = NFTCollectionsUM.Loading(
                search = SearchBarUM(
                    placeholderText = resourceReference(R.string.common_search),
                    query = "",
                    isActive = false,
                    onQueryChange = { },
                    onActiveChange = { },
                ),
                onReceiveClick = params.onReceiveClick,
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = { onRefresh() },
            ),
        ),
    )
    val state: StateFlow<NFTCollectionsStateUM> get() = _state

    private val collectionIdProvider: NFTCollection.() -> String = {
        "${network.name}_${network.derivationPath.value}_$id"
    }

    init {
        subscribeToNFTCollections()
    }

    private fun subscribeToNFTCollections() {
        combine(
            flow = getNFTCollectionsUseCase(params.userWalletId),
            flow2 = searchManager.query.distinctUntilChanged(),
        ) { nftCollections, query ->
            _state.update {
                UpdateDataStateTransformer(
                    nftCollections = nftCollections,
                    searchQuery = query,
                    onReceiveClick = {
                        params.onReceiveClick()
                    },
                    onRetryClick = ::onRefresh,
                    onExpandCollectionClick = ::onExpandCollectionClick,
                    onRetryAssetsClick = ::onRetryAssetsClick,
                    onAssetClick = { asset, collectionName ->
                        params.onAssetClick(asset, collectionName)
                    },
                    initialSearchBarFactory = ::getInitialSearchBar,
                    collectionIdProvider = collectionIdProvider,
                ).transform(it)
            }
        }
            .onStart { onRefresh() }
            .launchIn(modelScope)
    }

    private fun onRefresh() {
        modelScope.launch {
            _state.update { ChangeRefreshingStateTransformer(true).transform(it) }
            try {
                refreshAllNFTUseCase(params.userWalletId)
                    .onLeft { Timber.e(it) }
            } finally {
                _state.update { ChangeRefreshingStateTransformer(false).transform(it) }
            }
        }
    }

    private fun onSearchQueryChange(newQuery: String) {
        modelScope.launch {
            _state.update { UpdateSearchQueryTransformer(newQuery).transform(it) }

            searchManager.update(newQuery)
        }
    }

    private fun getInitialSearchBar(): SearchBarUM = SearchBarUM(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        isActive = false,
        onQueryChange = ::onSearchQueryChange,
        onActiveChange = ::toggleSearchBar,
    )

    private fun toggleSearchBar(isActive: Boolean) {
        _state.update {
            ToggleSearchBarTransformer(isActive).transform(it)
        }
    }

    private fun onExpandCollectionClick(collection: NFTCollection) {
        _state.update {
            ChangeCollectionExpandedStateTransformer(
                collection = collection,
                collectionIdProvider = collectionIdProvider,
                onFirstExpanded = { onFirstExpanded(collection) },
            ).transform(it)
        }
    }

    private fun onFirstExpanded(collection: NFTCollection) {
        loadCollectionAssets(collection)
    }

    private fun onRetryAssetsClick(collection: NFTCollection) {
        loadCollectionAssets(collection)
    }

    private fun loadCollectionAssets(collection: NFTCollection) {
        modelScope.launch {
            fetchNFTCollectionAssetsUseCase(
                userWalletId = params.userWalletId,
                network = collection.network,
                collectionId = collection.id,
            )
        }
    }
}