package com.tangem.core.ui.components.currency.tokenicon

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

private const val GRAY_SCALE_SATURATION = 0f
private const val GRAY_SCALE_ALPHA = 0.4f
private const val NORMAL_ALPHA = 1f

/**
 * Cryptocurrency icon with network badge
 *
* [REDACTED_TODO_COMMENT]
 *
 * @param state cryptocurrency icon config
 * @param modifier component modifier
 * @param shouldDisplayNetwork specifies whether to display network badge
 */
@Composable
fun TokenIcon(state: TokenIconState, modifier: Modifier = Modifier, shouldDisplayNetwork: Boolean = true) {
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
                ContentIconContainer(
                    icon = state,
                    modifier = iconModifier,
                    shouldDisplayNetwork = shouldDisplayNetwork,
                )
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
                    color = TangemTheme.colors.field.primary,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun BoxScope.ContentIconContainer(
    icon: TokenIconState,
    modifier: Modifier = Modifier,
    shouldDisplayNetwork: Boolean = true,
) {
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

    if (icon.networkBadgeIconResId != null && shouldDisplayNetwork) {
        NetworkBadge(
            modifier = Modifier
                .offset(x = networkBadgeOffset, y = -networkBadgeOffset)
                .align(Alignment.TopEnd),
            iconResId = requireNotNull(icon.networkBadgeIconResId),
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }

    if (icon.showCustomBadge) {
        CustomBadge(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private inline fun BaseContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.size(size = TangemTheme.dimens.size40), content = content)
}

private val GrayscaleColorFilter: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) })
