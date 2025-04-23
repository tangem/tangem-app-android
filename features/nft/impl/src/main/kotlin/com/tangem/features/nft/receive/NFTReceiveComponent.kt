package com.tangem.features.nft.receive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.nft.receive.model.NFTReceiveModel
import com.tangem.features.nft.receive.ui.NFTReceive
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class NFTReceiveComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: NFTReceiveModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        NFTReceive(state, modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val walletName: String,
        val onBackClick: () -> Unit,
    )
}