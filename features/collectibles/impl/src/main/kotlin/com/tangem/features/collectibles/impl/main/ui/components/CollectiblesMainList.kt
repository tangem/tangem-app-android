package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.features.collectibles.impl.common.ui.CollectiblesLoadMoreEffect
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.ui.CollectiblesDiscoveryListContext
import com.tangem.features.collectibles.impl.discovery.ui.LocalCollectiblesDiscoveryListContext
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import androidx.compose.ui.unit.dp

internal object MainListKeys {
    const val HEADER = "header"
    const val FILLER = "filler"
}

internal data class CollectiblesLeadingBlocks(
    val areShown: Boolean,
    val isTabBandPinned: Boolean,
    val alpha: Float,
)

@Suppress("LongParameterList")
@Composable
internal fun CollectiblesMainList(
    state: CollectiblesMainUM,
    discoveryComponent: CollectiblesDiscoveryComponent,
    discoveryState: CollectiblesDiscoveryUM,
    listState: LazyListState,
    lead: CollectiblesLeadingBlocks,
    fillerHeight: Dp,
    statusHeight: Dp,
    contentPadding: PaddingValues,
) {
    CollectiblesLoadMoreEffect(
        listState = listState,
        isEnabled = (discoveryState.content as? CollectiblesDiscoveryUM.ContentUM.Content)?.isReloading == false,
        onLoadMore = discoveryComponent::onLoadMore,
    )

    val discoveryListContext = CollectiblesDiscoveryListContext(
        isTabBandPinned = lead.isTabBandPinned,
        isTabBandShown = lead.areShown,
        leadingBlocksAlpha = lead.alpha,
        statusHeight = statusHeight,
    )

    CompositionLocalProvider(LocalCollectiblesDiscoveryListContext provides discoveryListContext) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            headerItem(state = state, isHeaderShown = lead.areShown, alpha = lead.alpha)

            with(discoveryComponent) { content(uiState = discoveryState, modifier = Modifier) }

            if (discoveryState.content is CollectiblesDiscoveryUM.ContentUM.Content) {
                fillerItem(height = fillerHeight)
            }
        }
    }
}

private fun LazyListScope.headerItem(state: CollectiblesMainUM, isHeaderShown: Boolean, alpha: Float) {
    item(
        key = MainListKeys.HEADER,
        contentType = MainListKeys.HEADER,
    ) {
        // Zero-height rather than omitted, so a scroll position copied from the resting list keeps pointing
        // at the same item.
        if (!isHeaderShown) return@item

        Column(modifier = Modifier.alpha(alpha).animateContentSize()) {
            CollectiblesBalanceBlock(balance = state.balance)
            CollectiblesActionsBlock(actions = state.actions)
            (state.relink as? CollectiblesMainUM.RelinkUM.Shown)?.let { relink -> RelinkBanner(relink = relink) }
            state.collection?.let { collection -> CollectiblesCollectionWidget(state = collection) }
        }
    }
}

@Composable
private fun RelinkBanner(relink: CollectiblesMainUM.RelinkUM.Shown) {
    val bannerState = remember(relink) {
        TangemMessageBanner.State(
            title = resourceReference(R.string.collectibles_main_relink_title),
            description = resourceReference(R.string.collectibles_main_relink_description),
            variant = TangemMessageBanner.Variant.Error,
            iconEnd = TangemIconUM.Icon(
                imageVector = Icons.ic_error_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            primaryButton = TangemMessageBanner.Button(
                text = resourceReference(R.string.collectibles_main_relink_button),
                onClick = relink.onRelinkClick,
                isLoading = relink.isLoading,
            ),
        )
    }

    TangemMessageBanner(
        state = bannerState,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

private fun LazyListScope.fillerItem(height: Dp) {
    item(
        key = MainListKeys.FILLER,
        contentType = MainListKeys.FILLER,
    ) {
        Spacer(modifier = Modifier.height(height))
    }
}