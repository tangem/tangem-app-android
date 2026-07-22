package com.tangem.features.foryou.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.ui.components.ForYouPortfolioTokenList
import com.tangem.features.foryou.impl.ui.preview.ForYouEarnOpportunitiesPreviewData

private const val INLINE_CONTENT_PADDING_COEF = 2.2f

@Composable
internal fun ForYouEarnOpportunities(earnOpportunitiesUM: EarnOpportunitiesUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResourceSafe(R.string.for_you_earn_opportunities),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.secondary,
        )

        when (earnOpportunitiesUM) {
            is EarnOpportunitiesUM.Content -> {
                val (textContent, inlineContent) = if (earnOpportunitiesUM.potentialReward != null) {
                    val potentialReward = earnOpportunitiesUM.potentialReward.resolveReference()

                    val inlineContent = rememberEarnInlineContent(potentialReward)

                    val textContent = resourceReference(
                        earnOpportunitiesUM.subtitleRes,
                        wrappedList(
                            annotatedReference {
                                if (earnOpportunitiesUM.potentialRewardType != null) {
                                    append(earnOpportunitiesUM.potentialRewardType.resolveAnnotatedReference())
                                    appendSpace()
                                }
                                appendInlineContent(potentialReward, potentialReward)
                            },
                        ),
                    )

                    textContent to inlineContent
                } else {
                    resourceReference(earnOpportunitiesUM.subtitleRes) to emptyMap()
                }

                Text(
                    text = textContent.resolveAnnotatedReference(),
                    inlineContent = inlineContent,
                    style = TangemTheme.typography3.heading.small,
                    color = TangemTheme.colors3.text.primary,
                )
            }
            is EarnOpportunitiesUM.Loading -> {
                TangemShimmer(
                    style = TangemTheme.typography3.heading.small,
                )
            }
        }

        ForYouPortfolioTokenList(
            tokenList = earnOpportunitiesUM.tokenList,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (earnOpportunitiesUM is EarnOpportunitiesUM.Content) {
            TangemButton(
                text = stringReference("Explore all tokens"), // todo FOR YOU lokalize
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X9,
                isEnabled = true,
                contentDescription = "Explore all tokens", // todo FOR YOU lokalize
                onClick = earnOpportunitiesUM.onAllEarnTokensClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun rememberEarnInlineContent(potentialReward: String): Map<String, InlineTextContent> {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val horizontalPadding = 2.dp

    val potentialStyle = TangemTheme.typography3.heading.small

    val potentialLabelBgWidthSp = remember(potentialReward) {
        val textLayoutMeasure = textMeasurer.measure(
            text = potentialReward,
            style = potentialStyle,
        )
        with(density) {
            (textLayoutMeasure.size.width.toDp() + (horizontalPadding.value * INLINE_CONTENT_PADDING_COEF).dp).toSp()
        }
    }

    return remember(potentialReward, potentialLabelBgWidthSp) {
        mapOf(
            potentialReward to InlineTextContent(
                Placeholder(
                    width = potentialLabelBgWidthSp,
                    height = potentialStyle.lineHeight,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Text(
                    text = potentialReward,
                    style = potentialStyle,
                    color = TangemTheme.colors3.text.primary,
                    modifier = Modifier
                        .background(
                            color = TangemTheme.colors3.icon.brand,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = horizontalPadding),
                )
            },
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ForYouEarnOpportunities_Preview(
    @PreviewParameter(ForYouEarnOpportunitiesPreviewProvider::class) params: EarnOpportunitiesUM,
) {
    TangemThemePreviewRedesign {
        ForYouEarnOpportunities(
            earnOpportunitiesUM = params,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private class ForYouEarnOpportunitiesPreviewProvider : PreviewParameterProvider<EarnOpportunitiesUM> {
    override val values: Sequence<EarnOpportunitiesUM>
        get() = sequenceOf(
            ForYouEarnOpportunitiesPreviewData.tokensRewards,
            ForYouEarnOpportunitiesPreviewData.noAvailableTokens,
            ForYouEarnOpportunitiesPreviewData.allTokensActive,
            ForYouEarnOpportunitiesPreviewData.loading,
        )
}
// endregion