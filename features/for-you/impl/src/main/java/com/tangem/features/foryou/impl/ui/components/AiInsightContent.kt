package com.tangem.features.foryou.impl.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.CanvasGradientDivider
import com.tangem.features.foryou.impl.components.state.AiInsightUM

@Suppress("ModifierHeightWithText")
@Composable
internal fun AiInsightContent(aiInsightUM: AiInsightUM, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = aiInsightUM,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { currentState ->
        when (currentState) {
            is AiInsightUM.AskAiInsight -> {
                SecondaryTangemButton(
                    modifier = modifier
                        .fillMaxWidth(),
                    onClick = currentState.askAiInsightClick,
                    size = TangemButtonSize.X9,
                    text = resourceReference(R.string.market_chart_ask_for_ai_summary_button),
                )
            }
            is AiInsightUM.Displayed -> {
                Row(
                    modifier = modifier
                        .height(IntrinsicSize.Min),
                ) {
                    CanvasGradientDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 2.dp),
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            TangemTheme.colors3.icon.accent.violet,
                                            TangemTheme.colors3.icon.accent.blue,
                                        ),
                                    ),
                                    alpha = 1f,
                                ),
                            ) { append(stringResourceSafe(R.string.market_chart_ai_total)) }
                            append(" ")
                            append(currentState.text)
                        },
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                }
            }
            AiInsightUM.Hide -> {}
        }
    }
}