package com.tangem.features.nft.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.nft.details.model.NFTDetailsModel
import com.tangem.features.nft.details.ui.NFTDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class NFTDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: NFTDetailsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTDetails(state, modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
        val onBackClick: () -> Unit,
        val onAllTraitsClick: () -> Unit,
    )
}