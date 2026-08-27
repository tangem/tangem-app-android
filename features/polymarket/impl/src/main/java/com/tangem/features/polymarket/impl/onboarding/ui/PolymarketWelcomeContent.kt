package com.tangem.features.polymarket.impl.onboarding.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_wallet_24
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM

@Suppress("MagicNumber")
@Composable
internal fun PolymarketWelcomeContent(
    state: PolymarketOnboardingUM,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = contentPadding.calculateStartPadding(layoutDirection)
    val endPadding = contentPadding.calculateEndPadding(layoutDirection)
    val scrollState = rememberScrollState()
    var footerHeight by remember { mutableIntStateOf(0) }

    val isScrolledToEnd by remember { derivedStateOf { !scrollState.canScrollForward } }
    val legalAlpha by animateFloatAsState(
        targetValue = if (isScrolledToEnd) 1f else 0f,
        label = "legalLineAlpha",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = contentPadding.calculateTopPadding(), start = startPadding, end = endPadding),
        ) {
            WelcomeHero()
            WelcomeHeadline()
            WelcomeBenefits()
            WelcomeFaq()
            Spacer(modifier = Modifier.height(with(LocalDensity.current) { footerHeight.toDp() }))
        }

        WelcomeFooter(
            state = state,
            legalAlpha = legalAlpha,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { footerHeight = it.height },
            bottomPadding = contentPadding.calculateBottomPadding(),
            startPadding = startPadding,
            endPadding = endPadding,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun WelcomeHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TangemTheme.colors3.bg.secondary,
                        TangemTheme.colors3.bg.primary,
                    ),
                ),
            ),
    )
}

@Suppress("MagicNumber")
@Composable
private fun WelcomeHeadline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.prediction_onboarding_title),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = stringResourceSafe(R.string.prediction_onboarding_subtitle),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun WelcomeBenefits(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_lightning_24,
                title = stringResourceSafe(R.string.prediction_onboarding_benefit_fund_title),
                subtitle = stringResourceSafe(R.string.prediction_onboarding_benefit_fund_subtitle),
            )
            BenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_shield_checkmark_24,
                title = stringResourceSafe(R.string.prediction_onboarding_benefit_custody_title),
                subtitle = stringResourceSafe(R.string.prediction_onboarding_benefit_custody_subtitle),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_percent_backward_24,
                title = stringResourceSafe(R.string.prediction_onboarding_benefit_history_title),
                subtitle = stringResourceSafe(R.string.prediction_onboarding_benefit_history_subtitle),
            )
            BenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_wallet_24,
                title = stringResourceSafe(R.string.prediction_onboarding_benefit_payout_title),
                subtitle = stringResourceSafe(R.string.prediction_onboarding_benefit_payout_subtitle),
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun BenefitCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = subtitle,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun WelcomeFaq(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FaqItem(
            hasTopBorder = false,
            question = stringResourceSafe(R.string.prediction_onboarding_faq_tap_question),
            answer = stringResourceSafe(R.string.prediction_onboarding_faq_tap_answer),
        )
        FaqItem(
            hasTopBorder = true,
            question = stringResourceSafe(R.string.prediction_onboarding_faq_risks_question),
            answer = stringResourceSafe(R.string.prediction_onboarding_faq_risks_answer),
        )
        FaqItem(
            hasTopBorder = true,
            question = stringResourceSafe(R.string.prediction_onboarding_faq_availability_question),
            answer = stringResourceSafe(R.string.prediction_onboarding_faq_availability_answer),
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun FaqItem(hasTopBorder: Boolean, question: String, answer: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (hasTopBorder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TangemTheme.colors3.border.primary),
            )
        }
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = question,
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = answer,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun WelcomeFooter(
    state: PolymarketOnboardingUM,
    legalAlpha: Float,
    bottomPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            modifier = Modifier.matchParentSize(),
            position = TangemFade.Position.Bottom,
        )
        Column(
            modifier = Modifier.padding(
                start = 16.dp + startPadding,
                end = 16.dp + endPadding,
                bottom = bottomPadding + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val isLegalVisible = legalAlpha > 0f

            LegalLine(
                modifier = Modifier.alpha(legalAlpha),
                isVisible = isLegalVisible,
                onPolymarketTermsClick = state.onPolymarketTermsClick,
                onTangemTermsClick = state.onTangemTermsClick,
            )
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X10,
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

@Preview(widthDp = 360, heightDp = 900, showBackground = true)
@Preview(widthDp = 360, heightDp = 900, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketWelcomeContentPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PolymarketWelcomeContent(
                state = PolymarketOnboardingUM(
                    isStarting = false,
                    startButtonText = resourceReference(R.string.prediction_onboarding_start_button),
                    onStartClick = {},
                    onPolymarketTermsClick = {},
                    onTangemTermsClick = {},
                ),
                contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 900, showBackground = true)
@Preview(widthDp = 360, heightDp = 900, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketWelcomeContentStartingPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PolymarketWelcomeContent(
                state = PolymarketOnboardingUM(
                    isStarting = true,
                    startButtonText = resourceReference(R.string.prediction_onboarding_start_button),
                    onStartClick = {},
                    onPolymarketTermsClick = {},
                    onTangemTermsClick = {},
                ),
                contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            )
        }
    }
}