package com.tangem.features.nft.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.nft.collections.model.NFTCollectionsModel
import com.tangem.features.nft.collections.ui.NFTCollections
import com.tangem.features.nft.component.NFTCollectionsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTCollectionsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTCollectionsComponent.Params,
) : NFTCollectionsComponent, AppComponentContext by context {

    private val model: NFTCollectionsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTCollections(state, modifier)
    }

    @AssistedFactory
    interface Factory : NFTCollectionsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTCollectionsComponent.Params,
        ): DefaultNFTCollectionsComponent
    }
}