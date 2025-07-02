package com.tangem.features.nft.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.nft.collections.model.NFTCollectionsModel
import com.tangem.features.nft.collections.ui.NFTCollections
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class NFTCollectionsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: NFTCollectionsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTCollections(state, modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val onBackClick: () -> Unit,
        val onAssetClick: (asset: NFTAsset, collection: NFTCollection) -> Unit,
        val onReceiveClick: () -> Unit,
    )
}