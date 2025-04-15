package com.tangem.features.nft.traits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.features.nft.traits.model.NFTAssetTraitsModel
import com.tangem.features.nft.traits.ui.NFTAssetTraits
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class NFTAssetTraitsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: NFTAssetTraitsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTAssetTraits(state)
    }

    data class Params(
        val nftAsset: NFTAsset,
        val onBackClick: () -> Unit,
    )
}