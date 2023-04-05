package com.tangem.tap.features.tokens.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.extensions.OnBottomReached
import com.tangem.tap.features.tokens.presentation.states.AddTokensNetworkItemState
import com.tangem.tap.features.tokens.presentation.states.AddTokensStateHolder
import com.tangem.tap.features.tokens.presentation.states.AddTokensToolbarState
import com.tangem.tap.features.tokens.presentation.states.TokenItemModel
import com.tangem.tap.features.tokens.presentation.states.TokensListVisibility
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddTokensScreen(stateHolder: AddTokensStateHolder) {
    BackHandler(onBack = stateHolder.toolbarState.onBackButtonClick)

    Scaffold(
        topBar = { AddTokensToolbar(state = stateHolder.toolbarState) },
        floatingActionButton = {
            if (stateHolder is AddTokensStateHolder.ManageAccess) {
                SaveChangesButton(onClick = stateHolder.onSaveButtonClick)
            }
        },
    ) {
        when (stateHolder) {
            is AddTokensStateHolder.Loading -> LoadingContent()
            is TokensListVisibility -> TokensListContent(stateHolder = stateHolder, scaffoldPadding = it)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemColorPalette.White),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemColorPalette.Meadow)
    }
}

@Composable
private fun TokensListContent(stateHolder: TokensListVisibility, scaffoldPadding: PaddingValues) {
    val state = rememberLazyListState()
    state.OnBottomReached(loadMoreThreshold = 40) {
        store.dispatch(TokensAction.LoadMore(scanResponse = store.state.globalState.scanResponse))
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
        state,
    ) {
        items(
            items = stateHolder.tokens,
            key = TokenItemModel::name,
            itemContent = { TokenItem(model = it) },
        )
    }
}

@Composable
private fun SaveChangesButton(onClick: () -> Unit) {
    PrimaryButton(
        modifier = Modifier
            .imePadding()
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        text = stringResource(id = R.string.common_save_changes),
        onClick = onClick,
    )
}

@Preview
@Composable
private fun Preview_AddTokensScreen_Loading() {
    TangemTheme {
        AddTokensScreen(
            stateHolder = AddTokensStateHolder.Loading(
                toolbarState = AddTokensToolbarState.Title.EditAccess(
                    titleResId = R.string.main_manage_tokens,
                    onBackButtonClick = {},
                    onSearchButtonClick = {},
                    onAddCustomTokenClick = {},
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun Preview_AddTokensScreen_ManageAccess() {
    TangemTheme {
        AddTokensScreen(
            stateHolder = AddTokensStateHolder.ManageAccess(
                toolbarState = AddTokensToolbarState.Title.EditAccess(
                    titleResId = R.string.main_manage_tokens,
                    onBackButtonClick = {},
                    onSearchButtonClick = {},
                    onAddCustomTokenClick = {},
                ),
                tokens = listOf(
                    TokenItemModel(
                        name = "ETHEREUM (ETH)",
                        iconUrl = "",
                        networks = listOf(
                            AddTokensNetworkItemState.EditAccess(
                                name = "ETHEREUM",
                                protocolName = "MAIN",
                                iconResId = R.drawable.img_btc_22,
                                isMainNetwork = true,
                                isAdded = true,
                                networkId = "",
                                onToggleClick = {},
                            ),
                        ),
                    ),
                    TokenItemModel(
                        name = "Tether (USDT)",
                        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
                        networks = listOf(
                            AddTokensNetworkItemState.EditAccess(
                                name = "ETHEREUM",
                                protocolName = "MAIN",
                                iconResId = R.drawable.img_eth_22,
                                isMainNetwork = false,
                                isAdded = true,
                                networkId = "",
                                onToggleClick = {},
                            ),
                            AddTokensNetworkItemState.EditAccess(
                                name = "BNB SMART CHAIN",
                                protocolName = "BEP20",
                                iconResId = R.drawable.ic_bsc_no_color,
                                isMainNetwork = false,
                                isAdded = false,
                                networkId = "",
                                onToggleClick = {},
                            ),
                        ),
                    ),
                ),
                onSaveButtonClick = {},
            ),
        )
    }
}

@Preview
@Composable
private fun Preview_AddTokensScreen_ReadAccess() {
    TangemTheme {
        AddTokensScreen(
            stateHolder = AddTokensStateHolder.ReadAccess(
                toolbarState = AddTokensToolbarState.Title.EditAccess(
                    titleResId = R.string.search_tokens_title,
                    onBackButtonClick = {},
                    onSearchButtonClick = {},
                    onAddCustomTokenClick = {},
                ),
                tokens = listOf(
                    TokenItemModel(
                        name = "ETHEREUM (ETH)",
                        iconUrl = "",
                        networks = listOf(
                            AddTokensNetworkItemState.ReadAccess(
                                name = "ETHEREUM",
                                protocolName = "MAIN",
                                iconResId = R.drawable.img_btc_22,
                                isMainNetwork = true,
                            ),
                        ),
                    ),
                    TokenItemModel(
                        name = "Tether (USDT)",
                        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
                        networks = listOf(
                            AddTokensNetworkItemState.ReadAccess(
                                name = "ETHEREUM",
                                protocolName = "MAIN",
                                iconResId = R.drawable.img_eth_22,
                                isMainNetwork = false,
                            ),
                            AddTokensNetworkItemState.ReadAccess(
                                name = "BNB SMART CHAIN",
                                protocolName = "BEP20",
                                iconResId = R.drawable.ic_bsc_no_color,
                                isMainNetwork = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}