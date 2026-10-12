package com.tangem.features.tangempay.campaign.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.campaign.TangemPayCampaignUM

@Composable
internal fun TangemPayCampaignErrorContent(state: TangemPayCampaignUM.Error, modifier: Modifier = Modifier) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Error,
        title = resourceReference(R.string.tangempay_campaign_error_title),
        description = resourceReference(R.string.tangempay_campaign_error_description),
        asset = {
            TangemStatusIcon(kind = TangemStatusIcon.Kind.HeartBroken, modifier = Modifier.size(40.dp))
        },
        footer = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_retry),
                    onClick = state.onRetry,
                )
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_close),
                    onClick = state.onClose,
                )
            }
        },
    )
}