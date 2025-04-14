package com.tangem.features.nft.traits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.nft.component.NFTAssetTraitsComponent
import com.tangem.features.nft.traits.ui.NFTAssetTraits
import com.tangem.features.nft.traits.model.NFTAssetTraitsModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTAssetTraitsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTAssetTraitsComponent.Params,
) : NFTAssetTraitsComponent, AppComponentContext by context {

    private val model: NFTAssetTraitsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTAssetTraits(state)
    }

    @AssistedFactory
    interface Factory : NFTAssetTraitsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTAssetTraitsComponent.Params,
        ): DefaultNFTAssetTraitsComponent
    }
}