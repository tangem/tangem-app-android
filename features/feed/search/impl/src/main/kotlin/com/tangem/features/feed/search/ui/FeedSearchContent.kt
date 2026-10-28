package com.tangem.features.feed.search.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.shtorka.LocalShtorkaSurfaceOpacity
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.FeedTabRow
import com.tangem.features.feed.ui.FeedTabUM
import com.tangem.features.feed.v2.FeedV2Component
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList

/**
 * Search results: the feed's tab row over the selected tab's own list. The row is the same set and
 * order as the feed home's and does not shrink when a tab finds nothing.
 *
 * @param tabContent the selected tab page, provided by the search screen's pager
 */
@Composable
internal fun FeedSearchTabsContent(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    tabContent: @Composable (Modifier) -> Unit,
) {
    SearchScreenScaffold(
        fadeHeight = contentPadding.calculateTopPadding() + FeedV2Component.TabRowHeight,
        modifier = modifier,
        overlay = {
            FeedTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelect = onTabSelect,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
            )
        },
        content = tabContent,
    )
}

@Composable
private fun SearchScreenScaffold(
    fadeHeight: Dp,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    // A local haze source — LocalHazeState here would sample the screen behind the card instead.
    val outerHazeState = LocalHazeState.current
    val searchHazeState = rememberHazeState()
    SideEffect { searchHazeState.blurEnabled = outerHazeState.blurEnabled }

    val surfaceOpacity = LocalShtorkaSurfaceOpacity.current
    val sheetColor = LocalMainBottomSheetColor.current.value.takeOrElse { TangemTheme.colors3.bg.primary }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(state = searchHazeState)
                .drawBehind { drawRect(color = sheetColor, alpha = surfaceOpacity()) },
        ) {
            content(Modifier.fillMaxSize())
        }
        CompositionLocalProvider(LocalHazeState provides searchHazeState) {
            TangemFade(
                position = TangemFade.Position.Top,
                variant = TangemFade.Variant.Soft,
                blur = true,
                backgroundColor = sheetColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(fadeHeight)
                    .graphicsLayer { alpha = surfaceOpacity() }
                    .hazeSourceTangem(state = searchHazeState, zIndex = 1f),
            )
            overlay()
        }
    }
}