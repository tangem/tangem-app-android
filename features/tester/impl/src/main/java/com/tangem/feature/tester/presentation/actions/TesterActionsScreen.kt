package com.tangem.feature.tester.presentation.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
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
            val onClick = remember(state.hideAllCurrencies) {
                { (state.hideAllCurrencies as? HideAllCurrenciesState.Clickable)?.onClick?.invoke() ?: Unit }
            }
            TesterActionItem(
                progress = state.hideAllCurrencies is HideAllCurrenciesState.Progress,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun TesterActionItem(progress: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(all = TangemTheme.dimens.spacing16)) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.hide_all_currencies),
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
        TesterActionsScreen(state = TesterActionsContentState({}, HideAllCurrenciesState.Clickable({})))
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
