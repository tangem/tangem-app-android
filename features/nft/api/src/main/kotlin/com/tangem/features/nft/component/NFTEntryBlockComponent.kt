package com.tangem.features.nft.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableStatelessListContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface NFTEntryBlockComponent : ComposableStatelessListContentComponent {

    override fun LazyListScope.content(modifier: Modifier)

    data class Params(
        val selectedWallet: Flow<UserWalletId>,
    )

    interface Factory : ComponentFactory<Params, NFTEntryBlockComponent>
}