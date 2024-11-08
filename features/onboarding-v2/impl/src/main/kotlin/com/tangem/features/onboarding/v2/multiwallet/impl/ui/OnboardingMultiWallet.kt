package com.tangem.features.onboarding.v2.multiwallet.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.state.OnboardingMultiWalletUM

@Composable
@Suppress("MagicNumber")
internal fun OnboardingMultiWallet(
    state: OnboardingMultiWalletUM,
    modifier: Modifier = Modifier,
    childContent: @Composable (Modifier) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        WalletArtworks(
            url = state.artworkUrl,
            modifier = Modifier
                .weight(.56f)
                .padding(horizontal = 34.dp)
                .padding(top = 20.dp)
                .fillMaxWidth(),
            state = WalletArtworksState.Folded,
        )

        Box(
            modifier = Modifier.weight(.44f),
            contentAlignment = Alignment.BottomStart,
        ) {
            childContent(Modifier)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingMultiWallet(
            state = OnboardingMultiWalletUM(artworkUrl = null),
            childContent = { md ->
                Box(
                    modifier = md
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.DarkGray),
                )
            },
        )
    }
}
