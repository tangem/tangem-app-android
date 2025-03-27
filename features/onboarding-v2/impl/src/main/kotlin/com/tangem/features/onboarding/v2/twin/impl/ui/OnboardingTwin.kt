package com.tangem.features.onboarding.v2.twin.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.twin.impl.ui.state.OnboardingTwinUM

@Suppress("UnusedPrivateMember")
@Composable
internal fun OnboardingTwin(state: OnboardingTwinUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box {
            // TODO
        }
    }

    if (state is OnboardingTwinUM.TopUp) {
        TokenReceiveBottomSheet(config = state.bottomSheetConfig)
    }
}

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingTwin(OnboardingTwinUM.Welcome())
    }
}