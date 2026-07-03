package com.tangem.features.tangempay.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayInsufficientFundsNotification(onAddFundsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.status.warningSubtle)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.img_usdc_16),
                contentDescription = null,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResourceSafe(R.string.tangempay_reissue_card_insufficient_funds_title),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                Text(
                    text = stringResourceSafe(R.string.tangempay_reissue_card_insufficient_funds_subtitle),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X8,
            onClick = onAddFundsClick,
            text = resourceReference(R.string.tangempay_card_details_add_funds),
        )
    }
}