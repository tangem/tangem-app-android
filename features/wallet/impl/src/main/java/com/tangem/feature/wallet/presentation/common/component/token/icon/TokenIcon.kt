package com.tangem.feature.wallet.presentation.common.component.token.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.IconState as TokenIconState

private const val GRAY_SCALE_SATURATION = 0f
private const val GRAY_SCALE_ALPHA = 0.4f
private const val NORMAL_ALPHA = 1f

@Composable
internal fun TokenIcon(state: TokenIconState, modifier: Modifier = Modifier) {
    BaseContainer(modifier = modifier) {
        val iconModifier = Modifier
            .align(Alignment.Center)
            .size(TangemTheme.dimens.size36)

        when (state) {
            is TokenIconState.Loading -> LoadingIcon(modifier = iconModifier)
            is TokenIconState.Locked -> LockedIcon(modifier = iconModifier)
            is TokenIconState.CoinIcon,
            is TokenIconState.CustomTokenIcon,
            is TokenIconState.TokenIcon,
            -> {
                ContentIconContainer(modifier = iconModifier, icon = state)
            }
        }
    }
}

@Composable
internal fun LoadingIcon(modifier: Modifier = Modifier) {
    CircleShimmer(modifier = modifier)
}

@Composable
private fun LockedIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.background.secondary,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun BoxScope.ContentIconContainer(icon: TokenIconState, modifier: Modifier = Modifier) {
    val networkBadgeOffset = TangemTheme.dimens.spacing4
    val (alpha, colorFilter) = remember(icon.isGrayscale) {
        if (icon.isGrayscale) {
            GRAY_SCALE_ALPHA to GrayscaleColorFilter
        } else {
            NORMAL_ALPHA to null
        }
    }

    ContentIcon(
        modifier = modifier,
        icon = icon,
        alpha = alpha,
        colorFilter = colorFilter,
    )

    if (icon.networkBadgeIconResId != null) {
        NetworkBadge(
            modifier = Modifier
                .offset(x = networkBadgeOffset, y = -networkBadgeOffset)
                .align(Alignment.TopEnd),
            iconResId = requireNotNull(icon.networkBadgeIconResId),
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }

    if (icon.isCustom) {
        CustomBadge(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private inline fun BaseContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.size(size = TangemTheme.dimens.size40), content = content)
}

private val GrayscaleColorFilter: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) })
