package com.tangem.managetokens.presentation.managetokens.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.ui.ChooseWalletBottomSheet
import com.tangem.managetokens.presentation.common.ui.ChooseWalletBottomSheetConfig
import com.tangem.managetokens.presentation.common.ui.EventEffect
import com.tangem.managetokens.presentation.common.ui.components.Alert
import com.tangem.managetokens.presentation.managetokens.state.ManageTokensState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.previewdata.ManageTokensStatePreviewData
import com.tangem.managetokens.presentation.managetokens.ui.components.DerivationNotification
import com.tangem.managetokens.presentation.managetokens.ui.components.TokensList
import com.tangem.managetokens.presentation.managetokens.ui.components.TokensSearchBar

@Composable
internal fun ManageTokensScreen(state: ManageTokensState) {
    var alertState by remember { mutableStateOf<AlertState?>(value = null) }

    EventEffect(
        event = state.event,
        onAlertStateSet = { alertState = it },
    )
    alertState?.let {
        Alert(state = it, onDismiss = { alertState = null })
    }

    Content(state)
}

@Composable
private fun Content(state: ManageTokensState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.primary)
            .padding(top = TangemTheme.dimens.spacing32),
    ) {
        Column {
            val listState = rememberLazyListState()
            val raiseSearchBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

            val elevation by animateDpAsState(
                targetValue = if (raiseSearchBar) {
                    TangemTheme.dimens.elevation8
                } else {
                    TangemTheme.dimens.elevation0
                },
                label = "top_bar_elevation",
            )
            Surface(
                elevation = elevation,
                modifier = Modifier,
            ) {
                TokensSearchBar(
                    state = state.searchBarState,
                    modifier = Modifier
                        .background(color = TangemTheme.colors.background.primary)
                        .padding(horizontal = TangemTheme.dimens.spacing16, vertical = TangemTheme.dimens.spacing20),
                )
            }
            val tokens = state.tokens.collectAsLazyPagingItems()
            val query = state.searchBarState.query

            TrackPossibleEmptySearchResult(
                tokens = tokens,
                query = query,
                onEmptySearchResult = state.onEmptySearchResult,
            )

            TokensList(tokens = tokens, addCustomTokenButton = state.addCustomTokenButton)
        }
        state.derivationNotification?.let {
            DerivationNotification(
                config = it.config,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
            )
        }
        state.selectedToken?.let { selectedToken ->
            ManageTokensBottomSheet(selectedToken = selectedToken, state = state)
        }
    }
}

@Composable
private fun TrackPossibleEmptySearchResult(
    tokens: LazyPagingItems<TokenItemState>,
    query: String,
    onEmptySearchResult: (String) -> Unit,
) {
    val wasLoading = remember { mutableStateOf(false) }

    LaunchedEffect(tokens.loadState) {
        val isLoading = tokens.loadState.refresh == LoadState.Loading
        val stoppedLoading = wasLoading.value && !isLoading
        val queryAndTokensCondition = query.isNotEmpty() && tokens.itemSnapshotList.isEmpty()

        if (stoppedLoading && queryAndTokensCondition) {
            onEmptySearchResult(query)
        }

        wasLoading.value = isLoading
    }
}

@Composable
private fun ManageTokensBottomSheet(selectedToken: TokenItemState.Loaded, state: ManageTokensState) {
    if (state.showChooseWalletScreen && state.chooseWalletState is ChooseWalletState.Choose) {
        val config = TangemBottomSheetConfig(
            isShow = true,
            content = ChooseWalletBottomSheetConfig(state.chooseWalletState),
            onDismissRequest = state.chooseWalletState.onCloseChoosingWalletClick,
        )
        ChooseWalletBottomSheet(config)
    } else {
        val config = TangemBottomSheetConfig(
            isShow = true,
            content = ChooseNetworkBottomSheetConfig(
                selectedToken = selectedToken,
                chooseWalletState = state.chooseWalletState,
            ),
            onDismissRequest = selectedToken.chooseNetworkState.onCloseChooseNetworkScreen,
        )
        ChooseNetworkBottomSheet(config)
    }
}

@Preview
@Composable
private fun Preview_ManageTokensScreen_LightTheme(
    @PreviewParameter(ManageTokensConfigProvider::class)
    state: ManageTokensState,
) {
    TangemTheme(isDark = false) {
        ManageTokensScreen(state)
    }
}

@Preview
@Composable
private fun Preview_ManageTokensScreen_DarkTheme(
    @PreviewParameter(ManageTokensConfigProvider::class)
    state: ManageTokensState,
) {
    TangemTheme(isDark = true) {
        ManageTokensScreen(state)
    }
}

private class ManageTokensConfigProvider : CollectionPreviewParameterProvider<ManageTokensState>(
    collection = listOf(
        ManageTokensStatePreviewData.loadingState,
        ManageTokensStatePreviewData.loadedState,
    ),
)