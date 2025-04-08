package com.tangem.features.nft.collections.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.features.nft.collections.entity.NFTCollectionsWarningUM

@Composable
internal fun NFTCollectionWarning(state: NFTCollectionsWarningUM, modifier: Modifier = Modifier) {
    Notification(
        modifier = modifier,
        config = state.config,
    )
}