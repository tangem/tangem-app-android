package com.tangem.features.polymarket.impl.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingBenefitCard
import com.tangem.common.ui.onboarding.OnboardingFaqItem
import com.tangem.common.ui.onboarding.OnboardingHeadline
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_wallet_24
import com.tangem.core.ui.R as UiR

// Hero geometry as proportions of the design's 402pt-wide frame: the photo is 608 tall while the content
// column starts at 495, so the headline overlaps the photo's lower edge.
private const val HERO_FRAME_WIDTH = 402f
private const val HERO_IMAGE_HEIGHT = 608f
private const val HERO_CONTENT_TOP = 495f
private const val TINT_START = 380f / HERO_IMAGE_HEIGHT
private const val TINT_END = 600f / HERO_IMAGE_HEIGHT

/**
 * Scrolling body of the Welcome screen. Stateless — the footer overlaying it lives in the scaffold's overlay
 * slot and owns the legal-line reveal.
 *
 * @param trailingSpace empty scroll below the last FAQ answer: the footer's measured height plus the design's
 *  trailing gap, so the content can clear the pinned footer.
 */
@Composable
internal fun PolymarketWelcomeContent(
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    trailingSpace: Dp,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        // No top padding: the hero is full-bleed and scrolls under the status bar and the top navigation.
        Box {
            WelcomeHero()
            Column {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(HERO_FRAME_WIDTH / HERO_CONTENT_TOP),
                )
                OnboardingHeadline(
                    title = resourceReference(R.string.prediction_onboarding_title),
                    subtitle = resourceReference(R.string.prediction_onboarding_subtitle),
                )
                WelcomeBenefits()
                WelcomeFaq()
            }
        }
        Spacer(modifier = Modifier.height(trailingSpace))
    }
}

@Composable
private fun WelcomeHero(modifier: Modifier = Modifier) {
    val tintColor = TangemTheme.colors3.bg.primary

    Image(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HERO_FRAME_WIDTH / HERO_IMAGE_HEIGHT)
            .drawWithCache {
                val tint = Brush.verticalGradient(
                    colorStops = arrayOf(
                        TINT_START to Color.Transparent,
                        TINT_END to tintColor,
                        1f to tintColor,
                    ),
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(tint)
                }
            },
        painter = painterResource(UiR.drawable.img_prediction_welcome_hero),
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}

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
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_lightning_24,
                title = resourceReference(R.string.prediction_onboarding_benefit_fund_title),
                subtitle = resourceReference(R.string.prediction_onboarding_benefit_fund_subtitle),
            )
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_shield_checkmark_24,
                title = resourceReference(R.string.prediction_onboarding_benefit_custody_title),
                subtitle = resourceReference(R.string.prediction_onboarding_benefit_custody_subtitle),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_percent_backward_24,
                title = resourceReference(R.string.prediction_onboarding_benefit_history_title),
                subtitle = resourceReference(R.string.prediction_onboarding_benefit_history_subtitle),
            )
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_wallet_24,
                title = resourceReference(R.string.prediction_onboarding_benefit_payout_title),
                subtitle = resourceReference(R.string.prediction_onboarding_benefit_payout_subtitle),
            )
        }
    }
}

@Composable
private fun WelcomeFaq(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OnboardingFaqItem(
            hasTopBorder = false,
            question = resourceReference(R.string.prediction_onboarding_faq_tap_question),
            answer = resourceReference(R.string.prediction_onboarding_faq_tap_answer),
        )
        OnboardingFaqItem(
            hasTopBorder = true,
            question = resourceReference(R.string.prediction_onboarding_faq_risks_question),
            answer = resourceReference(R.string.prediction_onboarding_faq_risks_answer),
        )
        OnboardingFaqItem(
            hasTopBorder = true,
            question = resourceReference(R.string.prediction_onboarding_faq_availability_question),
            answer = resourceReference(R.string.prediction_onboarding_faq_availability_answer),
        )
    }
}