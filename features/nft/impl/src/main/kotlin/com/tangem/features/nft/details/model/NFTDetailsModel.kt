package com.tangem.features.nft.details.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.nft.component.NFTDetailsComponent
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
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
            onSeeAllClick = ::onSeeAllClick,
            onExploreClick = ::onExploreClick,
            onSendClick = ::onSendClick,
            bottomSheetConfig = null,
        ),
    )

    private fun onReadMoreClick() {
        // TODO implement
    }

    private fun onSeeAllClick() {
        // TODO implement
    }

    private fun onExploreClick() {
        // TODO implement
    }

    private fun onSendClick() {
        // TODO implement
    }

    private fun navigateBack() {
        router.pop()
    }
}