package com.tangem.features.feed.ui.earn.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.earn.state.EarnFilterByTypeBottomSheetContentUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM

@Composable
internal fun EarnFilterByTypeBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<EarnFilterByTypeBottomSheetContentUM>(
        config = config,
        titleText = resourceReference(R.string.earn_filter_by),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: EarnFilterByTypeBottomSheetContentUM) {
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

@Preview(widthDp = 360, heightDp = 640)
@Preview(widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
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