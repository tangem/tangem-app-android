package com.tangem.features.collectibles.impl.inventory.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.collectibles.impl.common.ui.CollectiblesStatus
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ContentUM

@Composable
internal fun CollectiblesInventoryContent(
    content: ContentUM,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    when (content) {
        is ContentUM.Loading -> CollectiblesInventorySkeleton(
            info = content.info,
            contentPadding = contentPadding,
            modifier = modifier,
        )
        is ContentUM.Items -> CollectiblesInventoryCardsGrid(
            items = content.items,
            gridState = gridState,
            contentPadding = contentPadding,
            modifier = modifier,
        )
        is ContentUM.Empty -> Status(
            message = content.message,
            onReloadClick = null,
            isReloading = false,
            contentPadding = contentPadding,
            modifier = modifier,
        )
        is ContentUM.Error -> Status(
            message = content.message,
            onReloadClick = content.onReloadClick,
            isReloading = content.isReloading,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }
}

@Composable
private fun Status(
    message: TextReference,
    onReloadClick: (() -> Unit)?,
    isReloading: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Makes pull-to-refresh work on this state: it only reacts to drags from a scrollable child
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .height(maxHeight)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CollectiblesStatus(
                message = message,
                onReloadClick = onReloadClick,
                isReloading = isReloading,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}