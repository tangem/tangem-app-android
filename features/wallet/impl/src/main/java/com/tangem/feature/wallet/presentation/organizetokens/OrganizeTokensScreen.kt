// TODO: Remove after components implementation
@file:Suppress("UNUSED_PARAMETER")

package com.tangem.feature.wallet.presentation.organizetokens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.state.TokenListState

@Composable
internal fun OrganizeTokensScreen(state: OrganizeTokensStateHolder) {
    Scaffold(
        topBar = {
            TopBar(state.header)
        },
        content = { paddingValues ->
            TokenList(
                state = state.tokens,
                modifier = Modifier.padding(paddingValues),
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Actions(state.actions)
        },
        contentColor = TangemTheme.colors.background.primary,
    )
}

@Composable
private fun TokenList(state: TokenListState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenListState.GroupedByNetwork -> {
            /* [REDACTED_TODO_COMMENT] */
        }
        is TokenListState.Ungrouped -> {
            /* [REDACTED_TODO_COMMENT] */
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(config: OrganizeTokensStateHolder.HeaderConfig, modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Text(
                text = "Organize tokens", // TODO: Move to resources
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.text.primary1,
        ),
    )
}

@Composable
private fun Actions(config: OrganizeTokensStateHolder.ActionsConfig, modifier: Modifier = Modifier) {
    // TODO: Implement list apply and cancel actions
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OrganizeTokensScreenPreview_Light() {
    TangemTheme {
        OrganizeTokensScreen(state = WalletPreviewData.organizeTokensState)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OrganizeTokensScreenPreview_Dark() {
    TangemTheme(isDark = true) {
        OrganizeTokensScreen(state = WalletPreviewData.organizeTokensState)
    }
}
// endregion Preview