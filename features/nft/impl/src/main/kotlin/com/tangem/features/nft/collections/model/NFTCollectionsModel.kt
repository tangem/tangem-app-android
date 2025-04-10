package com.tangem.features.nft.collections.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.nft.FetchNFTCollectionAssetsUseCase
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.features.nft.collections.entity.NFTCollectionsStateUM
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.collections.entity.transformer.ChangeCollectionExpandedStateTransformer
import com.tangem.features.nft.collections.entity.transformer.ToggleSearchBarTransformer
import com.tangem.features.nft.collections.entity.transformer.UpdateDataStateTransformer
import com.tangem.features.nft.collections.entity.transformer.UpdateSearchQueryTransformer
import com.tangem.features.nft.component.NFTCollectionsComponent
import com.tangem.features.nft.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class NFTCollectionsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val searchManager: InputManager,
    private val getNFTCollectionsUseCase: GetNFTCollectionsUseCase,
    private val fetchNFTCollectionAssetsUseCase: FetchNFTCollectionAssetsUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    val state: StateFlow<NFTCollectionsStateUM> get() = _state

    private val _state = MutableStateFlow(
        value = NFTCollectionsStateUM(
            onBackClick = ::navigateBack,
            content = NFTCollectionsUM.Loading(::onReceiveClick),
        ),
    )

    private val params: NFTCollectionsComponent.Params = paramsContainer.require()

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
                    onReceiveClick = ::onReceiveClick,
                    onRetryClick = ::onRetryClick,
                    onExpandCollectionClick = ::onExpandCollectionClick,
                    onRetryAssetsClick = ::onRetryAssetsClick,
                    onAssetClick = ::onAssetClick,
                    initialSearchBarFactory = ::getInitialSearchBar,
                ).transform(it)
            }
        }
            .launchIn(modelScope)
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
                collectionId = collection.id,
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

    private fun onRetryClick() {
        // TODO refresh all
    }

    private fun onAssetClick(asset: NFTAsset, collectionName: String) {
        router.push(
            AppRoute.NFTDetails(
                userWalletId = params.userWalletId,
                nftAsset = asset,
                collectionName = collectionName,
            ),
        )
    }

    private fun onReceiveClick() {
        router.push(
            AppRoute.NFTReceive(
                userWalletId = params.userWalletId,
            ),
        )
    }

    private fun navigateBack() {
        router.pop()
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