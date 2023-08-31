package com.tangem.feature.tester.presentation.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.feature.tester.impl.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TesterActionsScreen(state: TesterActionsContentState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary),
    ) {
        stickyHeader {
            AppBarWithBackButton(
                text = stringResource(R.string.tester_actions),
                onBackClick = state.onBackClick,
            )
        }
        item {
            val onClick = remember(state.hideAllCurrenciesConfig) {
                { (state.hideAllCurrenciesConfig as? HideAllCurrenciesConfig.Clickable)?.onClick?.invoke() ?: Unit }
            }
            TesterActionItem(
                name = stringResource(R.string.hide_all_currencies),
                progress = state.hideAllCurrenciesConfig is HideAllCurrenciesConfig.Progress,
                onClick = onClick,
            )
        }
        item {
            val config = state.toggleAppThemeConfig

            TesterActionItem(
                name = stringResource(id = R.string.toggle_app_theme, config.currentAppTheme.name),
                onClick = config.onClick,
            )
        }
    }
}

@Composable
private fun TesterActionItem(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Boolean = false,
) {
    Box(modifier = modifier.padding(all = TangemTheme.dimens.spacing16)) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = name,
            onClick = onClick,
            showProgress = progress,
        )
    }
}

// region Preview
@Composable
private fun TesterActionsScreenSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        TesterActionsScreen(
            state = TesterActionsContentState(
                onBackClick = {},
                hideAllCurrenciesConfig = HideAllCurrenciesConfig.Clickable {},
                toggleAppThemeConfig = ToggleAppThemeConfig(AppThemeMode.DEFAULT) {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TesterActionsScreenPreview_Light() {
    TangemTheme {
        TesterActionsScreenSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TesterActionsScreenPreview_Dark() {
    TangemTheme(isDark = true) {
        TesterActionsScreenSample()
    }
}
// endregion Preview
