package com.tangem.features.polymarket.impl.onboarding.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
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

/**
 * Pinned footer of the Welcome screen: the legal line and the start button over a blurring scrim.
 *
 * Belongs in the scaffold's overlay slot rather than its content slot — the content slot is the haze
 * source, so a blurring child of it would sample itself back as a ghost.
 *
 * The legal line is shown at all times, above the button, because consent has to be visible before the
 * action it covers is taken. It is never gated on scroll position.
 */
@Composable
internal fun PolymarketWelcomeFooter(
    state: PolymarketOnboardingUM,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current

    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            modifier = Modifier.matchParentSize(),
            position = TangemFade.Position.Bottom,
            variant = TangemFade.Variant.Hard,
            blur = true,
        )
        Column(
            modifier = Modifier.padding(
                start = 24.dp + contentPadding.calculateStartPadding(layoutDirection),
                end = 24.dp + contentPadding.calculateEndPadding(layoutDirection),
                top = 16.dp + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LegalLine(
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
        Triple(fullText.indexOf(polymarketTitle), polymarketTitle, onPolymarketTermsClick),
        Triple(fullText.indexOf(tangemTitle), tangemTitle, onTangemTermsClick),
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
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}