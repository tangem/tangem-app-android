package com.tangem.features.details.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.details.entity.SelectEmailFeedbackTypeBS
import com.tangem.features.details.impl.R

@Composable
internal fun SelectFeedbackEmailTypeBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<SelectEmailFeedbackTypeBS>(
        config = config,
        titleText = resourceReference(R.string.common_choose_action),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: SelectEmailFeedbackTypeBS) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        SelectEmailFeedbackTypeBS.Option.entries.forEachIndexed { index, type ->
            DividerContainer(
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = SelectEmailFeedbackTypeBS.Option.entries.lastIndex,
                        addDefaultPadding = false,
                    )
                    .background(TangemTheme.colors.background.action)
                    .clickable { content.onOptionClick(type) },
                showDivider = index != SelectEmailFeedbackTypeBS.Option.entries.lastIndex,
            ) {
                InputRowChecked(
                    text = type.text,
                    checked = false,
                )
            }
        }
    }
}