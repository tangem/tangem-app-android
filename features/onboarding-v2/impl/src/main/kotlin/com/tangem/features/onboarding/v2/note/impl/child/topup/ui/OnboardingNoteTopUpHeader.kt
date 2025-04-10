package com.tangem.features.onboarding.v2.note.impl.child.topup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalTangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.common.ui.RefreshButton
import com.tangem.features.onboarding.v2.common.ui.WalletCard
import com.tangem.features.onboarding.v2.impl.R
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.ImmutableList

@Composable
fun OnboardingNoteTopUpHeader(
    balance: String,
    cardArtwork: ArtworkUM?,
    isRefreshing: Boolean,
    onRefreshBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 180.dp)
            .widthIn(max = 450.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp)
                .fillMaxSize()
                .background(
                    TangemTheme.colors.button.secondary,
                    shape = TangemTheme.shapes.roundedCornersMedium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                SpacerHMax()
                Text(
                    text = stringResourceSafe(R.string.common_balance_title),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
                SpacerH8()
                Text(
                    modifier = if (balance.isEmpty()) {
                        Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius3))
                            .shimmer(LocalTangemShimmer.current)
                    } else {
                        Modifier
                    },
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                    text = balance,
                )
                SpacerHMax()
            }
        }
        WalletCard(
            modifier = Modifier.width(120.dp).align(Alignment.TopCenter),
            artwork = cardArtwork,
        )
        RefreshButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            isRefreshing = isRefreshing,
            onRefreshBalanceClick = onRefreshBalanceClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardinNoteTopUpHeaderPreview() {
    TangemThemePreview {
        OnboardingNoteTopUpHeader(
            balance = "0.00000001 BTC",
            cardArtwork = ArtworkUM(null as ImmutableList<Byte>?, ""),
            onRefreshBalanceClick = {},
            isRefreshing = false,
        )
    }
}