package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.checkbox.TangemCheckbox
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.ui.earn.state.EarnFilterByTypeBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM

@Composable
internal fun EarnFilterByTypeBottomSheet(config: TangemBottomSheetConfig) {
    EarnFilterBottomSheet<EarnFilterByTypeBottomSheetContentUM>(
        config = config,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: EarnFilterByTypeBottomSheetContentUM) {
    CardFilterBlock(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        EarnFilterTypeUM.entries.forEachIndexed { index, type ->
            TangemRowContainer(
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = EarnFilterTypeUM.entries.lastIndex,
                        addDefaultPadding = false,
                    )
                    .clickable { content.onOptionClick(type) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                    text = type.text.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (type == content.selectedOption) {
                    TangemCheckbox(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .layoutId(layoutId = TangemRowLayoutId.TAIL),
                        isChecked = true,
                        onCheckedChange = { content.onOptionClick(type) },
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Preview(widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign(
        alwaysShowBottomSheets = true,
    ) {
        Box(Modifier.background(TangemTheme.colors3.bg.tertiary)) {
            EarnFilterByTypeBottomSheet(
                TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = EarnFilterByTypeBottomSheetContentUM(
                        selectedOption = EarnFilterTypeUM.All,
                        onOptionClick = {},
                    ),
                ),
            )
        }
    }
}