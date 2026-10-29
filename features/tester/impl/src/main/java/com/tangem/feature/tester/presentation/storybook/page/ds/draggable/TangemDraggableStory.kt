@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.draggable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.ds2.draggable.TangemDraggable
import com.tangem.core.ui.ds2.draggable.rememberTangemDraggableState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemDraggableStory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
internal fun TangemDraggableStory(state: TangemDraggableStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ComponentPreview(visualState = state.visualState)
        StateSelector(selected = state.visualState, onSelect = state.onVisualStateChange)
        LiveReorderList(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ComponentPreview(visualState: TangemDraggable.State) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 16.dp),
    ) {
        TangemDraggable(state = visualState, modifier = Modifier.fillMaxWidth()) {
            DemoRow(item = DemoItem(id = 0, title = "Bitcoin", subtitle = "BTC", value = "$64,210.00"))
        }
    }
}

@Composable
private fun StateSelector(selected: TangemDraggable.State, onSelect: (TangemDraggable.State) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "State",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        val shape = RoundedCornerShape(50)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(TangemTheme.colors3.bg.primary)
                .border(width = 1.dp, color = TangemTheme.colors3.border.tertiary, shape = shape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TangemDraggable.State.entries.forEach { visualState ->
                Chip(
                    label = visualState.name,
                    selected = visualState == selected,
                    onClick = { onSelect(visualState) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(if (selected) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography3.caption.medium,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
        )
    }
}

/** Order lives in a [MutableStateFlow], like in a real model, so moves reach the list a frame later. */
@Composable
private fun LiveReorderList(modifier: Modifier = Modifier) {
    val itemsFlow = remember { MutableStateFlow(DemoItems) }
    val items by itemsFlow.collectAsStateWithLifecycle()
    val clickCounts = remember { mutableStateMapOf<Int, Int>() }
    var dropCount by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()
    val draggableState = rememberTangemDraggableState(
        listState = listState,
        onMove = { from, to -> itemsFlow.update { it.toMutableList().apply { add(to, removeAt(from)) } } },
        onDragEnd = { dropCount++ },
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Long-press a row to drag it. Drops: $dropCount",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(items = items, key = DemoItem::id) { item ->
                TangemDraggable(state = draggableState, key = item.id) {
                    DemoRow(
                        item = item,
                        clickCount = clickCounts[item.id] ?: 0,
                        onClick = { clickCounts[item.id] = (clickCounts[item.id] ?: 0) + 1 },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoRow(item: DemoItem, clickCount: Int = 0, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.value,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            if (onClick != null) {
                Text(
                    text = "Clicks: $clickCount",
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

private data class DemoItem(val id: Int, val title: String, val subtitle: String?, val value: String)

private val DemoItems = listOf(
    DemoItem(id = 1, title = "Bitcoin", subtitle = "BTC · Bitcoin network", value = "$64,210.00"),
    DemoItem(id = 2, title = "Ethereum", subtitle = null, value = "$3,120.45"),
    DemoItem(id = 3, title = "Solana", subtitle = null, value = "$148.90"),
    DemoItem(id = 4, title = "Tether", subtitle = "USDT · Ethereum, Tron, Solana", value = "$1.00"),
    DemoItem(id = 5, title = "XRP", subtitle = null, value = "$0.52"),
    DemoItem(id = 6, title = "Cardano", subtitle = null, value = "$0.45"),
    DemoItem(id = 7, title = "Dogecoin", subtitle = "DOGE · Dogecoin network", value = "$0.12"),
    DemoItem(id = 8, title = "Polygon", subtitle = null, value = "$0.71"),
    DemoItem(id = 9, title = "Polkadot", subtitle = null, value = "$6.80"),
    DemoItem(id = 10, title = "USD Coin", subtitle = "USDC · Ethereum, Polygon, Arbitrum", value = "$1.00"),
    DemoItem(id = 11, title = "Litecoin", subtitle = null, value = "$72.30"),
    DemoItem(id = 12, title = "Avalanche", subtitle = null, value = "$27.60"),
    DemoItem(id = 13, title = "Chainlink", subtitle = "LINK · Ethereum", value = "$14.20"),
    DemoItem(id = 14, title = "Toncoin", subtitle = null, value = "$5.40"),
    DemoItem(id = 15, title = "Tron", subtitle = null, value = "$0.13"),
)