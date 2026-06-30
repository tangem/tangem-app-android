package com.tangem.features.virtualaccount.common.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.shimmers.TextShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.utils.StringsSigns.DASH_SIGN

@Composable
fun TangemBalanceHeader(
    state: TangemBalanceHeaderState,
    label: TextReference,
    modifier: Modifier = Modifier,
    balanceModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        AnimatedContent(
            targetState = state,
            label = "Updating the balance",
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 90))
            },
        ) { animatedState ->
            when (animatedState) {
                is TangemBalanceHeaderState.Loading -> TextShimmer(
                    modifier = Modifier.size(width = 160.dp, height = 56.dp),
                    text = "1234.00",
                    style = TextShimmerStyle.HEADING_MEDIUM,
                    radius = TangemTheme.dimens2.x25,
                )
                is TangemBalanceHeaderState.Content -> Text(
                    modifier = balanceModifier,
                    text = animatedState.balance
                        .orMaskWithStars(animatedState.isBalanceHidden)
                        .resolveAnnotatedReference(),
                    style = TangemTheme.typography3.display.medium.applyBladeBrush(
                        isEnabled = animatedState.isFlickering,
                        textColor = TangemTheme.colors3.text.primary,
                    ),
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography3.heading.medium.fontSize,
                        maxFontSize = TangemTheme.typography3.display.medium.fontSize,
                    ),
                )
                is TangemBalanceHeaderState.Error -> Text(
                    modifier = balanceModifier,
                    text = DASH_SIGN,
                    style = TangemTheme.typography3.display.medium,
                    color = TangemTheme.colors3.text.primary,
                )
            }
        }
        Text(
            modifier = Modifier.padding(vertical = TangemTheme.dimens2.x1),
            text = label.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemBalanceHeaderPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemBalanceHeader(
                modifier = Modifier.fillMaxWidth(),
                state = TangemBalanceHeaderState.Content(
                    balance = stringReference("$0.00"),
                    isBalanceHidden = false,
                ),
                label = stringReference("Total balance"),
            )
            TangemBalanceHeader(
                modifier = Modifier.fillMaxWidth(),
                state = TangemBalanceHeaderState.Loading,
                label = stringReference("Total balance"),
            )
            TangemBalanceHeader(
                modifier = Modifier.fillMaxWidth(),
                state = TangemBalanceHeaderState.Error,
                label = stringReference("Total balance"),
            )
        }
    }
}