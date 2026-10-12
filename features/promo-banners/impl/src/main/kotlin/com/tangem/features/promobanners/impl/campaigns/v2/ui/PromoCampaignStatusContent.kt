package com.tangem.features.promobanners.impl.campaigns.v2.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.userwallet.UserWalletItemRow
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignStatusUM

@Composable
internal fun PromoCampaignStatusContent(
    state: PromoCampaignStatusUM,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = state.variant,
        title = state.title,
        description = state.description,
        animated = animated,
        asset = {
            TangemStatusIcon(kind = state.icon, modifier = Modifier.size(40.dp), animated = animated)
        },
        details = when (val details = state.details) {
            is PromoCampaignStatusUM.Details.EnrolledWallets -> {
                { EnrolledWalletsDetails(details) }
            }
            is PromoCampaignStatusUM.Details.Recipient -> {
                { RecipientDetails(details) }
            }
            null -> null
        },
        footer = {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = state.buttonText,
                onClick = state.onButtonClick,
            )
        },
    )
}

@Composable
private fun EnrolledWalletsDetails(details: PromoCampaignStatusUM.Details.EnrolledWallets) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = stringResourceSafe(R.string.promo_campaign_enrolled_wallets),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        // The status screen pins details above the footer without scrolling, so a long wallet list scrolls itself.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .background(color = TangemTheme.colors3.bg.opaque.primary, shape = RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            details.wallets.forEach { wallet ->
                UserWalletItemRow(
                    state = wallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RecipientDetails(details: PromoCampaignStatusUM.Details.Recipient) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors3.bg.opaque.primary, shape = RoundedCornerShape(24.dp)),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = details.walletName.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            details.tokenIcon?.let { icon ->
                CurrencyIcon(state = icon, iconSize = 36.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResourceSafe(R.string.send_recipient),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
                Text(
                    text = details.address,
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignStatusContent(
    @PreviewParameter(PromoCampaignStatusProvider::class) state: PromoCampaignStatusUM,
) {
    TangemThemePreviewRedesign {
        PromoCampaignStatusContent(state = state, animated = false)
    }
}

private class PromoCampaignStatusProvider : CollectionPreviewParameterProvider<PromoCampaignStatusUM>(
    collection = PromoCampaignPreviewData.statuses,
)
// endregion