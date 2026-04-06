package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.checkbox.TangemCheckbox
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnFilterByTypeBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM

@Composable
internal fun EarnFilterByTypeBottomSheet(config: TangemBottomSheetConfig) {
    if (LocalRedesignEnabled.current) {
        EarnFilterByTypeBottomSheetV2(config)
    } else {
        EarnFilterByTypeBottomSheetV1(config)
    }
}

@Composable
private fun EarnFilterByTypeBottomSheetV1(config: TangemBottomSheetConfig) {
    TangemBottomSheet<EarnFilterByTypeBottomSheetContentUM>(
        config = config,
        titleText = resourceReference(R.string.earn_filter_by),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { ContentV1(it) },
    )
}

@Composable
private fun EarnFilterByTypeBottomSheetV2(config: TangemBottomSheetConfig) {
    EarnFilterBottomSheet<EarnFilterByTypeBottomSheetContentUM>(
        config = config,
        content = { ContentV2(it) },
    )
}

@Composable
private fun ContentV1(content: EarnFilterByTypeBottomSheetContentUM) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        EarnFilterTypeUM.entries.forEachIndexed { index, type ->
            DividerContainer(
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = EarnFilterTypeUM.entries.lastIndex,
                        addDefaultPadding = false,
                    )
                    .background(TangemTheme.colors.background.action)
                    .clickable { content.onOptionClick(type) },
                showDivider = index != EarnFilterTypeUM.entries.lastIndex,
            ) {
                InputRowChecked(
                    text = type.text,
                    checked = type == content.selectedOption,
                )
            }
        }
    }
}

@Composable
private fun ContentV2(content: EarnFilterByTypeBottomSheetContentUM) {
    CardFilterBlock(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4),
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
                contentPadding = PaddingValues(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3),
            ) {
                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                    text = type.text.resolveReference(),
                    style = TangemTheme.typography2.bodySemibold16,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                TangemCheckbox(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .layoutId(layoutId = TangemRowLayoutId.TAIL),
                    isChecked = type == content.selectedOption,
                    onCheckedChange = { content.onOptionClick(type) },
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Preview(widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV1() {
    TangemThemePreview(
        alwaysShowBottomSheets = true,
    ) {
        Box(Modifier.background(TangemTheme.colors.background.secondary)) {
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

@Preview(widthDp = 360, heightDp = 640)
@Preview(widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign(
        alwaysShowBottomSheets = true,
    ) {
        Box(Modifier.background(TangemTheme.colors.background.secondary)) {
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