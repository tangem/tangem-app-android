package com.tangem.features.markets.tokenlist.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.rows.CornersToRound
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByBottomSheetContentUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM

@Composable
fun MarketsListSortByBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<SortByBottomSheetContentUM>(
        config = config,
        titleText = resourceReference(R.string.markets_sort_by_title),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: SortByBottomSheetContentUM) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        SortByTypeUM.entries.forEachIndexed { index, type ->
            val cornersToRound = when (index) {
                0 -> CornersToRound.TOP_2
                SortByTypeUM.entries.lastIndex -> CornersToRound.BOTTOM_2
                else -> CornersToRound.ZERO
            }

            DividerContainer(
                modifier = Modifier
                    .clip(cornersToRound.getShape())
                    .background(TangemTheme.colors.background.action)
                    .clickable { content.onOptionClicked(type) },
                showDivider = index != SortByTypeUM.entries.lastIndex,
            ) {
                InputRowChecked(
                    text = type.text,
                    checked = type == content.selectedOption,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Preview(widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview(
        alwaysShowBottomSheets = true,
    ) {
        Box(Modifier.background(TangemTheme.colors.background.secondary)) {
            MarketsListSortByBottomSheet(
                TangemBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = {},
                    content = SortByBottomSheetContentUM(
                        selectedOption = SortByTypeUM.Trending,
                        onOptionClicked = {},
                    ),
                ),
            )
        }
    }
}
