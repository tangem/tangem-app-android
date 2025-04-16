package com.tangem.features.onboarding.v2.multiwallet.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    artworksState: WalletArtworksState,
    isSeedPhraseState: Boolean,
    modifier: Modifier = Modifier,
    childContent: @Composable (Modifier) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (isSeedPhraseState.not()) {
            WalletArtworks(
                artwork1 = state.artwork1,
                artwork2 = state.artwork2,
                artwork3 = state.artwork3,
                modifier = Modifier
                    .padding(horizontal = 34.dp)
                    .padding(top = 20.dp)
                    .weight(.48f)
                    .fillMaxWidth(),
                state = artworksState,
            )

            Box(
                modifier = Modifier.weight(.52f),
                contentAlignment = Alignment.BottomStart,
            ) {
                childContent(Modifier)
            }
        } else {
            childContent(Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingMultiWallet(
            state = OnboardingMultiWalletUM(artwork1 = null),
            artworksState = WalletArtworksState.Folded,
            isSeedPhraseState = false,
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun Preview2() {
    TangemThemePreview {
        OnboardingMultiWallet(
            state = OnboardingMultiWalletUM(artwork1 = null),
            artworksState = WalletArtworksState.Folded,
            isSeedPhraseState = true,
            childContent = { md ->
                Box(
                    modifier = md
                        .background(Color.DarkGray)
                        .fillMaxSize(),
                )
            },
        )
    }
}