package com.tangem.core.ui.components.currency.icon

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
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.getGreyScaleColorFilter

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
fun CurrencyIcon(state: CurrencyIconState, modifier: Modifier = Modifier, shouldDisplayNetwork: Boolean = true) {
    BaseContainer(modifier = modifier) {
        val iconModifier = Modifier
            .align(Alignment.Center)
            .size(TangemTheme.dimens.size36)

        when (state) {
            is CurrencyIconState.Loading -> LoadingIcon(modifier = iconModifier)
            is CurrencyIconState.Locked -> LockedIcon(modifier = iconModifier)
            is CurrencyIconState.CoinIcon,
            is CurrencyIconState.CustomTokenIcon,
            is CurrencyIconState.TokenIcon,
            -> {
                ContentIconContainer(
                    icon = state,
                    modifier = iconModifier,
                    shouldShowTopBadge = shouldDisplayNetwork,
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
    icon: CurrencyIconState,
    shouldShowTopBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    val networkBadgeOffset = TangemTheme.dimens.spacing4
    val (alpha, colorFilter) = remember(icon.isGrayscale) {
        getGreyScaleColorFilter(icon.isGrayscale)
    }

    ContentIcon(
        modifier = modifier,
        icon = icon,
        alpha = alpha,
        colorFilter = colorFilter,
    )

    if (icon.topBadgeIconResId != null && shouldShowTopBadge) {
        TopBadge(
            modifier = Modifier
                .offset(x = networkBadgeOffset, y = -networkBadgeOffset)
                .align(Alignment.TopEnd),
            iconResId = requireNotNull(icon.topBadgeIconResId),
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }

    if (icon.showCustomBadge) {
        BottomBadge(
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private inline fun BaseContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.size(size = TangemTheme.dimens.size40), content = content)
}
