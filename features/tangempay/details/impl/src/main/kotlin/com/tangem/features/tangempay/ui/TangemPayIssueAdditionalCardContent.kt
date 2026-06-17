package com.tangem.features.tangempay.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.tangem.core.ui.res.generated.icons.ic_error_28
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
    val appearance = state.contentAppearance()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpacerH(16.dp)
        StatusIcon(appearance = appearance)
        SpacerH(32.dp)
        CenteredMessageText(
            textRes = appearance.titleRes,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        CenteredMessageText(
            textRes = appearance.subtitleRes,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        SpacerH(32.dp)
        FeeBlock(modifier = Modifier.padding(top = 16.dp), state = state)
        SpacerH(8.dp)
        BottomButtonsBlock(state = state, appearance = appearance)
    }
}

@Composable
private fun StatusIcon(appearance: IssueAdditionalCardContentAppearance) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(appearance.iconBackgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = appearance.icon,
            contentDescription = null,
            tint = appearance.iconColor,
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
private fun BottomButtonsBlock(
    state: TangemPayIssueAdditionalCardUM,
    appearance: IssueAdditionalCardContentAppearance,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            onClick = state.onDismiss,
            text = resourceReference(R.string.common_cancel),
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            onClick = appearance.primaryAction(state),
            isEnabled = !state.isLoading,
            isLoading = state.isLoading,
            text = resourceReference(appearance.primaryButtonTextRes),
        )
    }
}

private data class IssueAdditionalCardContentAppearance(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBackgroundColor: Color,
    val primaryButtonTextRes: Int,
    val primaryAction: (TangemPayIssueAdditionalCardUM) -> () -> Unit,
)

@Composable
private fun TangemPayIssueAdditionalCardUM.contentAppearance(): IssueAdditionalCardContentAppearance {
    return if (isBalanceInsufficient) {
        IssueAdditionalCardContentAppearance(
            titleRes = R.string.tangempay_reissue_card_insufficient_funds_title,
            subtitleRes = R.string.tangempay_reissue_card_insufficient_funds_subtitle,
            icon = Icons.ic_error_28,
            iconColor = TangemTheme.colors3.icon.status.warning,
            iconBackgroundColor = TangemTheme.colors3.bg.status.warningSubtle,
            primaryButtonTextRes = R.string.tangempay_card_details_add_funds,
            primaryAction = { it.onAddFundsClick },
        )
    } else {
        IssueAdditionalCardContentAppearance(
            titleRes = R.string.tangempay_issue_additional_card_title,
            subtitleRes = R.string.tangempay_issue_additional_card_description,
            icon = Icons.ic_card_plus_32,
            iconColor = TangemTheme.colors3.icon.status.info,
            iconBackgroundColor = TangemTheme.colors3.bg.status.infoSubtle,
            primaryButtonTextRes = R.string.tangempay_issue_card,
            primaryAction = { it.onIssueClick },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayIssueAdditionalCardContentPreview(
    @PreviewParameter(IssueAdditionalCardPreviewProvider::class) state: TangemPayIssueAdditionalCardUM,
) {
    TangemThemePreviewRedesign {
        IssueAdditionalCardSheetPreview(state = state)
    }
}

@Composable
private fun IssueAdditionalCardSheetPreview(state: TangemPayIssueAdditionalCardUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
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
        Content(state)
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