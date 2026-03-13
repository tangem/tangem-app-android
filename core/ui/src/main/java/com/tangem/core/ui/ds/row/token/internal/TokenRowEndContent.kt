package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.placeholder.TextPlaceholder
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TokenRowEndContent(
    endContentUM: TangemTokenRowUM.EndContentUM,
    isBalanceHidden: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    placeholderWidth: Dp,
    modifier: Modifier = Modifier,
) {
    when (endContentUM) {
        is TangemTokenRowUM.EndContentUM.Content -> Content(
            modifier = modifier,
            endContentUM = endContentUM,
            isBalanceHidden = isBalanceHidden,
            textStyle = textStyle,
            textColor = textColor,
        )
        TangemTokenRowUM.EndContentUM.Loading -> TextShimmer(
            style = textStyle,
            modifier = modifier.width(placeholderWidth),
            radius = TangemTheme.dimens2.x25,
        )
        TangemTokenRowUM.EndContentUM.Placeholder -> TextPlaceholder(
            modifier = modifier,
            textStyle = textStyle,
            width = placeholderWidth,
        )
        TangemTokenRowUM.EndContentUM.Empty -> Unit
    }
}

@Composable
private fun Content(
    endContentUM: TangemTokenRowUM.EndContentUM.Content,
    textStyle: TextStyle,
    textColor: Color,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = endContentUM.startIcons.isNotEmpty(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            ) {
                endContentUM.startIcons.fastForEach { icon ->
                    Icon(
                        modifier = Modifier.size(TangemTheme.dimens2.x3),
                        painter = rememberVectorPainter(image = ImageVector.vectorResource(icon.iconRes)),
                        tint = icon.tintReference(),
                        contentDescription = null,
                    )
                }
            }
        }

        Text(
            text = endContentUM.text.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
            style = textStyle.applyBladeBrush(
                isEnabled = endContentUM.isFlickering,
                textColor = if (endContentUM.isAvailable) {
                    TangemTheme.colors2.text.neutral.primary
                } else {
                    TangemTheme.colors2.text.status.disabled
                },
            ),
        )

        AnimatedVisibility(
            visible = endContentUM.endIcons.isNotEmpty(),
        ) {
            Row(
                modifier = Modifier.padding(start = TangemTheme.dimens2.x0_5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            ) {
                endContentUM.endIcons.fastForEach { icon ->
                    Icon(
                        modifier = Modifier.size(TangemTheme.dimens2.x3),
                        painter = rememberVectorPainter(image = ImageVector.vectorResource(icon.iconRes)),
                        tint = icon.tintReference(),
                        contentDescription = null,
                    )
                }
            }
        }

        when (val priceChangeUM = endContentUM.priceChangeUM) {
            is PriceChangeState.Content -> TokenRowPriceChangeContent(
                priceChangeState = priceChangeUM,
                isFlickering = endContentUM.isFlickering,
                isAvailable = endContentUM.isAvailable,
            )
            PriceChangeState.Unknown -> Unit
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenRowEndContent_Preview(
    @PreviewParameter(TokenRowEndContentPreviewProvider::class) params: TangemTokenRowUM.EndContentUM,
) {
    TangemThemePreviewRedesign {
        TokenRowEndContent(
            endContentUM = params,
            isBalanceHidden = false,
            textColor = TangemTheme.colors2.text.neutral.primary,
            textStyle = TangemTheme.typography2.captionSemibold12,
            placeholderWidth = TangemTheme.dimens2.x11,
        )
    }
}

private class TokenRowEndContentPreviewProvider : PreviewParameterProvider<TangemTokenRowUM.EndContentUM> {
    override val values: Sequence<TangemTokenRowUM.EndContentUM>
        get() = sequenceOf(
            TangemTokenRowPreviewData.bottomEndContentUM,
            TangemTokenRowUM.EndContentUM.Loading,
            TangemTokenRowUM.EndContentUM.Empty,
        )
}
// endregion