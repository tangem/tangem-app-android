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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24
import com.tangem.features.feed.ui.LocalIsOpenedInBottomSheet
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM

@Composable
internal fun MarketsTokenDetailsTitle(
    state: MarketsTokenDetailsUM,
    isBackButtonEnabled: Boolean,
    onBackClick: () -> Unit,
) {
    MarketsTokenDetailsRedesignTopBar(
        isAddToPortfolioButtonVisible = state.isAddToPortfolioButtonVisible,
        isAddToPortfolioButtonEnabled = state.isAddToPortfolioButtonEnabled,
        onAddToPortfolioClick = state.onAddToPortfolioClick,
        onShareClick = state.onShareClick,
        isBackButtonEnabled = isBackButtonEnabled,
        onBackClick = onBackClick,
    )
}

@Suppress("LongParameterList")
@Composable
private fun MarketsTokenDetailsRedesignTopBar(
    isAddToPortfolioButtonVisible: Boolean,
    isAddToPortfolioButtonEnabled: Boolean,
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
                contentPadding = 8.dp,
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
                        contentPadding = 10.dp,
                        isDimmed = !isAddToPortfolioButtonEnabled,
                    )
                }
                TopBarHazeIconButton(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_share_new_24),
                    onClick = onShareClick,
                    enabled = isBackButtonEnabled,
                    contentPadding = 10.dp,
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
private fun TopBarHazeIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    contentPadding: Dp,
    isDimmed: Boolean = false,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = if (isDimmed) TangemTheme.colors3.icon.tertiary else TangemTheme.colors3.icon.primary,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .hazeEffectTangem { blurRadius = 8.dp }
            .clickableSingle(onClick = onClick, enabled = enabled)
            .padding(contentPadding),
    )
}