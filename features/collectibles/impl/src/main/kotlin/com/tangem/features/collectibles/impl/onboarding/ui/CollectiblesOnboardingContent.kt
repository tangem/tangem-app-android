package com.tangem.features.collectibles.impl.onboarding.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingBenefitCard
import com.tangem.common.ui.onboarding.OnboardingFaqItem
import com.tangem.common.ui.onboarding.OnboardingHeadline
import com.tangem.common.ui.onboarding.OnboardingHero
import com.tangem.core.ui.R as UiR
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Scrolling body of the Onboarding screen. Stateless — the pinned footer lives in the scaffold's overlay slot.
 *
 * @param scrollState drives the top bar's scrim, so it is owned by the caller.
 * @param contentPadding safe-area padding from the scaffold; the top inset is deliberately not applied —
 *   the hero area scrolls under the status bar and the top navigation, as in the design.
 * @param trailingSpace empty scroll below the last FAQ answer: the footer's measured height plus the
 *   design's trailing gap, so the content can clear the pinned footer.
 * @param modifier applied to the scrolling root.
 */
@Composable
internal fun CollectiblesOnboardingContent(
    state: CollectiblesOnboardingUM,
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
        OnboardingHero(imageResId = UiR.drawable.img_collectibles_welcome_hero) {
            OnboardingHeadline(title = state.title, subtitle = state.subtitle)
            OnboardingBenefits(benefits = state.benefits)
            OnboardingFaq(faq = state.faq)
        }
        Spacer(modifier = Modifier.height(trailingSpace))
    }
}

@Composable
private fun OnboardingBenefits(
    benefits: ImmutableList<CollectiblesOnboardingUM.Benefit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        benefits.chunked(size = 2).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { benefit ->
                    OnboardingBenefitCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        icon = benefit.icon,
                        title = benefit.title,
                        subtitle = benefit.subtitle,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFaq(faq: ImmutableList<CollectiblesOnboardingUM.FaqEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        faq.forEachIndexed { index, entry ->
            OnboardingFaqItem(
                hasTopBorder = index != 0,
                question = entry.question,
                answer = entry.answer,
            )
        }
    }
}