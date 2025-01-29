package com.tangem.features.onboarding.v2.visa.impl.child.inprogress.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun OnboardingVisaInProgress(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
        ) {
            // TODO
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaInProgress()
    }
}