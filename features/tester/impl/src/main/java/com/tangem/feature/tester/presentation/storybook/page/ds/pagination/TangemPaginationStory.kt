@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.pagination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.pagination.TangemPagination
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemPaginationStory
import kotlinx.coroutines.launch

private val PageCounts = listOf(1, 3, 4, 5, 10, 30)

private val PageColors = listOf(
    Color(0xFF6A5AE0),
    Color(0xFF00A3FF),
    Color(0xFF00C48C),
    Color(0xFFFFB800),
    Color(0xFFFF5C5C),
)

@Composable
internal fun TangemPaginationStory(state: TangemPaginationStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        key(state.pageCount) {
            PagerPreview(pageCount = state.pageCount, background = state.background)
        }
        ChipSelector(
            title = "Background",
            options = TangemPagination.Background.entries,
            selected = state.background,
            label = { it.name },
            onSelect = state.onBackgroundChange,
        )
        ChipSelector(
            title = "Page count",
            options = PageCounts,
            selected = state.pageCount,
            label = { it.toString() },
            onSelect = state.onPageCountChange,
        )
    }
}

@Composable
private fun PagerPreview(pageCount: Int, background: TangemPagination.Background) {
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                PageCard(page = page)
            }
            TangemPagination(
                pagerState = pagerState,
                background = background,
                contentDescription = "Page ${pagerState.currentPage + 1} of $pageCount",
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TangemButton(
                text = stringReference("Previous"),
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                isEnabled = pagerState.currentPage > 0,
            )
            TangemButton(
                text = stringReference("Next"),
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                isEnabled = pagerState.currentPage < pageCount - 1,
            )
        }
    }
}

@Composable
private fun PageCard(page: Int) {
    val color = PageColors[page % PageColors.size]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.5f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Page ${page + 1}",
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.staticLight.primary,
        )
    }
}

@Composable
private fun <T> ChipSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.subtitle1,
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
            options.forEach { option ->
                Chip(
                    label = label(option),
                    isSelected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(if (isSelected) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (isSelected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
        )
    }
}