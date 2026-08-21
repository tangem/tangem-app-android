package com.tangem.features.polymarket.impl.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.features.polymarket.impl.regionrestrictions.ui.RegionRestrictionsBottomSheet

@Composable
internal fun PolymarketOnboardingScreen(
    state: PolymarketOnboardingUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PolymarketOnboardingUM.Resolving -> ResolvingScreen(onCloseClick = onCloseClick, modifier = modifier)
        is PolymarketOnboardingUM.Welcome -> WelcomeScreen(
            state = state,
            onCloseClick = onCloseClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun ResolvingScreen(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(endButton = { TangemButton.Close(onClick = onCloseClick) })
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            TangemLoader(
                color = TangemTheme.colors3.icon.primary,
                size = TangemLoaderSize.X32,
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    state: PolymarketOnboardingUM.Welcome,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var footerHeight by remember { mutableIntStateOf(0) }

    // The design draws the bar over a clean hero, but content scrolling under a bare bar is unreadable — so the
    // DS scrim (which animates its own alpha) is off only while the hero is at rest.
    val isTopBarScrimShown by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                fadeEnabled = isTopBarScrimShown,
                endButton = { TangemButton.Close(onClick = onCloseClick) },
            )
        },
        overlay = { contentPadding ->
            PolymarketWelcomeFooter(
                state = state,
                contentPadding = contentPadding,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { footerHeight = it.height },
            )
        },
    ) { contentPadding ->
        PolymarketWelcomeContent(
            scrollState = scrollState,
            contentPadding = contentPadding,
            trailingSpace = with(LocalDensity.current) { footerHeight.toDp() } + 54.dp,
        )
    }

    RegionRestrictionsBottomSheet(
        isShown = state.isRegionRestrictionsShown,
        onDismiss = state.onRegionRestrictionsDismiss,
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenResolvingPreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(state = PolymarketOnboardingUM.Resolving, onCloseClick = {})
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenWelcomePreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(state = previewState(isInProgress = false), onCloseClick = {})
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenStartingPreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(state = previewState(isInProgress = true), onCloseClick = {})
    }
}

private fun previewState(isInProgress: Boolean) = PolymarketOnboardingUM.Welcome(
    isInProgress = isInProgress,
    startButtonText = resourceReference(R.string.prediction_onboarding_start_button),
    onStartClick = {},
    onPolymarketTermsClick = {},
    onTangemTermsClick = {},
)