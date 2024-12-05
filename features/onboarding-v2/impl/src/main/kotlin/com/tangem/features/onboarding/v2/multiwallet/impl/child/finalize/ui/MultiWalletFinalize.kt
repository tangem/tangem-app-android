package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.state.MultiWalletFinalizeUM

@Suppress("UnusedPrivateMember")
@Composable
internal fun MultiWalletFinalize(state: MultiWalletFinalizeUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 74.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(
                    when {
                        state.scanPrimary && state.isRing -> R.string.common_origin_ring
                        state.scanPrimary && state.isRing.not() -> R.string.common_origin_card
                        state.scanPrimary.not() && state.isRing -> R.string.onboarding_title_backup_ring
                        else -> R.string.onboarding_title_backup_card
                    },
                ),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h2,
            )

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = if (state.isRing) {
                    stringResource(R.string.onboarding_subtitle_scan_ring)
                } else {
                    stringResource(
                        R.string.onboarding_subtitle_scan_backup_card_format,
                        state.cardNumber,
                    )
                },
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.body1,
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResource(
                if (state.isRing) R.string.onboarding_button_backup_ring else R.string.onboarding_button_backup_origin,
            ),
            iconResId = R.drawable.ic_tangem_24,
            onClick = state.onScanClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletFinalize(
            state = MultiWalletFinalizeUM(),
        )
    }
}