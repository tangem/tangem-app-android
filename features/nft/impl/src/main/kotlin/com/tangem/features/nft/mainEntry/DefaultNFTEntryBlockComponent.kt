package com.tangem.features.nft.mainEntry

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.nft.component.NFTEntryBlockComponent
import com.tangem.features.nft.mainEntry.model.NFTEntryBlockModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultNFTEntryBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTEntryBlockComponent.Params,
) : NFTEntryBlockComponent, AppComponentContext by context {

    private val model: NFTEntryBlockModel = getOrCreateModel(params = params)

    override fun LazyListScope.content(modifier: Modifier) {
        item(key = NFT_COLLECTIONS_CONTENT_TYPE, contentType = NFT_COLLECTIONS_CONTENT_TYPE) {
            val state by model.uiState.collectAsStateWithLifecycle()
            NFTBlockItem(
                modifier = modifier.animateItem(),
                nftBlockUM = state,
            )
        }
    }

    @AssistedFactory
    interface Factory : NFTEntryBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTEntryBlockComponent.Params,
        ): DefaultNFTEntryBlockComponent
    }

    companion object {
        private const val NFT_COLLECTIONS_CONTENT_TYPE = "NFTCollections"
    }
}