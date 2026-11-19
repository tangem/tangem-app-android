package com.tangem.features.tangempay.hotwallet

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.ui.TangemPayUnifiedOnboardingLayout

@Composable
internal fun TangemPayHotWalletOnboardingScreen(
    state: TangemPayHotWalletOnboardingUM,
    video: VideoSurfaceState?,
    modifier: Modifier = Modifier,
) {
    TangemPayUnifiedOnboardingLayout(
        modifier = modifier,
        video = video,
        isGetStartedLoading = state.isLoading,
        isGetStartedIconShown = false,
        onGetStartedClick = state.onGetCardClick,
        onTermsClick = state.onTermsClick,
        onCloseClick = null,
    )
}

@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayHotWalletOnboardingScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayHotWalletOnboardingScreen(
            state = TangemPayHotWalletOnboardingUM(
                isLoading = false,
                onGetCardClick = {},
                onTermsClick = {},
            ),
            video = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}