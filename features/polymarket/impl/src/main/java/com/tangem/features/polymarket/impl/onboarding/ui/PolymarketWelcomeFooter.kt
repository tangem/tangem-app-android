package com.tangem.features.polymarket.impl.onboarding.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM

private val HorizontalPadding = 24.dp
private val VerticalPadding = 12.dp
private val ContentGap = 12.dp

/** How far the scrim reaches above the footer's content, per the design's fade offset. */
private val ScrimOvershoot = 16.dp

/**
 * Pinned footer of the Welcome screen: the legal line and the start button over a blurring scrim.
 *
 * Belongs in the scaffold's overlay slot rather than its content slot — the content slot is the haze
 * source, so a blurring child of it would sample itself back as a ghost.
 *
 * @param revealThreshold how much scroll may remain before the legal line fades in. The line stays laid out
 *  at all times and only its alpha animates: the scroll spacer is sized from this footer's measured height,
 *  so a height that varied with the reveal would oscillate.
 */
@Composable
internal fun PolymarketWelcomeFooter(
    state: PolymarketOnboardingUM,
    scrollState: ScrollState,
    revealThreshold: Dp,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val thresholdPx = with(LocalDensity.current) { revealThreshold.roundToPx() }

    // maxValue is Int.MAX_VALUE until the content is measured, which would otherwise reveal on the first frame.
    val isLegalRevealed by remember(scrollState, thresholdPx) {
        derivedStateOf {
            scrollState.maxValue != Int.MAX_VALUE && scrollState.maxValue - scrollState.value <= thresholdPx
        }
    }
    val legalAlpha by animateFloatAsState(
        targetValue = if (isLegalRevealed) 1f else 0f,
        label = "legalLineAlpha",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            modifier = Modifier.matchParentSize(),
            position = TangemFade.Position.Bottom,
            variant = TangemFade.Variant.Hard,
            blur = true,
        )
        Column(
            modifier = Modifier.padding(
                start = HorizontalPadding + contentPadding.calculateStartPadding(layoutDirection),
                end = HorizontalPadding + contentPadding.calculateEndPadding(layoutDirection),
                top = ScrimOvershoot + VerticalPadding,
                bottom = contentPadding.calculateBottomPadding() + VerticalPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(ContentGap),
        ) {
            LegalLine(
                modifier = Modifier.alpha(legalAlpha),
                isVisible = legalAlpha > 0f,
                onPolymarketTermsClick = state.onPolymarketTermsClick,
                onTangemTermsClick = state.onTangemTermsClick,
            )
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                variant = TangemButton.Variant.Primary,
                isLoading = state.isStarting,
                iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24),
                text = state.startButtonText,
                contentDescription = if (state.isStarting) {
                    stringResourceSafe(R.string.common_in_progress)
                } else {
                    null
                },
                onClick = state.onStartClick,
            )
        }
    }
}

@Composable
private fun LegalLine(
    isVisible: Boolean,
    onPolymarketTermsClick: () -> Unit,
    onTangemTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)
    val polymarketTitle = stringResourceSafe(R.string.prediction_onboarding_legal_polymarket_terms)
    val tangemTitle = stringResourceSafe(R.string.prediction_onboarding_legal_tangem_terms)
    val fullText = stringResourceSafe(R.string.prediction_onboarding_legal, polymarketTitle, tangemTitle)

    // Locate each link title in the resolved (localized) string and splice them in appearance order.
    // Handles translations that reorder the %1$s/%2$s placeholders and skips a title that a translation
    // does not contain verbatim — falling back to plain text instead of crashing on an invalid substring range.
    val links = listOf(
        Triple(fullText.indexOf(polymarketTitle), polymarketTitle) { if (isVisible) onPolymarketTermsClick() },
        Triple(fullText.indexOf(tangemTitle), tangemTitle) { if (isVisible) onTangemTermsClick() },
    )
        .filter { it.first >= 0 }
        .sortedBy { it.first }

    val text = buildAnnotatedString {
        var cursor = 0
        links.forEach { (index, title, onClick) ->
            if (index < cursor) return@forEach
            append(fullText.substring(cursor, index))
            withLink(LinkAnnotation.Clickable(tag = title, linkInteractionListener = { onClick() })) {
                withStyle(linkStyle) { append(title) }
            }
            cursor = index + title.length
        }
        append(fullText.substring(cursor))
    }
    Text(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isVisible) Modifier else Modifier.clearAndSetSemantics { }),
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}