package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dot_12_filled
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM

/** How far below its place each row starts; measured off the demo render. */
private val CHROME_RISE = 30.dp

/** The fade is done in the first 250 of the 450 ms the rise takes. */
private const val CHROME_FADE_SHARE = 250f / 450f

/** Ease-out quad. */
private val RISE_EASING = CubicBezierEasing(a = 0.25f, b = 0.46f, c = 0.45f, d = 0.94f)

/** Ease-out cubic. */
private val FADE_EASING = CubicBezierEasing(a = 0.33f, b = 1f, c = 0.68f, d = 1f)

/** Opacity of the title, the footer and the back button at linear [progress] of the chrome clock. */
internal fun chromeAlpha(progress: Float): Float =
    FADE_EASING.transform((progress / CHROME_FADE_SHARE).coerceIn(0f, 1f))

@Composable
internal fun RewardTitle(um: RewardCardUM, progress: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChromeRow(progress = progress) {
            TangemBadge(
                text = stringReference(um.rarity),
                variant = TangemBadge.Variant.Tinted,
                status = um.accent.badgeStatus(),
                size = TangemBadge.Size.X6,
                iconStart = TangemIconUM.Icon(Icons.ic_dot_12_filled),
            )
        }

        ChromeRow(progress = progress) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = titleText(um),
                    style = TangemTheme.typography3.heading.medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography3.heading.small.fontSize,
                        maxFontSize = TangemTheme.typography3.heading.medium.fontSize,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Market value ${um.marketValue} · Insured",
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun titleText(um: RewardCardUM): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = TangemTheme.colors3.text.primary)) { append(um.title) }
    append(" ")
    withStyle(SpanStyle(color = TangemTheme.colors3.text.secondary)) { append(um.editionNumber) }
}

@Composable
internal fun RewardFooter(
    um: RewardCardUM,
    progress: Float,
    onKeepCardClick: () -> Unit,
    onSellBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemFooter(modifier = modifier, fadeEnabled = false, blurBackground = false) {
        ChromeRow(progress = progress) {
            TangemButtonGroup {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = stringReference("Keep card"),
                    onClick = onKeepCardClick,
                )
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = stringReference("Sell back for ${um.sellBackValue}"),
                    iconEnd = TangemIconUM.Icon(Icons.ic_logo_tangem_24),
                    onClick = onSellBackClick,
                )
            }
        }
    }
}

@Composable
private fun ChromeRow(progress: Float, content: @Composable () -> Unit) {
    val rise = RISE_EASING.transform(progress.coerceIn(0f, 1f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = chromeAlpha(progress)
                translationY = CHROME_RISE.toPx() * (1f - rise)
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun RewardCardUM.Accent.badgeStatus(): TangemBadge.Status = when (this) {
    RewardCardUM.Accent.Neutral -> TangemBadge.Status.Neutral
    RewardCardUM.Accent.Green -> TangemBadge.Status.Success
    RewardCardUM.Accent.Blue -> TangemBadge.Status.Info
    // TODO([REDACTED_TASK_KEY]): the design's epic badge is a placeholder copy of rare; the badge has no violet status
    RewardCardUM.Accent.Violet -> TangemBadge.Status.Info
}