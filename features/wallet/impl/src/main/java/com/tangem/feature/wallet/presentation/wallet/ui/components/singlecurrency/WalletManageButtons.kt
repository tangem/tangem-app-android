package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletManageButton
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet manage buttons
 *
 * @param buttons  manage buttons
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@Composable
internal fun WalletManageButtons(buttons: ImmutableList<WalletManageButton>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = buttons,
            key = { it.config.text },
            itemContent = { ActionButton(config = it.config) },
        )
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
