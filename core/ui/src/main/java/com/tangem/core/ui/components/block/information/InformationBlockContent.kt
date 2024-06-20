package com.tangem.core.ui.components.block.information

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun <T : Any> InformationBlockContentScope.ListItems(
    items: ImmutableList<T>,
    itemContent: @Composable BoxScope.(T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens.spacing8)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8, Alignment.Top),
    ) {
        items.fastForEach { item ->
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemContent(item)
            }
        }
    }
}

@Composable
fun <T : Any> InformationBlockContentScope.GridItems(
    items: ImmutableList<T>,
    itemContent: @Composable BoxScope.(T) -> Unit,
    modifier: Modifier = Modifier,
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        rowItems.fastForEach { row ->
            Row(
                modifier = Modifier
                    .padding(vertical = TangemTheme.dimens.spacing8)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12, Alignment.Start),
            ) {
                row.fastForEach { item ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        itemContent(item)
                    }
                }
            }
        }
    }
}