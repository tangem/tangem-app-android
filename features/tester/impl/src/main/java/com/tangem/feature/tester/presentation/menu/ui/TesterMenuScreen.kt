package com.tangem.feature.tester.presentation.menu.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.menu.state.TesterMenuUM
import com.tangem.feature.tester.presentation.menu.state.TesterMenuUM.ButtonUM
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Screen with functionality for testers
 *
 * @param state screen state
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TesterMenuScreen(state: TesterMenuUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        stickyHeader {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResourceSafe(id = R.string.tester_menu),
                containerColor = TangemTheme.colors.background.primary,
            )
        }

        items(
            items = state.buttons.toImmutableList(),
            key = ButtonUM::textResId,
            contentType = { "button" },
        ) { button ->
            PrimaryButton(
                text = stringResourceSafe(button.textResId),
                onClick = { state.onButtonClick(button) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
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
            state = TesterMenuUM(
                onBackClick = {},
                buttons = persistentSetOf(
                    ButtonUM.FEATURE_TOGGLES,
                    ButtonUM.EXCLUDED_BLOCKCHAINS,
                    ButtonUM.ENVIRONMENT_TOGGLES,
                    ButtonUM.BLOCKCHAIN_PROVIDERS,
                    ButtonUM.TESTER_ACTIONS,
                ),
                onButtonClick = {},
            ),
        )
    }
}