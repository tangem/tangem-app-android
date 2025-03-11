package com.tangem.core.ui.components.token.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState as TokenFiatAmountState

@Composable
internal fun TokenFiatAmount(state: TokenFiatAmountState?, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokenFiatAmountState.Content -> {
            ContentFiatAmount(
                modifier = modifier,
                text = state.text.orMaskWithStars(isBalanceHidden),
                icons = state.icons,
                isAmountFlickering = state.isFlickering,
            )
        }
        is TokenFiatAmountState.TextContent -> {
            FiatAmountText(
                text = state.text.orMaskWithStars(isBalanceHidden),
                modifier = modifier,
                isAvailable = state.isAvailable,
                isFlickering = state.isFlickering,
            )
        }
        is TokenFiatAmountState.Loading -> {
            RectangleShimmer(modifier = modifier.placeholderSize(), radius = 4.dp)
        }
        is TokenFiatAmountState.Locked -> {
            LockedRectangle(modifier = modifier.placeholderSize())
        }
        null -> Unit
    }
}

@Composable
private fun ContentFiatAmount(
    text: String,
    icons: List<TokenFiatAmountState.Content.IconUM>,
    isAmountFlickering: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = icons.isNotEmpty(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                icons.fastForEach { icon ->
                    Icon(
                        modifier = Modifier.size(12.dp),
                        painter = rememberVectorPainter(image = ImageVector.vectorResource(icon.iconRes)),
                        tint = if (icon.useAccentColor) {
                            TangemTheme.colors.icon.accent
                        } else {
                            TangemTheme.colors.icon.inactive
                        },
                        contentDescription = null,
                    )
                }
            }
        }

        FiatAmountText(text = text, isFlickering = isAmountFlickering)
    }
}

@Composable
private fun FiatAmountText(
    text: String,
    modifier: Modifier = Modifier,
    isAvailable: Boolean = true,
    isFlickering: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.body2.applyBladeBrush(
            isEnabled = isFlickering,
            textColor = if (isAvailable) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.tertiary,
        ),
    )
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = 4.dp)
        .size(width = 40.dp, height = 12.dp)
}