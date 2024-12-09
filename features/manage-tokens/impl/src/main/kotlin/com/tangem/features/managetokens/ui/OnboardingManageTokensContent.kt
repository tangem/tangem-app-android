package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.res.LocalSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.core.ui.utils.rememberHideKeyboardNestedScrollConnection
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.managetokens.component.preview.PreviewOnboardingManageTokensComponent
import com.tangem.features.managetokens.entity.managetokens.OnboardingManageTokensUM
import com.tangem.features.managetokens.impl.R

@Composable
internal fun OnboardingManageTokensContent(state: OnboardingManageTokensUM, modifier: Modifier = Modifier) {
    val nestedScrollConnection = rememberHideKeyboardNestedScrollConnection()

    Scaffold(
        modifier = modifier.nestedScroll(nestedScrollConnection),
        containerColor = TangemTheme.colors.background.primary,
        contentWindowInsets = WindowInsetsZero,
        topBar = {
            SearchBar(
                modifier = Modifier
                    .padding(vertical = TangemTheme.dimens.spacing12, horizontal = TangemTheme.dimens.spacing16),
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
                modifier = Modifier
                    .padding(all = TangemTheme.dimens.spacing16)
                    .navigationBarsPadding(),
                hostState = LocalSnackbarHostState.current,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                config = state.actionButtonConfig,
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

@Composable
private fun FloatingActionButton(config: OnboardingManageTokensUM.ActionButtonConfig, modifier: Modifier = Modifier) {
    when (config) {
        is OnboardingManageTokensUM.ActionButtonConfig.Continue -> ContinueButton(config = config, modifier = modifier)
        is OnboardingManageTokensUM.ActionButtonConfig.Later -> SecondaryButton(
            modifier = modifier,
            text = stringResource(id = R.string.common_later),
            showProgress = config.showProgress,
            onClick = config.onClick,
        )
    }
}

@Composable
private fun ContinueButton(
    config: OnboardingManageTokensUM.ActionButtonConfig.Continue,
    modifier: Modifier = Modifier,
) {
    if (config.showTangemIcon) {
        PrimaryButtonIconEnd(
            modifier = modifier,
            text = stringResource(id = R.string.common_continue),
            iconResId = R.drawable.ic_tangem_24,
            showProgress = config.showProgress,
            onClick = config.onClick,
        )
    } else {
        PrimaryButton(
            modifier = modifier,
            text = stringResource(id = R.string.common_continue),
            showProgress = config.showProgress,
            onClick = config.onClick,
        )
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