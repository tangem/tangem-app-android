package com.tangem.core.ui.components.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Horizontal pager indicator
 *
 * @param pagerState state of pager
 * @param indicatorCount counter of visible indicator items
 */
@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier, indicatorCount: Int = 5) {
    val listState = rememberLazyListState()

    val indicatorColor = TangemTheme.colors.control.key
    val overlayColor = TangemTheme.colors.overlay.secondary
    val indicatorSize = 8.dp
    val spacing = 4.dp

    val totalWidth: Dp = indicatorSize * indicatorCount + spacing * (indicatorCount - 1)
    val widthInPx = LocalDensity.current.run { indicatorSize.toPx() }

    val currentItem by remember {
        derivedStateOf {
            pagerState.currentPage
        }
    }

    val itemCount = pagerState.pageCount

    LaunchedEffect(key1 = currentItem) {
        val viewportSize = listState.layoutInfo.viewportSize
        listState.animateScrollToItem(
            currentItem,
            (widthInPx / 2 - viewportSize.width / 2).toInt(),
        )
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .background(
                color = overlayColor,
                shape = CircleShape,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            modifier = Modifier
                .width(totalWidth),
            state = listState,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = false,
        ) {
            indicatorItems(
                itemCount = itemCount,
                currentItem = currentItem,
                indicatorShape = CircleShape,
                activeColor = indicatorColor,
                inActiveColor = indicatorColor.copy(alpha = 0.5f),
                indicatorSize = indicatorSize,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.indicatorItems(
    itemCount: Int,
    currentItem: Int,
    indicatorShape: Shape,
    activeColor: Color,
    inActiveColor: Color,
    indicatorSize: Dp,
) {
    items(itemCount) { index ->

        val isSelected = index == currentItem

        Box(
            modifier = Modifier
                .clip(indicatorShape)
                .size(indicatorSize)
                .background(
                    if (isSelected) activeColor else inActiveColor,
                    indicatorShape,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicatorPreviewFirstPage() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(),
            contentAlignment = Alignment.Center,
        ) {
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { 10 },
            )
            PagerIndicator(pagerState = pagerState)
        }
    }
}