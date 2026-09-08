package com.tangem.features.tangempay.campaign.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.campaign.TangemPayCampaignUM

@Composable
internal fun TangemPayCampaignScreen(state: TangemPayCampaignUM, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayCampaignUM.Loading -> TangemPayCampaignSkeleton(modifier = modifier)
        is TangemPayCampaignUM.Error -> TangemPayCampaignErrorContent(state = state, modifier = modifier)
        is TangemPayCampaignUM.Landing -> TangemPayCampaignLandingContent(state = state, modifier = modifier)
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCampaignScreenPreview(
    @PreviewParameter(TangemPayCampaignUMProvider::class) state: TangemPayCampaignUM,
) {
    TangemThemePreviewRedesign {
        TangemPayCampaignScreen(state = state)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCampaignCardArtPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemPayCampaignCardArt(cardArtUrl = PREVIEW_CARD_ART_URL)
            TangemPayCampaignCardArtLoading(modifier = previewCardArtModifier())
            TangemPayCampaignCardArtError(onRetryClick = {}, modifier = previewCardArtModifier())
        }
    }
}

private fun previewCardArtModifier(): Modifier = Modifier
    .width(CampaignCardArtWidth)
    .aspectRatio(CAMPAIGN_CARD_ART_ASPECT_RATIO)
    .clip(RoundedCornerShape(CampaignCardArtCornerRadius))

private const val PREVIEW_CARD_ART_URL = "https://app.tangem.com/cards/card_default.png"

private class TangemPayCampaignUMProvider : CollectionPreviewParameterProvider<TangemPayCampaignUM>(
    collection = listOf(
        TangemPayCampaignUM.Loading,
        TangemPayCampaignUM.Error(onRetry = {}, onClose = {}),
        previewLanding(mode = TangemPayCampaignUM.Landing.Mode.NoWallet, isCtaLoading = false),
        previewLanding(mode = TangemPayCampaignUM.Landing.Mode.NoPay, isCtaLoading = true),
    ),
)

private fun previewLanding(mode: TangemPayCampaignUM.Landing.Mode, isCtaLoading: Boolean) = TangemPayCampaignUM.Landing(
    cardArtUrl = PREVIEW_CARD_ART_URL,
    mode = mode,
    isCtaLoading = isCtaLoading,
    onGetCardClick = {},
    onTermsClick = {},
    onCloseClick = {},
)