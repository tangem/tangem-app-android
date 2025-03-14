package com.tangem.features.onboarding.v2.note.impl.child.topup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.common.ui.WalletCard
import com.tangem.features.onboarding.v2.impl.R

@Composable
fun OnboardinNoteTopUpHeader(
    balance: String?,
    cardArtworkUrl: String?,
    isRefreshing: Boolean,
    onRefreshBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 32.dp)
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
                text = balance.orEmpty(),
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

@Composable
private fun RefreshButton(isRefreshing: Boolean, onRefreshBalanceClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size48)
            .shadow(elevation = 2.dp, shape = CircleShape)
            .background(TangemTheme.colors.background.action)
            .clickable(
                enabled = !isRefreshing,
                onClick = onRefreshBalanceClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            ),
    ) {
        if (isRefreshing) {
            Box {
                CircularProgressIndicator(
                    modifier = Modifier.padding(TangemTheme.dimens.spacing12),
                    color = TangemTheme.colors.icon.primary1,
                    strokeWidth = TangemTheme.dimens.size2,
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh_24),
                contentDescription = null,
                tint = TangemTheme.colors.text.disabled,
                modifier = Modifier.padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardinNoteTopUpHeaderPreview() {
    TangemThemePreview {
        OnboardinNoteTopUpHeader(
            balance = "0.00000001 BTC",
            cardArtworkUrl = "",
            onRefreshBalanceClick = {},
            isRefreshing = false,
        )
    }
}