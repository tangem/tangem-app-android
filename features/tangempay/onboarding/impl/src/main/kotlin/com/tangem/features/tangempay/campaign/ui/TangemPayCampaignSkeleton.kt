package com.tangem.features.tangempay.campaign.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.common.TangemPayCampaignCardArtPlaceholder

private val FooterReservedHeight = 136.dp
private const val BULLET_SECOND_LINE_WIDTH_FRACTION = 0.6f

@Composable
internal fun TangemPayCampaignSkeleton(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        TangemPayCampaignLayout(
            modifier = modifier,
            topBar = { TangemPayCampaignTopBar(onCloseClick = null) },
            card = { TangemPayCampaignCardArtPlaceholder() },
            footer = { Spacer(modifier = Modifier.height(FooterReservedHeight)) },
            texts = { TangemPayCampaignTextsSkeleton() },
        )
    }
}

@Composable
private fun TangemPayCampaignTextsSkeleton(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        LandingTextsSpaceReservation()
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TangemShimmer(style = TangemTheme.typography3.heading.medium)
            BulletSkeleton()
            BulletSkeleton()
        }
    }
}

@Composable
private fun LandingTextsSpaceReservation() {
    TangemPayCampaignTexts(
        modifier = Modifier
            .alpha(0f)
            .clearAndSetSemantics {},
    )
}

@Composable
private fun BulletSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemShimmer(style = TangemTheme.typography3.subheading.medium)
        TangemShimmer(
            style = TangemTheme.typography3.subheading.medium,
            modifier = Modifier.fillMaxWidth(BULLET_SECOND_LINE_WIDTH_FRACTION),
        )
    }
}