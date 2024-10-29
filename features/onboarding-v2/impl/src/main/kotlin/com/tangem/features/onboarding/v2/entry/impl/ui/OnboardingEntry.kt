package com.tangem.features.onboarding.v2.entry.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal inline fun OnboardingEntry(modifier: Modifier = Modifier, stepperContent: @Composable (Modifier) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(TangemTheme.colors.background.primary),
    ) {
        stepperContent(Modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingEntry {
            TangemTopAppBar(
                title = "Onboarding",
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_back_24,
                    enabled = true,
                    onIconClicked = {},
                ),
            )
        }
    }
}
