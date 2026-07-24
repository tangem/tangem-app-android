package com.tangem.features.foryou.impl.tokensummaryblock.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummaryblock.entity.TokenSummaryBlockUM
import com.tangem.features.foryou.impl.ui.components.AiInsightContent
import com.tangem.features.foryou.impl.ui.components.GradientScaleBar
import com.tangem.features.foryou.impl.ui.components.GradientScaleBarState
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TokenSummaryBlock(state: TokenSummaryBlockUM, modifier: Modifier = Modifier) {
    when (val sentiment = state.sentiment) {
        is TokenSentimentUM.Content -> {
            SentimentsContent(
                tokenSentiment = sentiment,
                aiInsight = state.aiInsight,
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(TangemTheme.colors3.bg.secondary)
                    .clickableSingle(onClick = state.onClick)
                    .padding(16.dp),
            )
        }
        TokenSentimentUM.Empty,
        TokenSentimentUM.Loading,
        -> Unit
    }
}

@Composable
private fun SentimentsContent(
    tokenSentiment: TokenSentimentUM.Content,
    aiInsight: AiInsightUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResourceSafe(R.string.token_summary_title),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        SpacerH(4.dp)

        Text(
            text = tokenSentiment.sentiment.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
        )

        SpacerH(20.dp)

        GradientScaleBar(
            state = GradientScaleBarState.Content(value = tokenSentiment.totalScore),
        )

        if (aiInsight !is AiInsightUM.Hide) SpacerH(20.dp)

        AiInsightContent(aiInsightUM = aiInsight)
    }
}

// region Preview

private fun previewBlockState(aiInsight: AiInsightUM): TokenSummaryBlockUM = TokenSummaryBlockUM(
    sentiment = TokenSentimentUM.Content(
        sentiment = stringReference("Positive outlook"),
        lastUpdate = stringReference("Updated Jan 20 2026, 9:24 PM"),
        totalScore = 5,
        indicators = persistentListOf(),
    ),
    aiInsight = aiInsight,
    onClick = {},
)

private val previewAiInsight = AiInsightUM.Displayed(
    "Imagine a car rolling downhill at high speed: almost every gauge says \"going down\".",
)

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryBlockPreview() {
    TangemThemePreviewRedesign {
        TokenSummaryBlock(
            state = previewBlockState(aiInsight = previewAiInsight),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "No AI insight · Light", showBackground = true, widthDp = 360)
@Preview(name = "No AI insight · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryBlockNoAiInsightPreview() {
    TangemThemePreviewRedesign {
        TokenSummaryBlock(
            state = previewBlockState(aiInsight = AiInsightUM.Hide),
            modifier = Modifier.padding(16.dp),
        )
    }
}
// endregion