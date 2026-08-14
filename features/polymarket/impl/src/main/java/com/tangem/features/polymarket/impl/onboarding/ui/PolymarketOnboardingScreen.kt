package com.tangem.features.polymarket.impl.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
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
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                endButton = { TangemButton.Close(onClick = onCloseClick) },
            )
        },
    ) { contentPadding ->
        if (state.overlay is PolymarketOnboardingUM.Overlay.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TangemTheme.colors3.bg.primary),
                contentAlignment = Alignment.Center,
            ) {
                FailedState(onRetryClick = state.overlay.onRetryClick)
            }
        } else {
            PolymarketWelcomeContent(
                state = state,
                contentPadding = contentPadding,
            )
        }
    }

    RegionRestrictionsBottomSheet(
        isShown = state.overlay is PolymarketOnboardingUM.Overlay.RegionRestrictions,
        onDismiss = {
            (state.overlay as? PolymarketOnboardingUM.Overlay.RegionRestrictions)?.onDismiss?.invoke()
        },
    )
}

@Composable
private fun FailedState(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_something_went_wrong),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X10,
            variant = TangemButton.Variant.Primary,
            text = resourceReference(R.string.common_retry),
            onClick = onRetryClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenWelcomePreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(state = previewState(isStarting = false), onCloseClick = {})
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenStartingPreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(state = previewState(isStarting = true), onCloseClick = {})
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketOnboardingScreenErrorPreview() {
    TangemThemePreviewRedesign {
        PolymarketOnboardingScreen(
            state = previewState(
                isStarting = false,
                overlay = PolymarketOnboardingUM.Overlay.Error(onRetryClick = {}),
            ),
            onCloseClick = {},
        )
    }
}

private fun previewState(isStarting: Boolean, overlay: PolymarketOnboardingUM.Overlay? = null) = PolymarketOnboardingUM(
    isStarting = isStarting,
    startButtonText = resourceReference(R.string.prediction_onboarding_start_button),
    onStartClick = {},
    onPolymarketTermsClick = {},
    onTangemTermsClick = {},
    overlay = overlay,
)