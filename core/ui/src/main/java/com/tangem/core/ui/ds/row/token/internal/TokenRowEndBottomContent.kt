package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TokenRowEndBottomContent(
    endContentUM: TangemTokenRowUM.EndContentUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (endContentUM) {
        is TangemTokenRowUM.EndContentUM.Content -> Content(
            modifier = modifier,
            endContentUM = endContentUM,
            isBalanceHidden = isBalanceHidden,
        )
        TangemTokenRowUM.EndContentUM.Empty -> Unit
        TangemTokenRowUM.EndContentUM.Loading -> TextShimmer(
            style = TangemTheme.typography2.captionSemibold12,
            modifier = modifier.width(TangemTheme.dimens2.x10),
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Composable
private fun Content(
    endContentUM: TangemTokenRowUM.EndContentUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = endContentUM.text.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography2.captionSemibold12.applyBladeBrush(
                isEnabled = endContentUM.isFlickering,
                textColor = if (endContentUM.isAvailable) {
                    TangemTheme.colors2.text.neutral.secondary
                } else {
                    TangemTheme.colors2.text.status.disabled
                },
            ),
        )

        when (endContentUM.priceChangeUM) {
            is PriceChangeState.Content -> TokenRowPriceChangeContent(
                priceChangeState = endContentUM.priceChangeUM,
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
private fun TokenRowEndBottomContent_Preview(
    @PreviewParameter(TokenRowEndBottomContentPreviewProvider::class) params: TangemTokenRowUM.EndContentUM,
) {
    TangemThemePreviewRedesign {
        TokenRowEndBottomContent(
            endContentUM = params,
            isBalanceHidden = false,
        )
    }
}

private class TokenRowEndBottomContentPreviewProvider : PreviewParameterProvider<TangemTokenRowUM.EndContentUM> {
    override val values: Sequence<TangemTokenRowUM.EndContentUM>
        get() = sequenceOf(
            TangemTokenRowPreviewData.bottomEndContentUM,
        )
}
// endregion