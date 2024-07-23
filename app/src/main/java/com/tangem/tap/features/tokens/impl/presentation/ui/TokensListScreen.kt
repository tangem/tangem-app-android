package com.tangem.tap.features.tokens.impl.presentation.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.tokens.impl.presentation.states.TokenItemState
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListStateHolder
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListToolbarState
import com.tangem.wallet.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Tokens list screen
 *
 * @param stateHolder state holder
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun TokensListScreen(stateHolder: TokensListStateHolder, modifier: Modifier = Modifier) {
    BackHandler(onBack = stateHolder.toolbarState.onBackButtonClick)

    var floatingButtonHeight by remember { mutableStateOf(value = 0.dp) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TokensListToolbar(state = stateHolder.toolbarState)
        },
        floatingActionButton = {
            if (stateHolder is TokensListStateHolder.ManageContent) {
                val density = LocalDensity.current
                val verticalPadding = TangemTheme.dimens.spacing32

                SaveChangesButton(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onSizeChanged {
                            with(density) { floatingButtonHeight = it.height.toDp() + verticalPadding }
                        },
                    showProgress = stateHolder.isSavingInProgress,
                    onClick = stateHolder.onSaveButtonClick,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        backgroundColor = TangemTheme.colors.background.secondary,
    ) { _ ->
        val tokens = stateHolder.tokens.collectAsLazyPagingItems()

        if (stateHolder !is TokensListStateHolder.ManageContent) {
            NavigationBar3ButtonsScrim()
        }

        Box {
            TokensListContent(
                isDifferentAddressesBlockVisible = stateHolder.isDifferentAddressesBlockVisible,
                tokens = tokens,
                bottomMarginDp = floatingButtonHeight,
            )

            if (stateHolder is TokensListStateHolder.ManageContent) {
                BottomFade(Modifier.align(Alignment.BottomCenter))
            }
        }

        Crossfade(targetState = stateHolder.isLoading, label = "Update progress bar visibility") {
            if (it) {
                LoadingContent()
            }
        }

        LaunchedEffect(key1 = tokens.loadState.refresh) {
            stateHolder.onTokensLoadStateChanged(tokens.loadState.refresh)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.accent)
    }
}

@Composable
private fun TokensListContent(
    isDifferentAddressesBlockVisible: Boolean,
    tokens: LazyPagingItems<TokenItemState>,
    bottomMarginDp: Dp,
) {
    val state = rememberLazyListState()
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    LazyColumn(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .imePadding()
            .fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            bottom = bottomMarginDp + bottomBarHeight,
        ),
    ) {
        item(
            key = "DifferentAddressesWarning$isDifferentAddressesBlockVisible",
            contentType = "DifferentAddressesWarning$isDifferentAddressesBlockVisible",
        ) {
            if (isDifferentAddressesBlockVisible) DifferentAddressesWarning()
        }

        tokens.itemKey(TokenItemState::composedId)
        tokens.itemContentType(TokenItemState::composedId)

        items(items = tokens.itemSnapshotList.items, key = TokenItemState::composedId) {
            TokenItem(model = it)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(key1 = state) {
        snapshotFlow(state::isScrollInProgress)
            .distinctUntilChanged()
            .collectLatest { if (it) keyboardController?.hide() }
    }
}

@Composable
private fun DifferentAddressesWarning() {
    Box(
        modifier = Modifier
            .padding(TangemTheme.dimens.spacing16)
            .background(
                color = TangemTheme.colors.button.disabled,
                shape = RoundedCornerShape(TangemTheme.dimens.radius10),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val text = stringResource(id = R.string.warning_manage_tokens_legacy_derivation_message)
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing8,
            ),
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Start,
            style = TangemTheme.typography.body2.copy(
                letterSpacing = TextUnit(value = 0.5f, type = TextUnitType.Sp),
                lineHeight = TextUnit(value = 25f, type = TextUnitType.Sp),
            ),
        )
    }
}

@Composable
private fun SaveChangesButton(showProgress: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(
        modifier = modifier
            .imePadding()
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        text = stringResource(id = R.string.common_save_changes),
        showProgress = showProgress,
        onClick = onClick,
    )
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokensListScreen(
    @PreviewParameter(TokensListScreenProvider::class) stateHolder: TokensListStateHolder,
) {
    TangemThemePreview {
        TokensListScreen(stateHolder)
    }
}

private class TokensListScreenProvider : CollectionPreviewParameterProvider<TokensListStateHolder>(
    collection = listOf(
        TokensListStateHolder.ReadContent(
            toolbarState = TokensListToolbarState.Title.Manage(
                titleResId = R.string.main_manage_tokens,
                onBackButtonClick = {},
                onSearchButtonClick = {},
                onAddCustomTokenClick = {},
            ),
            isLoading = true,
            isDifferentAddressesBlockVisible = false,
            tokens = emptyFlow(),
            onTokensLoadStateChanged = {},
        ),
        TokensListStateHolder.ManageContent(
            toolbarState = TokensListToolbarState.Title.Manage(
                titleResId = R.string.main_manage_tokens,
                onBackButtonClick = {},
                onSearchButtonClick = {},
                onAddCustomTokenClick = {},
            ),
            isLoading = false,
            isDifferentAddressesBlockVisible = true,
            tokens = flowOf(
                PagingData.from(
                    listOf(TokenListPreviewData.createManageToken()),
                ),
            ),
            onSaveButtonClick = {},
            onTokensLoadStateChanged = {},
            isSavingInProgress = false,
        ),
        TokensListStateHolder.ReadContent(
            toolbarState = TokensListToolbarState.Title.Read(
                titleResId = R.string.common_search_tokens,
                onBackButtonClick = {},
                onSearchButtonClick = {},
            ),
            isLoading = false,
            isDifferentAddressesBlockVisible = false,
            tokens = flowOf(
                PagingData.from(
                    listOf(TokenListPreviewData.createManageToken()),
                ),
            ),
            onTokensLoadStateChanged = {},
        ),
    ),
)