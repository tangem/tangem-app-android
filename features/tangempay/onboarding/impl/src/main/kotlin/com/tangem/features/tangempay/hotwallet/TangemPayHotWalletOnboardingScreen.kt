package com.tangem.features.tangempay.hotwallet

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.legal.LegalCaption
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.ui.TangemPayOnboardingLayout

@Composable
internal fun TangemPayHotWalletOnboardingScreen(state: TangemPayHotWalletOnboardingUM, modifier: Modifier = Modifier) {
    TangemPayOnboardingLayout(
        modifier = modifier,
        isGetCardLoading = state.isLoading,
        onGetCardClick = state.onGetCardClick,
        onTermsClick = state.onTermsClick,
        legalCaption = {
            LegalCaption(
                onTermsOfServiceClick = state.onTermsOfServiceClick,
                onPrivacyPolicyClick = state.onPrivacyPolicyClick,
            )
        },
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
                onTermsOfServiceClick = {},
                onPrivacyPolicyClick = {},
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}