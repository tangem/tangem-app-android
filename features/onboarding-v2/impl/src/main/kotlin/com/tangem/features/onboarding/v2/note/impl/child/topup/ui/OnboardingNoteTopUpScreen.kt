package com.tangem.features.onboarding.v2.note.impl.child.topup.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.note.impl.child.topup.ui.state.OnboardingNoteTopUpUM

@Composable
fun OnboardingNoteTopUp(state: OnboardingNoteTopUpUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingNoteTopUpHeader(
            balance = state.balance,
            cardArtworkUrl = state.cardArtworkUrl,
            onRefreshBalanceClick = state.onRefreshBalanceClick,
            isRefreshing = state.isRefreshing,
            modifier = Modifier
                .padding(horizontal = 34.dp)
                .padding(top = 16.dp)
                .height(228.dp)
                .fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(top = 16.dp)
                .weight(1.0f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(R.string.onboarding_topup_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                text = stringResourceSafe(R.string.onboarding_top_up_body),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        BottomButtons(state)

        state.addressBottomSheetConfig?.let { config ->
            TokenReceiveBottomSheet(config = config)
        }
    }
}

@Composable
private fun BottomButtons(state: OnboardingNoteTopUpUM) {
    if (state.availableForBuy) {
        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_top_up_button_but_crypto),
            onClick = state.onBuyCryptoClick,
        )
    } else {
        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_button_receive_crypto),
            onClick = state.onShowWalletAddressClick,
        )
    }
    AnimatedVisibility(visible = !state.availableForBuyLoading) {
        if (state.availableForBuy) {
            SecondaryButton(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.onboarding_top_up_button_show_wallet_address),
                onClick = state.onShowWalletAddressClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingNoteTopUpPreview() {
    TangemThemePreview {
        OnboardingNoteTopUp(
            state = OnboardingNoteTopUpUM(
                availableForBuy = true,
            ),
        )
    }
}