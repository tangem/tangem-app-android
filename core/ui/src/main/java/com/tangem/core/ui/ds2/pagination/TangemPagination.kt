package com.tangem.core.ui.ds2.pagination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 page indicator: a row of dots, one per page, with the current page highlighted.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=13196-2954)
 *
 * Behavior notes:
 * - Up to four dots are visible. With more pages the dots sit in a four-item window; a dot at a
 *   window edge with pages beyond it is drawn smaller (overflow). The window moves by one dot when
 *   the new current page would land on an overflow dot, so the current dot is never an overflow one.
 *   The indicator keeps its width while the window moves.
 * - Every page change is a single 200 ms transition: the current-dot color crossfade, the window
 *   shift and the dots shrinking out of / growing into the window run together. A page change in
 *   the middle of a transition retargets from the current values, so fast swipes stay in sync.
 *
 * Accessibility: the dots are not announced one by one. Pass [contentDescription] (e.g. "Page 2 of
 * 5") for TalkBack to read the position; without it the indicator is silent.
 *
 * @param pageCount Total number of pages. `0` renders an empty indicator.
 * @param currentPage Zero-based index of the current page; values out of range are clamped.
 * @param modifier Modifier applied to the indicator.
 * @param background Backdrop drawn behind the dots. See [TangemPagination.Background].
 * @param contentDescription Accessibility label announced by TalkBack. `null` keeps the indicator silent.
 */
@Composable
fun TangemPagination(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    background: TangemPagination.Background = TangemPagination.Background.None,
    contentDescription: String? = null,
) {
    val semanticsModifier = modifier.clearAndSetSemantics {
        contentDescription?.let { this.contentDescription = it }
    }
    val items: @Composable () -> Unit = {
        PaginationItems(
            pageCount = pageCount,
            currentPage = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
            modifier = Modifier.padding(10.dp),
        )
    }

    when (background) {
        TangemPagination.Background.None -> Box(modifier = semanticsModifier) { items() }
        TangemPagination.Background.Material -> TangemSurface(
            modifier = semanticsModifier,
            isMaterial = true,
            shape = CircleShape,
            content = items,
        )
    }
}

/**
 * [TangemPagination] bound to a pager: shows [PagerState.pageCount] dots and follows
 * [PagerState.currentPage], which switches halfway through a swipe.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=13196-2954)
 *
 * @param pagerState State of the pager the indicator follows.
 * @param modifier Modifier applied to the indicator.
 * @param background Backdrop drawn behind the dots. See [TangemPagination.Background].
 * @param contentDescription Accessibility label announced by TalkBack. `null` keeps the indicator silent.
 */
@Composable
fun TangemPagination(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    background: TangemPagination.Background = TangemPagination.Background.None,
    contentDescription: String? = null,
) {
    TangemPagination(
        pageCount = pagerState.pageCount,
        currentPage = pagerState.currentPage,
        modifier = modifier,
        background = background,
        contentDescription = contentDescription,
    )
}

/** Public API surface of [TangemPagination]. */
object TangemPagination {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /** Backdrop drawn behind the dots. */
    enum class Background {
        /** Dots only — for placing on a plain surface. */
        None,

        /** Translucent material pill with a soft shadow — for placing over content, e.g. images. */
        Material,
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemPaginationPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TangemPagination.Background.entries.forEach { background ->
                PreviewRow(label = background.name, background = background)
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, background: TangemPagination.Background) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemPagination(pageCount = 4, currentPage = 0, background = background)
            TangemPagination(pageCount = 10, currentPage = 2, background = background)
            TangemPagination(pageCount = 10, currentPage = 4, background = background)
            TangemPagination(pageCount = 10, currentPage = 8, background = background)
        }
    }
}