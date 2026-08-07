package com.tangem.features.polymarket.impl.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.stringResourceSafe
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

private val ScreenPadding = 24.dp
private val HeadlineVerticalPadding = 12.dp
private val HeadlineGap = 8.dp
private val BenefitGap = 8.dp
private val BenefitCardPadding = 16.dp
private val BenefitCardRadius = 24.dp
private val BenefitContentMinHeight = 100.dp
private val IconSize = 24.dp
private val FaqGap = 12.dp
private val FaqItemTopPadding = 24.dp
private val FaqItemBottomPadding = 12.dp
private val FaqRuleHeight = 1.dp

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
                WelcomeHeadline()
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
private fun WelcomeHeadline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = ScreenPadding, vertical = HeadlineVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(HeadlineGap),
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

@Composable
private fun WelcomeBenefits(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(all = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BenefitGap),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(BenefitGap),
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
            horizontalArrangement = Arrangement.spacedBy(BenefitGap),
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

@Composable
private fun BenefitCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(BenefitCardRadius))
            .padding(
                start = BenefitCardPadding,
                end = ScreenPadding,
                top = BenefitCardPadding,
                bottom = BenefitCardPadding,
            )
            .heightIn(min = BenefitContentMinHeight),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            modifier = Modifier.size(IconSize),
            imageVector = icon,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
        Column {
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

@Composable
private fun WelcomeFaq(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(FaqGap),
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

@Composable
private fun FaqItem(hasTopBorder: Boolean, question: String, answer: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FaqItemBottomPadding),
    ) {
        if (hasTopBorder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FaqRuleHeight)
                    .background(TangemTheme.colors3.border.secondary),
            )
        }
        Column(
            modifier = Modifier.padding(top = FaqItemTopPadding),
            verticalArrangement = Arrangement.spacedBy(FaqGap),
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