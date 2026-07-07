package com.tangem.features.promobanners.impl.campaigns.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignAlreadyActivatedUM
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM

@Composable
internal fun ActivateCampaignContent(um: CampaignAlreadyActivatedUM) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.status.infoSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.ic_info_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.info,
            )
        }

        SpacerH32()

        Text(
            text = "You’re already enrolled in Whale Swap Cashback", // TODO localization
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        SpacerH12()

        SelectedTokenContent(
            selectedToken = um.selectedToken,
            selectedAccount = um.selectedAccount,
        )

        SpacerH32()
    }
}

@Composable
private fun SelectedTokenContent(selectedToken: TokenItemState, selectedAccount: SelectedAccountUM?) {
    SpacerH24()

    Text(
        text = "Eligible cashback will be distributed to:", // TODO([REDACTED_TASK_KEY]): localize
        style = TangemTheme.typography.subtitle1,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    SpacerH12()

    PromoCampaignTokenItem(
        modifier = Modifier.fillMaxWidth(),
        selectedToken = selectedToken,
        selectedAccount = selectedAccount,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AlreadyActivatedCampaignContent() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            ActivateCampaignContent(um = CampaignPreviewData.alreadyActivated)
        }
    }
}
// endregion