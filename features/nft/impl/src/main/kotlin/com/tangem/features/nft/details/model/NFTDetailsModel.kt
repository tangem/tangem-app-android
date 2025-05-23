package com.tangem.features.nft.details.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.nft.FetchNFTCollectionAssetsUseCase
import com.tangem.domain.nft.FetchNFTPriceUseCase
import com.tangem.domain.nft.GetNFTExploreUrlUseCase
import com.tangem.domain.nft.GetNFTPriceUseCase
import com.tangem.domain.nft.analytics.NFTAnalyticsEvent
import com.tangem.features.nft.details.NFTDetailsComponent
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsBottomSheetConfig
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.details.entity.factory.NFTDetailsUMFactory
import com.tangem.features.nft.details.entity.transformer.NFTPriceChangeTransformer
import com.tangem.features.nft.details.entity.transformer.NFTPriceUpdatingTransformer
import com.tangem.features.nft.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class NFTDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val getNFTExploreUrlUseCase: GetNFTExploreUrlUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getNFTPriceUseCase: GetNFTPriceUseCase,
    private val fetchNFTCollectionAssetsUseCase: FetchNFTCollectionAssetsUseCase,
    private val fetchNFTPriceUseCase: FetchNFTPriceUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: NFTDetailsComponent.Params = paramsContainer.require()

    val state: StateFlow<NFTDetailsUM> get() = _state

    private var appCurrency: AppCurrency = AppCurrency.Default

    private val stateFactory: NFTDetailsUMFactory = NFTDetailsUMFactory(
        appCurrency = appCurrency,
        onBackClick = { params.onBackClick() },
        onReadMoreClick = ::onReadMoreClick,
        onSeeAllTraitsClick = { params.onAllTraitsClick() },
        onExploreClick = ::onExploreClick,
        onSendClick = ::onSendClick,
        onInfoBlockClick = ::onInfoBlockClick,
        onRefresh = ::onRefresh,
    )

    private val _state by lazy {
        MutableStateFlow(
            value = stateFactory.getInitialState(params.nftAsset),
        )
    }

    val bottomSheetNavigation: SlotNavigation<NFTDetailsBottomSheetConfig> = SlotNavigation()

    init {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ScreenOpened(params.nftAsset.network.name))
        initAppCurrency()
        subscribeToPriceChanges()
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun subscribeToPriceChanges() {
        _state.update(NFTPriceUpdatingTransformer)
        modelScope.launch {
            getNFTPriceUseCase(params.userWalletId, params.nftAsset)
                .fold(
                    ifLeft = {
                        Timber.w(it)
                    },
                    ifRight = { quoteFlow ->
                        quoteFlow
                            .distinctUntilChanged()
                            .onEach { salePrice ->
                                _state.update(
                                    NFTPriceChangeTransformer(
                                        appCurrency = appCurrency,
                                        nftSalePrice = salePrice,
                                    ),
                                )
                            }.launchIn(modelScope)
                    },
                )
        }
    }

    private fun onRefresh() {
        _state.update {
            it.copy(pullToRefreshConfig = it.pullToRefreshConfig.copy(isRefreshing = true))
        }
        _state.update(NFTPriceUpdatingTransformer)
        modelScope.launch {
            awaitAll(
                async {
                    fetchNFTCollectionAssetsUseCase(
                        userWalletId = params.userWalletId,
                        network = params.nftAsset.network,
                        collectionId = params.nftAsset.collectionId,
                    )
                },
                async {
                    fetchNFTPriceUseCase(
                        network = params.nftAsset.network,
                        appCurrencyId = null,
                    )
                },
            )
            _state.update {
                it.copy(pullToRefreshConfig = it.pullToRefreshConfig.copy(isRefreshing = false))
            }
        }
    }

    private fun onInfoBlockClick(title: TextReference, text: TextReference) {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ButtonReadMore)
        bottomSheetNavigation.activate(
            NFTDetailsBottomSheetConfig.Info(
                title = title,
                text = text,
            ),
        )
    }

    private fun onReadMoreClick() {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ButtonReadMore)
        when (val topInfo = _state.value.nftAsset.topInfo) {
            is NFTAssetUM.TopInfo.Empty -> Unit
            is NFTAssetUM.TopInfo.Content -> {
                bottomSheetNavigation.activate(
                    NFTDetailsBottomSheetConfig.Info(
                        title = resourceReference(R.string.nft_about_title),
                        text = stringReference(topInfo.description.orEmpty()),
                    ),
                )
            }
        }
    }

    private fun onExploreClick() {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ButtonExplore)
        modelScope.launch {
            val url = getNFTExploreUrlUseCase.invoke(
                network = params.nftAsset.network,
                assetIdentifier = params.nftAsset.id,
            )
            if (url != null) {
                urlOpener.openUrl(url)
            }
        }
    }

    private fun onSendClick() {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ButtonSend)
        router.push(
            AppRoute.NFTSend(
                userWalletId = params.userWalletId,
                nftAsset = params.nftAsset,
                nftCollectionName = params.nftCollectionName,
            ),
        )
    }
}