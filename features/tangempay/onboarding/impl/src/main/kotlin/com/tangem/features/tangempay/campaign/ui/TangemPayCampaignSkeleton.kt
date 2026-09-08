package com.tangem.features.tangempay.campaign.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer

private val FooterReservedHeight = 136.dp

@Composable
internal fun TangemPayCampaignSkeleton(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        TangemPayCampaignLayout(
            modifier = modifier,
            topBar = { TangemPayCampaignTopBar(onCloseClick = null) },
            card = {
                TangemShimmer(
                    modifier = Modifier
                        .width(CampaignCardArtWidth)
                        .aspectRatio(CAMPAIGN_CARD_ART_ASPECT_RATIO),
                    radius = CampaignCardArtCornerRadius,
                )
            },
            footer = { Spacer(modifier = Modifier.height(FooterReservedHeight)) },
            texts = {
                TangemShimmer(modifier = Modifier.size(width = 236.dp, height = 28.dp), radius = 8.dp)
                Spacer(modifier = Modifier.height(32.dp))
                BulletSkeleton()
                Spacer(modifier = Modifier.height(28.dp))
                BulletSkeleton()
            },
        )
    }
}

@Composable
private fun BulletSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TangemShimmer(modifier = Modifier.size(width = 134.dp, height = 10.dp), radius = 5.dp)
        TangemShimmer(modifier = Modifier.size(width = 76.dp, height = 10.dp), radius = 5.dp)
    }
}