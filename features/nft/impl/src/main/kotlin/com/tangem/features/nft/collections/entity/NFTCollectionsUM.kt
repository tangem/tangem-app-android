package com.tangem.features.nft.collections.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class NFTCollectionsUM {

    data class Empty(
        val onReceiveClick: () -> Unit,
    ) : NFTCollectionsUM()

    data class Loading(
        val onReceiveClick: () -> Unit,
    ) : NFTCollectionsUM()

    data class Failed(
        val onRetryClick: () -> Unit,
        val onReceiveClick: () -> Unit,
    ) : NFTCollectionsUM()

    data class Content(
        val search: SearchBarUM,
        val collections: ImmutableList<NFTCollectionUM>,
        val warnings: ImmutableList<NFTCollectionsWarningUM>,
        val onReceiveClick: () -> Unit,
    ) : NFTCollectionsUM()
}