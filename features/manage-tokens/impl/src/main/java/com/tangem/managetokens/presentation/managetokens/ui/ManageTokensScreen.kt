package com.tangem.managetokens.presentation.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.SpacerH18
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.addcustomtoken.ui.AddCustomTokenBottomSheet
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
internal fun ManageTokensScreen(state: ManageTokensState, onHeaderSizeChange: (Dp) -> Unit) {
    var alertState by remember { mutableStateOf<AlertState?>(value = null) }

    EventEffect(
        event = state.event,
        onAlertStateSet = { alertState = it },
    )
    alertState?.let {
        Alert(state = it, onDismiss = { alertState = null })
    }

    Content(state = state, onHeaderSizeChange = onHeaderSizeChange)

    AddCustomTokenBottomSheet(state.customTokenBottomSheetConfig)
}

@Composable
private fun Content(state: ManageTokensState, onHeaderSizeChange: (Dp) -> Unit) {
    val keyboard by keyboardAsState()
    val density = LocalDensity.current
    var tokenListAlertBottomPadding by remember(keyboard is Keyboard.Opened) { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .background(color = TangemTheme.colors.background.primary),
    ) {
        Column {
            val listState = rememberLazyListState()
            val raiseSearchBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
            val elevation by animateDpAsState(
                targetValue = if (raiseSearchBar) TangemTheme.dimens.elevation8 else TangemTheme.dimens.elevation0,
                label = "top_bar_elevation",
            )

            Surface(
                elevation = elevation,
                modifier = Modifier.onGloballyPositioned {
                    with(density) { onHeaderSizeChange(it.size.height.toDp()) }
                },
            ) {
                TokensSearchBar(
                    state = state.searchBarState,
                    modifier = Modifier
                        .background(color = TangemTheme.colors.background.primary)
                        .padding(
                            start = TangemTheme.dimens.spacing16,
                            end = TangemTheme.dimens.spacing16,
                            bottom = TangemTheme.dimens.spacing4,
                        ),
                )
            }

            SpacerH18()

            val tokens = state.tokens.collectAsLazyPagingItems()
            val query = state.searchBarState.query

            TrackPossibleEmptySearchResult(
                tokens = tokens,
                query = query,
                onEmptySearchResult = state.onEmptySearchResult,
            )

            TokensList(
                modifier = Modifier.padding(bottom = tokenListAlertBottomPadding),
                tokens = tokens,
                addCustomTokenButton = state.addCustomTokenButton,
            )
        }

        state.selectedToken?.let { selectedToken ->
            ManageTokensBottomSheet(selectedToken = selectedToken, state = state)
        }

        state.derivationNotification?.let {
            if (keyboard is Keyboard.Closed) {
                DerivationNotification(
                    config = it.config,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned {
                            with(density) { tokenListAlertBottomPadding = it.size.height.toDp() }
                        },
                )
                DisposableEffect(Unit) {
                    onDispose { tokenListAlertBottomPadding = 0.dp }
                }
            }
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ManageTokensScreen(
    @PreviewParameter(ManageTokensConfigProvider::class)
    state: ManageTokensState,
) {
    TangemThemePreview {
        ManageTokensScreen(state) {}
    }
}

private class ManageTokensConfigProvider : CollectionPreviewParameterProvider<ManageTokensState>(
    collection = listOf(
        ManageTokensStatePreviewData.loadingState,
        ManageTokensStatePreviewData.loadedState,
    ),
)
