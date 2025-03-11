package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM
import kotlinx.collections.immutable.toImmutableList

@Composable
fun TokenActionsBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<TokenActionsBSContentUM>(
        config = config,
        title = { content ->
            TangemBottomSheetTitle(content.title)
        },
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: TokenActionsBSContentUM) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        content.actions.forEachIndexed { index, action ->
            Box(
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = content.actions.lastIndex,
                        addDefaultPadding = false,
                    )
                    .background(TangemTheme.colors.background.action),
            ) {
                SimpleSettingsRow(
                    title = action.text.resolveReference(),
                    icon = action.iconRes,
                    redesign = true,
                    onItemsClick = { content.onActionClick(action) },
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
                    isShown = true,
                    onDismissRequest = {},
                    content = TokenActionsBSContentUM(
                        title = "Wallet 1",
                        actions = TokenActionsBSContentUM.Action.entries.toImmutableList(),
                        onActionClick = {},
                    ),
                ),
            )
        }
    }
}