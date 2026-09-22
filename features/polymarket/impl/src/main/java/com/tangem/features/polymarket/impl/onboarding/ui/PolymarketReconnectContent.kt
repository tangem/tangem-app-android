package com.tangem.features.polymarket.impl.onboarding.ui

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
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24

/**
 * Scrolling body of the Reconnect screen. Stateless — the footer overlaying it lives in the scaffold's overlay
 * slot.
 *
 * @param trailingSpace empty scroll below the answer: the footer's measured height plus the design's trailing
 *  gap, so the content can clear the pinned footer.
 */
@Composable
internal fun PolymarketReconnectContent(
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
        OnboardingHero(imageResId = UiR.drawable.img_prediction_welcome_hero) {
            OnboardingHeadline(
                title = resourceReference(R.string.prediction_onboarding_reconnect_title),
                subtitle = resourceReference(R.string.prediction_onboarding_reconnect_subtitle),
            )
            ReconnectReassurance()
            ReconnectFaq()
        }
        Spacer(modifier = Modifier.height(trailingSpace))
    }
}

@Composable
private fun ReconnectReassurance(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(all = 24.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OnboardingBenefitCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            icon = Icons.ic_lightning_24,
            title = resourceReference(R.string.prediction_onboarding_reconnect_benefit_tap_title),
            subtitle = resourceReference(R.string.prediction_onboarding_reconnect_benefit_tap_subtitle),
        )
        OnboardingBenefitCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            icon = Icons.ic_shield_checkmark_24,
            title = resourceReference(R.string.prediction_onboarding_reconnect_benefit_safe_title),
            subtitle = resourceReference(R.string.prediction_onboarding_reconnect_benefit_safe_subtitle),
        )
    }
}

@Composable
private fun ReconnectFaq(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        OnboardingFaqItem(
            hasTopBorder = false,
            question = resourceReference(R.string.prediction_onboarding_reconnect_faq_question),
            answer = resourceReference(R.string.prediction_onboarding_reconnect_faq_answer),
        )
    }
}