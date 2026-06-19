package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_card_plus_32
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayIssueAdditionalCardUM

@Composable
internal fun TangemPayIssueAdditionalCardContent(state: TangemPayIssueAdditionalCardUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.onDismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            Content(state)
        },
    )
}

@Composable
private fun Content(state: TangemPayIssueAdditionalCardUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Header()
        FeeBlock(state = state)
        if (state.isBalanceInsufficient) {
            InsufficientFundsNotification(state = state)
        }
        IssueButton(state = state)
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpacerH(32.dp)
        StatusIcon()
        SpacerH(32.dp)
        CenteredMessageText(
            textRes = R.string.tangempay_issue_additional_card_title,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        SpacerH(8.dp)
        CenteredMessageText(
            textRes = R.string.tangempay_issue_additional_card_description,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        SpacerH(32.dp)
    }
}

@Composable
private fun StatusIcon() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.status.infoSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.ic_card_plus_32,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.info,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun CenteredMessageText(textRes: Int, style: TextStyle, color: Color) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = stringResourceSafe(textRes),
        style = style,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FeeBlock(state: TangemPayIssueAdditionalCardUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.tangempay_issue_additional_card_fee_label),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            TangemRowText(
                text = state.feeText,
                role = TangemRowTextRole.Value,
            )
        },
    )
}

@Composable
private fun InsufficientFundsNotification(state: TangemPayIssueAdditionalCardUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
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
            onClick = state.onAddFundsClick,
            text = resourceReference(R.string.tangempay_card_details_add_funds),
        )
    }
}

@Composable
private fun IssueButton(state: TangemPayIssueAdditionalCardUM, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        size = TangemButton.Size.X12,
        onClick = state.onIssueClick,
        isEnabled = !state.isLoading && !state.isBalanceInsufficient,
        isLoading = state.isLoading,
        text = resourceReference(R.string.tangempay_issue_card),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayIssueAdditionalCardContentPreview(
    @PreviewParameter(IssueAdditionalCardPreviewProvider::class) state: TangemPayIssueAdditionalCardUM,
) {
    TangemThemePreviewRedesign {
        TangemPayIssueAdditionalCardContent(state = state)
    }
}

private class IssueAdditionalCardPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayIssueAdditionalCardUM>(
        collection = listOf(
            stubState(),
            stubState(isBalanceInsufficient = true),
            stubState(isLoading = true),
        ),
    )

private fun stubState(
    isBalanceInsufficient: Boolean = false,
    isLoading: Boolean = false,
): TangemPayIssueAdditionalCardUM = TangemPayIssueAdditionalCardUM(
    isBalanceInsufficient = isBalanceInsufficient,
    feeText = "$4.25",
    isLoading = isLoading,
    onIssueClick = {},
    onAddFundsClick = {},
    onDismiss = {},
)