package com.tangem.features.markets.portfolio.impl.ui

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
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.components.inputrow.InputRowChecked
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.rows.CornersToRound
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContent
import kotlinx.collections.immutable.toImmutableList

@Composable
fun TokenActionsBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<TokenActionsBSContent>(
        config = config,
        title = { content ->
            TangemBottomSheetTitle(content.title)
        },
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: TokenActionsBSContent) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        content.actions.forEachIndexed { index, action ->
            val cornersToRound = when (index) {
                0 -> CornersToRound.TOP_2
                content.actions.lastIndex -> CornersToRound.BOTTOM_2
                else -> CornersToRound.ZERO
            }

            DividerContainer(
                modifier = Modifier
                    .clip(cornersToRound.getShape())
                    .background(TangemTheme.colors.background.action)
                    .clickable { content.onActionClick(action) },
                showDivider = index != content.actions.lastIndex,
            ) {
                InputRowChecked(
                    text = action.text,
                    checked = false,
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
            TokenActionsBottomSheet(
                TangemBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = {},
                    content = TokenActionsBSContent(
                        title = "Wallet 1",
                        actions = TokenActionsBSContent.Action.entries.toImmutableList(),
                        onActionClick = {},
                    ),
                ),
            )
        }
    }
}