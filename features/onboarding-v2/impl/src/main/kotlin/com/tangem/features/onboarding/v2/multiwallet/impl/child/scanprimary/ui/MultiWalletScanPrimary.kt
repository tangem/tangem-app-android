package com.tangem.features.onboarding.v2.multiwallet.impl.child.scanprimary.ui

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
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal fun MultiWalletScanPrimary(isRing: Boolean, onScanPrimaryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState())
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResourceSafe(R.string.onboarding_title_scan_origin_card),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h2,
            )

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResourceSafe(R.string.onboarding_subtitle_scan_primary),
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.body1,
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(
                if (isRing) R.string.onboarding_button_backup_ring else R.string.onboarding_button_backup_origin,
            ),
            iconResId = R.drawable.ic_tangem_24,
            onClick = onScanPrimaryClick,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletScanPrimary(
            isRing = true,
            onScanPrimaryClick = {},
        )
    }
}