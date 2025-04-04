package com.tangem.features.nft.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.nft.component.NFTDetailsComponent
import com.tangem.features.nft.details.model.NFTDetailsModel
import com.tangem.features.nft.details.ui.NFTDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTDetailsComponent.Params,
) : NFTDetailsComponent, AppComponentContext by context {

    private val model: NFTDetailsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTDetails(state, modifier)
    }

    @AssistedFactory
    interface Factory : NFTDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTDetailsComponent.Params,
        ): DefaultNFTDetailsComponent
    }
}