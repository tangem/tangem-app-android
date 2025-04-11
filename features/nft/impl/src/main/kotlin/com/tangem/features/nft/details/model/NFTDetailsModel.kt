package com.tangem.features.nft.details.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.nft.component.NFTDetailsComponent
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class NFTDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params: NFTDetailsComponent.Params = paramsContainer.require()

    val state: StateFlow<NFTDetailsUM> get() = _state

    private val _state = MutableStateFlow(
        value = NFTDetailsUM(
            nftAsset = NFTAssetUM(
                name = "",
                media = NFTAssetUM.Media.Empty,
                topInfo = NFTAssetUM.TopInfo.Empty,
                traits = persistentListOf(),
                baseInfoItems = persistentListOf(),
            ),
            onBackClick = ::navigateBack,
            onReadMoreClick = ::onReadMoreClick,
            onSeeAllTraitsClick = ::onSeeAllTraitsClick,
            onExploreClick = ::onExploreClick,
            onSendClick = ::onSendClick,
            bottomSheetConfig = null,
        ),
    )

    private fun onReadMoreClick() {
        // TODO implement
    }

    private fun onSeeAllTraitsClick() {
        router.push(
            AppRoute.NFTAssetTraits(
                nftAsset = params.nftAsset,
            ),
        )
    }

    private fun onExploreClick() {
        // TODO implement
    }

    private fun onSendClick() {
        router.push(
            AppRoute.NFTSend(
                userWalletId = params.userWalletId,
                nftAsset = params.nftAsset,
                nftCollectionName = params.nftCollectionName,
            ),
        )
    }

    private fun navigateBack() {
        router.pop()
    }
}