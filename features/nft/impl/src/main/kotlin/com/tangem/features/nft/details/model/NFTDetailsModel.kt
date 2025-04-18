package com.tangem.features.nft.details.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.nft.analytics.NFTAnalyticsEvent
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.nft.GetNFTExploreUrlUseCase
import com.tangem.features.nft.details.NFTDetailsComponent
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsBottomSheetConfig
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.details.entity.factory.NFTDetailsUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class NFTDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val getNFTExploreUrlUseCase: GetNFTExploreUrlUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: NFTDetailsComponent.Params = paramsContainer.require()

    val state: StateFlow<NFTDetailsUM> get() = _state

    private val stateFactory: NFTDetailsUMFactory = NFTDetailsUMFactory(
        onBackClick = { params.onBackClick() },
        onReadMoreClick = ::onReadMoreClick,
        onSeeAllTraitsClick = { params.onAllTraitsClick() },
        onExploreClick = ::onExploreClick,
        onSendClick = ::onSendClick,
    )

    private val _state = MutableStateFlow(
        value = stateFactory.getInitialState(params.nftAsset),
    )

    val bottomSheetNavigation: SlotNavigation<NFTDetailsBottomSheetConfig> = SlotNavigation()
    init {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ScreenOpened(params.nftAsset.network.name))
    }

    private fun onReadMoreClick() {
        analyticsEventHandler.send(NFTAnalyticsEvent.Details.ButtonReadMore)
        when (val topInfo = _state.value.nftAsset.topInfo) {
            is NFTAssetUM.TopInfo.Empty -> Unit
            is NFTAssetUM.TopInfo.Content -> {
                bottomSheetNavigation.activate(
                    NFTDetailsBottomSheetConfig.Info(
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