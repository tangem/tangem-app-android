package com.tangem.features.collectibles.impl.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ItemUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CollectiblesInventoryCardsGrid(
    items: ImmutableList<ItemUM>,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key = { item ->
                when (item) {
                    is ItemUM.Info -> "info"
                    is ItemUM.Card -> "card_" + item.id
                }
            },
            contentType = { item -> item::class },
            span = { item ->
                when (item) {
                    is ItemUM.Info -> GridItemSpan(maxLineSpan)
                    is ItemUM.Card -> GridItemSpan(1)
                }
            },
        ) { item ->
            when (item) {
                is ItemUM.Info -> {
                    CollectiblesInventoryInfoBanner(
                        state = item,
                        modifier = Modifier
                            .animateItem()
                            .padding(top = 4.dp, bottom = 20.dp),
                    )
                }
                is ItemUM.Card -> {
                    CollectiblesInventoryCard(
                        state = item,
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}