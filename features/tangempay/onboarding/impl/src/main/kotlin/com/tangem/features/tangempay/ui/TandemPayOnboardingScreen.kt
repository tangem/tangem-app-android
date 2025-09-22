package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun TandemPayOnboardingScreen(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
                onBackClick = {},
                iconRes = R.drawable.ic_back_24,
            )
        },
        content = { paddingValues ->
            TangemPayOnboardingContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                state = state,
            )
        },
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDarkTheme() {
    TangemThemePreview {
        TandemPayOnboardingScreen(state = TangemPayOnboardingScreenState())
    }
}