package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.res.LocalSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.managetokens.component.preview.PreviewOnboardingManageTokensComponent
import com.tangem.features.managetokens.entity.managetokens.OnboardingManageTokensUM

@Composable
internal fun OnboardingManageTokensContent(state: OnboardingManageTokensUM, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                keyboardController?.hide()

                return super.onPreScroll(available, source)
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(nestedScrollConnection),
        containerColor = TangemTheme.colors.background.primary,
        contentWindowInsets = WindowInsetsZero,
        topBar = {
            SearchBar(
                modifier = Modifier
                    .padding(bottom = TangemTheme.dimens.spacing12)
                    .padding(horizontal = TangemTheme.dimens.spacing16),
                state = state.search,
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                state = state,
            )
        },
        snackbarHost = {
            TangemSnackbarHost(
                modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
                hostState = LocalSnackbarHostState.current,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            SaveChangesButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                isVisible = true,
                showProgress = state.isSavingInProgress,
                onClick = state.saveChanges,
            )
        },
    )
}

@Composable
private fun Content(state: OnboardingManageTokensUM, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        Currencies(
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            items = state.items,
            showLoadingItem = state.isNextBatchLoading,
            onLoadMore = state.loadMore,
            isEditable = true,
        )

        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }

    EventEffect(event = state.scrollToTop) {
        listState.animateScrollToItem(index = 0)
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ManageTokens(
    @PreviewParameter(PreviewOnboardingManageTokensComponentProvider::class) component: OnboardingManageTokensComponent,
) {
    TangemThemePreview {
        component.Content(Modifier.fillMaxWidth())
    }
}

private class PreviewOnboardingManageTokensComponentProvider :
    CollectionPreviewParameterProvider<PreviewOnboardingManageTokensComponent>(
        collection = listOf(
            PreviewOnboardingManageTokensComponent(isLoading = false),
            PreviewOnboardingManageTokensComponent(isLoading = true),
        ),
    )
// endregion
