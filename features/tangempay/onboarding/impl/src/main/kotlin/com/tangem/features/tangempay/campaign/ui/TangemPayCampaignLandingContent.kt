package com.tangem.features.tangempay.campaign.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.legal.LegalCaption
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.campaign.TangemPayCampaignUM
import com.tangem.features.tangempay.common.TangemPayCampaignCardArt

@Composable
internal fun TangemPayCampaignLandingContent(state: TangemPayCampaignUM.Landing, modifier: Modifier = Modifier) {
    val isNoWallet = state.mode == TangemPayCampaignUM.Landing.Mode.NoWallet

    TangemPayCampaignLayout(
        modifier = modifier,
        topBar = { TangemPayCampaignTopBar(onCloseClick = state.onCloseClick.takeUnless { isNoWallet }) },
        card = { TangemPayCampaignCardArt(cardArtUrl = state.cardArtUrl) },
        footer = {
            TangemPayCampaignFooter(
                state = state,
                showLegalCaption = isNoWallet,
                showCtaIcon = !isNoWallet,
            )
        },
        texts = { TangemPayCampaignTexts() },
    )
}

@Composable
internal fun TangemPayCampaignTexts(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResourceSafe(R.string.tangempay_campaign_landing_title),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        TangemPayCampaignBlocks(modifier = Modifier.padding(top = 24.dp))
    }
}

@Composable
private fun TangemPayCampaignBlocks(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TangemPayCampaignBullet(
            titleRes = R.string.tangempay_onboarding_security_title,
            descriptionRes = R.string.tangempay_onboarding_security_description,
        )
        TangemPayCampaignBullet(
            titleRes = R.string.tangempay_campaign_cashback_title,
            descriptionRes = R.string.tangempay_campaign_cashback_description,
        )
        TangemPayCampaignBullet(
            titleRes = R.string.tangempay_onboarding_pay_title,
            descriptionRes = R.string.tangempay_campaign_spend_description,
        )
    }
}

@Composable
private fun TangemPayCampaignBullet(
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_information_24),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = TangemTheme.colors3.icon.primary,
        )
        Column {
            Text(
                text = stringResourceSafe(titleRes),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(descriptionRes),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun TangemPayCampaignFooter(
    state: TangemPayCampaignUM.Landing,
    showLegalCaption: Boolean,
    showCtaIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showLegalCaption) {
            LegalCaption(
                onTermsOfServiceClick = state.onTermsOfServiceClick,
                onPrivacyPolicyClick = state.onPrivacyPolicyClick,
            )
        }
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
            iconEnd = TangemIconUM.Icon(iconRes = R.drawable.ic_tangem_24).takeIf { showCtaIcon },
            isLoading = state.isCtaLoading,
            onClick = state.onGetCardClick,
        )
    }
}