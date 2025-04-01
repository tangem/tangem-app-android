package com.tangem.features.nft.receive.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

internal data class NFTReceiveUM(
    val onBackClick: () -> Unit,
    val search: SearchBarUM,
    val networks: Networks,
    val bottomSheetConfig: TangemBottomSheetConfig?,
) {

    @Immutable
    sealed class Networks {
        data object Empty : Networks()

        data class Content(
            val items: ImmutableList<NFTNetworkUM>,
        ) : Networks()
    }
}