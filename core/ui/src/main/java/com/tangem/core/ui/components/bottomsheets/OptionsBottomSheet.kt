package com.tangem.core.ui.components.bottomsheets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

/**
 * Generic options bottom sheet component
 *
 * @param config Bottom sheet configuration containing OptionsBottomSheetContent
 * @param title Title text for the bottom sheet
 * @param containerColor Background color of the bottom sheet
 */
@Composable
fun OptionsBottomSheet(
    config: TangemBottomSheetConfig,
    title: TextReference,
    containerColor: androidx.compose.ui.graphics.Color = TangemTheme.colors.background.tertiary,
) {
    TangemBottomSheet<OptionsBottomSheetContent>(
        config = config,
        titleText = title,
        containerColor = containerColor,
        content = { content ->
            OptionsBottomSheetContent(content = content)
        },
    )
}

@Composable
private fun OptionsBottomSheetContent(content: OptionsBottomSheetContent) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        content.options.forEachIndexed { index, option ->
            InputRowDefault(
                text = option.label,
                showDivider = index < content.options.size - 1,
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = content.options.size - 1,
                        addDefaultPadding = false,
                    )
                    .background(TangemTheme.colors.background.action)
                    .clickable { content.onOptionClick(option.key) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OptionsBottomSheetPreview() {
    TangemThemePreview {
        OptionsBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = OptionsBottomSheetContent(
                    options = persistentListOf(
                        BottomSheetOption(
                            key = "option1",
                            label = TextReference.Str("First Option"),
                        ),
                        BottomSheetOption(
                            key = "option2",
                            label = TextReference.Str("Second Option"),
                        ),
                        BottomSheetOption(
                            key = "option3",
                            label = TextReference.Str("Third Option"),
                        ),
                    ),
                    onOptionClick = {},
                ),
            ),
            title = TextReference.Str("Select Option"),
        )
    }
}