package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet manage buttons
 *
 * @param buttons  manage buttons
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletManageButtons(buttons: ImmutableList<WalletManageButton>) {
    Row(
        modifier = Modifier
            .horizontalScroll(state = rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
    ) {
        buttons.forEach { button ->
            key(button.config.text) {
                ActionButton(config = button.config)
            }
        }
    }
}

@Preview
@Composable
private fun Preview_WalletManageButtons_Light(
    @PreviewParameter(WalletManageButtonProvider::class) buttons: ImmutableList<WalletManageButton>,
) {
    TangemTheme(isDark = false) {
        WalletManageButtons(buttons = buttons)
    }
}

@Preview
@Composable
private fun Preview_WalletManageButtons_Dark(
    @PreviewParameter(WalletManageButtonProvider::class) buttons: ImmutableList<WalletManageButton>,
) {
    TangemTheme(isDark = true) {
        WalletManageButtons(buttons = buttons)
    }
}

private class WalletManageButtonProvider : CollectionPreviewParameterProvider<ImmutableList<WalletManageButton>>(
    collection = listOf(WalletPreviewData.manageButtons),
)