package com.tangem.features.tangempay.campaign.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.campaign.TangemPayCampaignIssueCardUM
import com.tangem.features.tangempay.common.TangemPayCardArtwork
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayCampaignIssueCardScreen(state: TangemPayCampaignIssueCardUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSourceTangem()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ill_tangempay_campaign_card_bg),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            TangemTopNavigation(
                title = resourceReference(R.string.tangempay_campaign_issue_card_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = state.onCloseClick,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TangemPayCardArtwork(
                    imageUrl = state.cardArtUrl,
                    isVirtual = true,
                    modifier = Modifier.padding(horizontal = 48.dp),
                )
            }
            IssueCardDetails(state = state)
        }
    }
}

@Composable
private fun ColumnScope.IssueCardDetails(state: TangemPayCampaignIssueCardUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        TangemTabItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            state = TangemTabItemUM.Content(
                id = VIRTUAL_TAB_ID,
                label = resourceReference(R.string.tangempay_order_type_segment_virtual),
                isSelected = true,
                onClick = {},
            ),
        )
        SpacerH(16.dp)
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            InfoRow(
                title = resourceReference(R.string.tangempay_order_type_issue_fee),
                value = stringReference(state.issueFee),
                divider = true,
            )
            InfoRow(
                title = resourceReference(R.string.tangempay_campaign_issue_time),
                value = resourceReference(R.string.tangempay_order_type_delivery_time_instant),
            )
        }
        SpacerH(64.dp)
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_campaign_issue_button),
            isLoading = state.isIssuing,
            onClick = state.onIssueClick,
        )
        SpacerH(12.dp)
    }
}

@Composable
private fun InfoRow(title: TextReference, value: TextReference, divider: Boolean = false) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.Start,
        titleSlot = {
            TangemRowText(text = title.resolveReference(), role = TangemRowTextRole.Title, maxLines = Int.MAX_VALUE)
        },
        valueSlot = {
            TangemRowText(text = value.resolveReference(), role = TangemRowTextRole.Value, maxLines = Int.MAX_VALUE)
        },
    )
}

private const val VIRTUAL_TAB_ID = "virtual"

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCampaignIssueCardScreenPreview(
    @PreviewParameter(TangemPayCampaignIssueCardUMProvider::class) state: TangemPayCampaignIssueCardUM,
) {
    TangemThemePreviewRedesign {
        TangemPayCampaignIssueCardScreen(state = state)
    }
}

private class TangemPayCampaignIssueCardUMProvider : CollectionPreviewParameterProvider<TangemPayCampaignIssueCardUM>(
    collection = listOf(
        previewIssueCard(isIssuing = false),
        previewIssueCard(isIssuing = true),
    ),
)

private fun previewIssueCard(isIssuing: Boolean) = TangemPayCampaignIssueCardUM(
    cardArtUrl = "https://app.tangem.com/cards/card_default.png",
    issueFee = "$0",
    isIssuing = isIssuing,
    onIssueClick = {},
    onCloseClick = {},
)