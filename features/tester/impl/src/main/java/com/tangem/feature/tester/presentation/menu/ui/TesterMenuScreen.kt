package com.tangem.feature.tester.presentation.menu.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.menu.state.TesterMenuContentState

/**
 * Screen with functionality for testers
 *
 * @param state screen state
 */
@Composable
internal fun TesterMenuScreen(state: TesterMenuContentState) {
    BackHandler(onBack = state.onBackClick)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary),
    ) {
        AppBarWithBackButton(
            onBackClick = state.onBackClick,
            text = stringResource(id = R.string.tester_menu),
        )

        Column(
            modifier = Modifier
                .padding(
                    horizontal = TangemTheme.dimens.spacing18,
                    vertical = TangemTheme.dimens.spacing8,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            PrimaryButton(
                text = stringResource(R.string.feature_toggles),
                onClick = state.onFeatureTogglesClick,
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton(
                text = stringResource(R.string.environment_toggles),
                onClick = state.onEnvironmentTogglesClick,
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.tester_actions),
                onClick = state.onTesterActionsClick,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTesterMenuScreen() {
    TangemThemePreview {
        TesterMenuScreen(
            state = TesterMenuContentState(
                onBackClick = {},
                onFeatureTogglesClick = {},
                onEnvironmentTogglesClick = {},
                onTesterActionsClick = {},
            ),
        )
    }
}