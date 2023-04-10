package com.tangem.tap.features.tokens.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.FabPosition
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
import com.tangem.tap.features.tokens.presentation.states.TokenItemState
import com.tangem.tap.features.tokens.presentation.states.TokensListStateHolder
import com.tangem.tap.features.tokens.presentation.states.TokensListToolbarState
import com.tangem.tap.features.tokens.presentation.states.TokensListVisibility
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddTokensScreen(stateHolder: TokensListStateHolder) {
    BackHandler(onBack = stateHolder.toolbarState.onBackButtonClick)

    Scaffold(
        topBar = { AddTokensToolbar(state = stateHolder.toolbarState) },
        floatingActionButton = {
            if (stateHolder is TokensListStateHolder.ManageAccess) {
                SaveChangesButton(onClick = stateHolder.onSaveButtonClick)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { scaffoldPadding ->
        // This is a hack because AnimatedContent trigger recomposition if stateHolder content is changed
        AnimatedVisibility(
            visible = stateHolder is TokensListVisibility,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (stateHolder is TokensListVisibility) {
                TokensListContent(
                    stateHolder = stateHolder,
                    scaffoldPadding = scaffoldPadding,
                )
            }
        }

        AnimatedVisibility(
            visible = stateHolder is TokensListStateHolder.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LoadingContent()
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
    val state = rememberLazyListState().apply {
        OnBottomReached(loadMoreThreshold = 40) {
            store.dispatch(TokensAction.LoadMore(scanResponse = store.state.globalState.scanResponse))
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
        state,
    ) {
        items(
            items = stateHolder.tokens,
            key = TokenItemState::name,
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
            stateHolder = TokensListStateHolder.Loading(
                toolbarState = TokensListToolbarState.Title.ManageAccess(
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
            stateHolder = TokensListStateHolder.ManageAccess(
                toolbarState = TokensListToolbarState.Title.ManageAccess(
                    titleResId = R.string.main_manage_tokens,
                    onBackButtonClick = {},
                    onSearchButtonClick = {},
                    onAddCustomTokenClick = {},
                ),
                tokens = persistentListOf(
                    TokenListPreviewData.createManageToken(),
                    TokenListPreviewData.createManageToken(),
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
            stateHolder = TokensListStateHolder.ReadAccess(
                toolbarState = TokensListToolbarState.Title.ReadAccess(
                    titleResId = R.string.search_tokens_title,
                    onBackButtonClick = {},
                    onSearchButtonClick = {},
                ),
                tokens = persistentListOf(
                    TokenListPreviewData.createReadToken(),
                    TokenListPreviewData.createReadToken(),
                ),
            ),
        )
    }
}