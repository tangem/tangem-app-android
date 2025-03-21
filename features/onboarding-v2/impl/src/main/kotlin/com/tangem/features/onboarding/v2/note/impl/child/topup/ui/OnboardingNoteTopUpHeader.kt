package com.tangem.features.onboarding.v2.note.impl.child.topup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.common.ui.RefreshButton
import com.tangem.features.onboarding.v2.common.ui.WalletCard
import com.tangem.features.onboarding.v2.impl.R

@Composable
fun OnboardingNoteTopUpHeader(
    balance: String,
    cardArtworkUrl: String?,
    isRefreshing: Boolean,
    onRefreshBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 228.dp)
            .widthIn(max = 450.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .fillMaxSize()
                .background(
                    TangemTheme.colors.button.secondary,
                    shape = TangemTheme.shapes.roundedCornersMedium,
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            WalletCard(
                modifier = Modifier.width(120.dp),
                url = cardArtworkUrl,
            )
            SpacerHMax()
            Text(
                text = stringResourceSafe(R.string.common_balance_title),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
            SpacerH8()
            Text(
                text = balance,
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            SpacerHMax()
            RefreshButton(
                isRefreshing = isRefreshing,
                onRefreshBalanceClick = onRefreshBalanceClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardinNoteTopUpHeaderPreview() {
    TangemThemePreview {
        OnboardingNoteTopUpHeader(
            balance = "0.00000001 BTC",
            cardArtworkUrl = "",
            onRefreshBalanceClick = {},
            isRefreshing = false,
        )
    }
}