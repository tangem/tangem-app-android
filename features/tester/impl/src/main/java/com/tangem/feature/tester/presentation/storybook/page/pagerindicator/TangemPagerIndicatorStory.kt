@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.pagerindicator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.TangemPagerIndicatorColors
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun TangemPagerIndicatorStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("section_page_counts") {
            SectionTitle(text = "Page counts")
        }

        listOf(1, 2, 3, 4, 5).forEach { pageCount ->
            item("count_$pageCount") {
                IndicatorRow(label = "$pageCount page(s)", pageCount = pageCount, currentPage = 0)
            }
        }

        item("section_many_pages") {
            SectionTitle(text = "Many pages (>5)")
        }

        item("many_start") {
            IndicatorRow(label = "7 pages, at start", pageCount = 7, currentPage = 0)
        }

        item("many_middle") {
            IndicatorRow(label = "7 pages, at middle", pageCount = 7, currentPage = 3)
        }

        item("many_end") {
            IndicatorRow(label = "7 pages, at end", pageCount = 7, currentPage = 6)
        }

        item("ten_start") {
            IndicatorRow(label = "10 pages, at start", pageCount = 10, currentPage = 0)
        }

        item("ten_middle") {
            IndicatorRow(label = "10 pages, at middle", pageCount = 10, currentPage = 5)
        }

        item("ten_end") {
            IndicatorRow(label = "10 pages, at end", pageCount = 10, currentPage = 9)
        }

        item("section_active_positions") {
            SectionTitle(text = "Active dot positions (5 pages)")
        }

        repeat(4) { page ->
            item("active_$page") {
                IndicatorRow(label = "Active: page ${page + 1}", pageCount = 5, currentPage = page)
            }
        }

        item("section_overlay") {
            SectionTitle(text = "With overlay background")
        }

        item("overlay") {
            IndicatorRowWithOverlay(pageCount = 5, currentPage = 2)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.subtitle1,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun IndicatorRow(label: String, pageCount: Int, currentPage: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        TangemPagerIndicator(
            pagerState = rememberPagerState(currentPage) { pageCount },
        )
    }
}

@Composable
private fun IndicatorRowWithOverlay(pageCount: Int, currentPage: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = "With overlay",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        TangemPagerIndicator(
            pagerState = rememberPagerState(currentPage) { pageCount },
            colors = TangemPagerIndicatorColors.copy(
                overlay = TangemTheme.colors2.tabs.backgroundSecondary,
            ),
        )
    }
}