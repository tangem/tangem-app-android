package com.tangem.features.promobanners.impl.campaigns.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM

@Composable
internal fun ActivateCampaignContent(um: ActivateCampaignUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemIcon(
            tangemIconUM = um.logo,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
        )

        SpacerH32()

        Text(
            text = um.title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        SpacerH8()

        Text(
            text = um.description.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        SpacerH12()

        Text(
            text = stringResourceSafe(R.string.common_learn_more),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier
                .align(Alignment.Start)
                .clickable(onClick = um.onLearnMoreClick),
        )

        SelectedTokenContent(
            selectedToken = um.selectedToken,
            selectedAccount = um.selectedAccount,
            onChooseTokenClick = um.onChooseTokenClick,
        )
    }
}

@Composable
private fun SelectedTokenContent(
    selectedToken: TokenItemState?,
    selectedAccount: SelectedAccountUM?,
    onChooseTokenClick: () -> Unit,
) {
    if (selectedToken != null) {
        SpacerH24()

        Text(
            text = stringResourceSafe(R.string.promo_campaign_select_cashback_account),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.fillMaxWidth(),
        )

        SpacerH12()

        PromoCampaignTokenItem(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TangemTheme.colors3.bg.tertiary)
                .clickable {
                    onChooseTokenClick.invoke()
                },
            selectedToken = selectedToken,
            selectedAccount = selectedAccount,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ActivateCampaignContent_WithToken() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            ActivateCampaignContent(um = CampaignPreviewData.activateCampaign)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ActivateCampaignContent_NoToken() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            ActivateCampaignContent(
                um = CampaignPreviewData.activateCampaign.copy(
                    selectedToken = null,
                    selectedAccount = null,
                ),
            )
        }
    }
}
// endregion