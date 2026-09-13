package com.tangem.features.feed.earn.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.earn.impl.R
import com.tangem.features.feed.earn.ui.state.EarnFilterByTypeBottomSheetContentUM
import com.tangem.features.feed.earn.ui.state.EarnFilterTypeUM

@Composable
internal fun EarnFilterByTypeBottomSheet(config: TangemBottomSheetConfig) {
    TangemModal<EarnFilterByTypeBottomSheetContentUM>(
        config = config,
        scrollableContent = false,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.common_type),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                fadeEnabled = false,
                onClose = config.onDismissRequest,
            )
        },
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: EarnFilterByTypeBottomSheetContentUM) {
    Column {
        EarnFilterTypeUM.entries.forEachIndexed { index, type ->
            TangemRow(
                titleSlot = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = type.title.resolveReference(),
                            style = TangemTheme.typography3.body.medium,
                            color = TangemTheme.colors3.text.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = type.text.resolveReference(),
                            style = TangemTheme.typography3.caption.medium,
                            color = TangemTheme.colors3.text.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                endSlot = {
                    TangemCheckmark(
                        checked = type == content.selectedOption,
                        modifier = Modifier.padding(start = 8.dp),
                        onCheckedChange = { content.onOptionClick(type) },
                    )
                },
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = EarnFilterTypeUM.entries.lastIndex,
                    addDefaultPadding = false,
                ),
                onClick = { content.onOptionClick(type) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign(
        alwaysShowBottomSheets = true,
    ) {
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