package com.tangem.features.onboarding.v2.note.impl.child.create.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.WalletArtworks
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.WalletArtworksState
import com.tangem.features.onboarding.v2.note.impl.ALL_STEPS_TOP_CONTAINER_WEIGHT
import com.tangem.features.onboarding.v2.note.impl.child.create.ui.state.OnboardingNoteCreateWalletUM

@Composable
internal fun OnboardingNoteCreateWallet(state: OnboardingNoteCreateWalletUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        WalletArtworks(
            artwork1 = state.artwork,
            artwork2 = null,
            artwork3 = null,
            modifier = Modifier
                .padding(top = 64.dp)
                .padding(horizontal = 24.dp)
                .weight(ALL_STEPS_TOP_CONTAINER_WEIGHT)
                .fillMaxWidth(),
            state = WalletArtworksState.Folded,
        )
        Column(
            modifier = Modifier.weight(1 - ALL_STEPS_TOP_CONTAINER_WEIGHT)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpacerHMax()
            Text(
                text = stringResource(R.string.onboarding_create_wallet_header),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            SpacerH16()
            Text(
                text = stringResource(R.string.onboarding_create_wallet_body),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            SpacerHMax()
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            text = stringResourceSafe(R.string.onboarding_create_wallet_button_create_wallet),
            onClick = state.onCreateClick,
            showProgress = state.createWalletInProgress,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingNoteCreatePreview() {
    TangemThemePreview {
        OnboardingNoteCreateWallet(
            state = OnboardingNoteCreateWalletUM(
                artwork = null,
                createWalletInProgress = false,
                onCreateClick = {},
            ),
        )
    }
}