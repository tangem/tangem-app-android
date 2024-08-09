package com.tangem.core.ui.components.block.information

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.rows.ArrowRow
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
inline fun <T : Any> InformationBlockContentScope.ListItems(
    items: ImmutableList<T>,
    itemContent: @Composable BoxScope.(T) -> Unit,
    modifier: Modifier = Modifier,
    itemPadding: PaddingValues = PaddingValues(all = TangemTheme.dimens.spacing0),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArragement: Arrangement.Vertical = Arrangement.Top,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArragement,
    ) {
        items.fastForEach { item ->
            Box(
                modifier = Modifier
                    .padding(itemPadding)
                    .fillMaxWidth(),
            ) {
                itemContent(item)
            }
        }
    }
}

@Composable
inline fun <T : Any> InformationBlockContentScope.GridItems(
    items: ImmutableList<T>,
    itemContent: @Composable BoxScope.(T) -> Unit,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    horizontalArragement: Arrangement.Horizontal = Arrangement.Start,
) {
    val rowItems by remember(items) {
        derivedStateOf {
            items.asSequence()
                .windowed(size = 2, step = 2, partialWindows = true)
                .map { it.toImmutableList() }
                .toImmutableList()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        rowItems.fastForEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = verticalAlignment,
                horizontalArrangement = horizontalArragement,
            ) {
                row.fastForEach { item ->
                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        itemContent(item)
                    }
                }
            }
        }
    }
}

@Composable
inline fun <T : Any> InformationBlockContentScope.ArrowRowItems(
    items: ImmutableList<T>,
    rootContent: @Composable BoxScope.() -> Unit,
    itemContent: @Composable BoxScope.(T) -> Unit,
    modifier: Modifier = Modifier,
    itemPadding: PaddingValues = PaddingValues(all = TangemTheme.dimens.spacing0),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
            content = rootContent,
        )

        items.forEachIndexed { index, item ->
            ArrowRow(
                modifier = Modifier.fillMaxWidth(),
                content = { itemContent(item) },
                contentPadding = itemPadding,
                isLastItem = index == items.size - 1,
            )
        }
    }
}