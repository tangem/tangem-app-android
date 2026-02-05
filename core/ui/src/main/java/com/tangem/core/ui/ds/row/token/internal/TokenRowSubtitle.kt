package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TokenRowSubtitle(subtitleUM: TangemTokenRowUM.SubtitleUM, modifier: Modifier = Modifier) {
    when (subtitleUM) {
        is TangemTokenRowUM.SubtitleUM.Content -> SubtitleContent(
            subtitleUM = subtitleUM,
            modifier = modifier,
        )
        TangemTokenRowUM.SubtitleUM.Loading -> TextShimmer(
            style = TangemTheme.typography2.captionSemibold12,
            modifier = modifier.width(TangemTheme.dimens2.x8),
            radius = TangemTheme.dimens2.x25,
        )
        TangemTokenRowUM.SubtitleUM.Empty -> Unit
    }
}

@Composable
private fun SubtitleContent(subtitleUM: TangemTokenRowUM.SubtitleUM.Content, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = subtitleUM.badge != null,
        ) {
            val wrappedBadge = remember(this) { requireNotNull(subtitleUM.badge) }
            TangemBadge(wrappedBadge)
        }

        Text(
            text = subtitleUM.text.resolveAnnotatedReference(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography2.captionSemibold12.applyBladeBrush(
                isEnabled = subtitleUM.isFlickering,
                textColor = if (subtitleUM.isAvailable) {
                    TangemTheme.colors2.text.neutral.secondary
                } else {
                    TangemTheme.colors2.text.status.disabled
                },
            ),
        )

        when (val priceChangeUM = subtitleUM.priceChangeUM) {
            is PriceChangeState.Content -> TokenRowPriceChangeContent(
                priceChangeState = priceChangeUM,
                isFlickering = subtitleUM.isFlickering,
                isAvailable = subtitleUM.isAvailable,
            )
            PriceChangeState.Unknown -> Unit
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenRowSubtitle_Preview(
    @PreviewParameter(TokenRowSubtitlePreviewProvider::class) params: TangemTokenRowUM.SubtitleUM,
) {
    TangemThemePreviewRedesign {
        TokenRowSubtitle(
            subtitleUM = params,
        )
    }
}

private class TokenRowSubtitlePreviewProvider : PreviewParameterProvider<TangemTokenRowUM.SubtitleUM> {
    override val values: Sequence<TangemTokenRowUM.SubtitleUM>
        get() = sequenceOf(
            TangemTokenRowPreviewData.subtitleUM,
            TangemTokenRowUM.SubtitleUM.Loading,
        )
}
// endregion