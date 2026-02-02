package com.tangem.core.ui.ds.row.token.internal

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun RowScope.TokenRowPriceChangeContent(
    priceChangeState: PriceChangeState.Content,
    isFlickering: Boolean,
    isAvailable: Boolean = true,
) {
    AnimatedContent(
        targetState = priceChangeState.type,
        label = "Update the price change's arrow",
        modifier = Modifier.padding(start = TangemTheme.dimens2.x1),
    ) { animatedType ->
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(
                    when (animatedType) {
                        PriceChangeType.UP -> R.drawable.ic_up_dynamic_24
                        PriceChangeType.DOWN -> R.drawable.ic_down_dynamic_24
                        PriceChangeType.NEUTRAL -> R.drawable.ic_static_dynamic_24
                    },
                ),
            ),
            tint = when (animatedType) {
                PriceChangeType.UP -> TangemTheme.colors2.graphic.status.accent
                PriceChangeType.DOWN -> TangemTheme.colors2.graphic.status.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors2.graphic.neutral.secondary
            },
            contentDescription = null,
            modifier = Modifier.size(TangemTheme.dimens2.x3),
        )
    }

    AnimatedContent(
        targetState = priceChangeState.valueInPercent,
        label = "Update the price text",
        modifier = Modifier.padding(start = TangemTheme.dimens2.x0_5),
    ) { animatedText ->
        Text(
            text = animatedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography2.captionSemibold12.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = if (isAvailable) {
                    TangemTheme.colors2.text.neutral.secondary
                } else {
                    TangemTheme.colors2.text.status.disabled
                },
            ),
        )
    }
}