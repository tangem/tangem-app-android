package com.tangem.features.promobanners.impl.campaigns.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.features.promobanners.impl.R
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_warning_24

@Composable
fun NotActiveCampaignMessageContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.status.warningSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.ic_warning_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.warning,
            )
        }

        SpacerH32()

        Text(
            text = stringResourceSafe(R.string.promo_campaign_not_active_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        SpacerH8()

        Text(
            text = stringResourceSafe(R.string.promo_campaign_not_active_subtitle),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}