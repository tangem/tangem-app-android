package com.tangem.features.feed.ui.market.detailed

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24
import com.tangem.features.feed.ui.LocalIsOpenedInBottomSheet
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM

@Composable
internal fun MarketsTokenDetailsTitle(
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    isBackButtonEnabled: Boolean,
    onBackClick: () -> Unit,
) {
    if (LocalRedesignEnabled.current) {
        MarketsTokenDetailsRedesignTopBar(
            isAddToPortfolioButtonVisible = state.isAddToPortfolioButtonVisible,
            onAddToPortfolioClick = state.onAddToPortfolioClick,
            onShareClick = state.onShareClick,
            isBackButtonEnabled = isBackButtonEnabled,
            onBackClick = onBackClick,
        )
    } else {
        MarketsTokenDetailsTopBar(
            onBackClick = onBackClick,
            isBackButtonEnabled = isBackButtonEnabled,
            shouldShowPriceSubtitle = state.shouldShowPriceSubtitle,
            tokenName = state.tokenName,
            tokenPrice = state.priceText,
            backgroundColor = backgroundColor,
            onShareClick = state.onShareClick,
        )
    }
}

@Composable
private fun MarketsTokenDetailsRedesignTopBar(
    isAddToPortfolioButtonVisible: Boolean,
    onAddToPortfolioClick: () -> Unit,
    onShareClick: () -> Unit,
    isBackButtonEnabled: Boolean,
    onBackClick: () -> Unit,
) {
    TangemTopBar(
        startContent = {
            TopBarHazeIconButton(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_28),
                onClick = onBackClick,
                enabled = isBackButtonEnabled,
                contentPadding = TangemTheme.dimens2.x2,
            )
        },
        endContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedVisibility(
                    visible = isAddToPortfolioButtonVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    TopBarHazeIconButton(
                        imageVector = Icons.ic_sign_plus_24,
                        onClick = onAddToPortfolioClick,
                        enabled = isBackButtonEnabled,
                        contentPadding = TangemTheme.dimens2.x2_5,
                    )
                }
                TopBarHazeIconButton(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_share_new_24),
                    onClick = onShareClick,
                    enabled = isBackButtonEnabled,
                    contentPadding = TangemTheme.dimens2.x2_5,
                )
            }
        },
        type = if (LocalIsOpenedInBottomSheet.current) {
            TangemTopBarType.BottomSheet
        } else {
            TangemTopBarType.Default
        },
    )
}

@Composable
private fun TopBarHazeIconButton(imageVector: ImageVector, onClick: () -> Unit, enabled: Boolean, contentPadding: Dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = TangemTheme.colors2.graphic.neutral.primary,
        modifier = Modifier
            .size(TangemTheme.dimens2.x11)
            .clip(CircleShape)
            .hazeEffectTangem { blurRadius = 8.dp }
            .clickableSingle(onClick = onClick, enabled = enabled)
            .padding(contentPadding),
    )
}