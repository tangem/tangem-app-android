package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.getDefaultRowColors
import com.tangem.core.ui.components.getWarningRowColors
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.ActionsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenActionButtonConfig
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TokenActionsBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<ActionsBottomSheetConfig>(config = config) {
        ActionsBottomSheetContent(actions = it.actions)
    }
}

@Composable
private fun ActionsBottomSheetContent(actions: ImmutableList<TokenActionButtonConfig>) {
    Column(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
        actions.forEach { action ->
            if (action.enabled) {
                val rowColors = if (action.isWarning) {
                    getWarningRowColors()
                } else {
                    getDefaultRowColors()
                }
                SimpleSettingsRow(
                    title = action.text.resolveReference(),
                    icon = action.iconResId,
                    enabled = action.enabled,
                    rowColors = rowColors,
                    onItemsClick = action.onClick,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActionsBottomSheetContent_Light(
    @PreviewParameter(ActionsBottomSheetContentConfigProvider::class)
    config: ActionsBottomSheetConfig,
) {
    TangemThemePreview {
        // Use preview of content because ModalBottomSheet isn't supported in Preview mode
        ActionsBottomSheetContent(actions = config.actions)
    }
}

private class ActionsBottomSheetContentConfigProvider : CollectionPreviewParameterProvider<ActionsBottomSheetConfig>(
    collection = listOf(WalletPreviewData.actionsBottomSheet),
)