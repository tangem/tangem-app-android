package com.tangem.features.tangempay.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.AppBarWithBackButton

@Suppress("UnusedPrivateMember")
@Composable
internal fun TandemPayOnboardingScreen(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
                onBackClick = {},
                text = "",
                iconRes = R.drawable.ic_back_24,
            )
        },
        content = { paddingValues ->
            val contentModifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        },
    )
}