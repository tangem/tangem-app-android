package com.tangem.features.tangempay.campaign.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.campaign.TangemPayCampaignUM
import com.tangem.features.tangempay.ui.TangemPayOnboardingBlock

@Composable
internal fun TangemPayCampaignLandingContent(state: TangemPayCampaignUM.Landing, modifier: Modifier = Modifier) {
    val onCloseClick = state.onCloseClick.takeIf { state.mode == TangemPayCampaignUM.Landing.Mode.NoPay }

    TangemPayCampaignLayout(
        modifier = modifier,
        topBar = { TangemPayCampaignTopBar(onCloseClick = onCloseClick) },
        card = { TangemPayCampaignCardArt(cardArtUrl = state.cardArtUrl) },
        footer = {
            TangemPayCampaignFooter(
                isCtaLoading = state.isCtaLoading,
                onGetCardClick = state.onGetCardClick,
                onTermsClick = state.onTermsClick,
            )
        },
        texts = {
            Text(
                text = stringResourceSafe(R.string.tangempay_campaign_landing_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            TangemPayCampaignBlocks(modifier = Modifier.padding(top = 24.dp))
        },
    )
}

@Composable
private fun TangemPayCampaignBlocks(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_security_check_24,
            titleRef = resourceReference(R.string.tangempay_onboarding_security_title),
            descriptionRef = resourceReference(R.string.tangempay_onboarding_security_description),
        )
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_shopping_basket_24,
            titleRef = resourceReference(R.string.tangempay_campaign_fees_title),
            descriptionRef = resourceReference(R.string.tangempay_campaign_fees_description),
        )
        TangemPayOnboardingBlock(
            painterRes = R.drawable.ic_credit_card_add_24,
            titleRef = resourceReference(R.string.tangempay_onboarding_pay_title),
            descriptionRef = resourceReference(R.string.tangempay_campaign_pay_description),
        )
    }
}

@Composable
private fun TangemPayCampaignFooter(
    isCtaLoading: Boolean,
    onGetCardClick: () -> Unit,
    onTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangem_pay_terms_fees_limits),
            onClick = onTermsClick,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
            iconEnd = TangemIconUM.Icon(iconRes = R.drawable.ic_tangem_24),
            isLoading = isCtaLoading,
            onClick = onGetCardClick,
        )
    }
}