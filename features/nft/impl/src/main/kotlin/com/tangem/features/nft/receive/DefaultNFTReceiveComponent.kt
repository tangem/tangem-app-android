package com.tangem.features.nft.receive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.nft.component.NFTReceiveComponent
import com.tangem.features.nft.receive.model.NFTReceiveModel
import com.tangem.features.nft.receive.ui.NFTReceive
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTReceiveComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTReceiveComponent.Params,
) : NFTReceiveComponent, AppComponentContext by context {

    private val model: NFTReceiveModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTReceive(state, modifier)
    }

    @AssistedFactory
    interface Factory : NFTReceiveComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTReceiveComponent.Params,
        ): DefaultNFTReceiveComponent
    }
}