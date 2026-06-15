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
import com.tangem.core.ui.ds2.shimmers.TextShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_32
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayReissueCardError
import com.tangem.features.tangempay.entity.TangemPayReissueCardUM

@Composable
internal fun TangemPayReissueCardContentV2(state: TangemPayReissueCardUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismissRequest,
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
                        onClick = state.onDismissRequest,
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
private fun Content(state: TangemPayReissueCardUM) {
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
        FeeBlock(
            modifier = Modifier.padding(top = 16.dp),
            state = state,
            appearance = appearance,
        )
        SpacerH(8.dp)
        BottomButtonsBlock(state = state, appearance = appearance)
    }
}

@Composable
private fun StatusIcon(appearance: ReissueCardContentAppearance) {
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
private fun FeeBlock(
    state: TangemPayReissueCardUM,
    appearance: ReissueCardContentAppearance,
    modifier: Modifier = Modifier,
) {
    if (!appearance.shouldShowFeeBlock) return

    Column(modifier = modifier) {
        FeeInfoRow(
            titleRes = R.string.tangempay_reissue_card_fee_label,
            value = state.feeAmount,
            showDivider = appearance.shouldShowBalanceRow,
        )
        if (appearance.shouldShowBalanceRow) {
            FeeInfoRow(
                titleRes = R.string.common_balance_title,
                value = state.cardBalance,
            )
        }
    }
}

@Composable
private fun FeeInfoRow(titleRes: Int, value: String, showDivider: Boolean = false) {
    TangemRow(
        divider = showDivider,
        contentLead = TangemRowContentLead.Start,
        titleSlot = {
            TangemRowText(
                text = resourceReference(titleRes),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            if (value.isEmpty()) {
                TextShimmer(
                    text = "$ 0.00",
                    style = TextShimmerStyle.BODY,
                    radius = 48.dp,
                )
            } else {
                TangemRowText(
                    text = value,
                    role = TangemRowTextRole.Value,
                )
            }
        },
    )
}

@Composable
private fun BottomButtonsBlock(
    state: TangemPayReissueCardUM,
    appearance: ReissueCardContentAppearance,
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
            onClick = state.onDismissRequest,
            text = resourceReference(R.string.common_cancel),
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            onClick = appearance.primaryAction(state),
            isEnabled = !state.isFeeLoading && !state.isReissuingInProgress,
            isLoading = state.isReissuingInProgress,
            text = resourceReference(appearance.primaryButtonTextRes),
        )
    }
}

private data class ReissueCardContentAppearance(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBackgroundColor: Color,
    val primaryButtonTextRes: Int,
    val primaryAction: (TangemPayReissueCardUM) -> () -> Unit,
    val shouldShowFeeBlock: Boolean,
    val shouldShowBalanceRow: Boolean,
)

@Composable
private fun TangemPayReissueCardUM.contentAppearance(): ReissueCardContentAppearance {
    val infoIconColor = TangemTheme.colors3.icon.status.info
    val infoIconBackgroundColor = TangemTheme.colors3.bg.status.infoSubtle
    val warningIconColor = TangemTheme.colors3.icon.status.warning
    val warningIconBackgroundColor = TangemTheme.colors3.bg.status.warningSubtle

    return when (error) {
        TangemPayReissueCardError.InsufficientFunds -> ReissueCardContentAppearance(
            titleRes = R.string.tangempay_reissue_card_insufficient_funds_title,
            subtitleRes = R.string.tangempay_reissue_card_insufficient_funds_subtitle,
            icon = Icons.ic_error_28,
            iconColor = warningIconColor,
            iconBackgroundColor = warningIconBackgroundColor,
            primaryButtonTextRes = R.string.tangempay_card_details_add_funds,
            primaryAction = { it.onAddFundsClick },
            shouldShowFeeBlock = true,
            shouldShowBalanceRow = true,
        )
        TangemPayReissueCardError.InitialDataLoading -> ReissueCardContentAppearance(
            titleRes = R.string.tangempay_reissue_card_fee_unreachable_error_title,
            subtitleRes = R.string.send_fee_unreachable_error_text,
            icon = Icons.ic_error_28,
            iconColor = warningIconColor,
            iconBackgroundColor = warningIconBackgroundColor,
            primaryButtonTextRes = R.string.warning_button_refresh,
            primaryAction = { it.onRetryFee },
            shouldShowFeeBlock = false,
            shouldShowBalanceRow = false,
        )
        null -> ReissueCardContentAppearance(
            titleRes = R.string.tangempay_reissue_card_title,
            subtitleRes = R.string.tangempay_reissue_card_description,
            icon = Icons.ic_arrow_refresh_32,
            iconColor = infoIconColor,
            iconBackgroundColor = infoIconBackgroundColor,
            primaryButtonTextRes = R.string.tangempay_reissue_card_confirm,
            primaryAction = { it.onConfirmClick },
            shouldShowFeeBlock = true,
            shouldShowBalanceRow = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayReissueCardContentV2Preview(
    @PreviewParameter(TangemPayReissueCardUMPreviewProvider::class) state: TangemPayReissueCardUM,
) {
    TangemThemePreviewRedesign {
        ReissueCardSheetPreview(state = state)
    }
}

@Composable
private fun ReissueCardSheetPreview(state: TangemPayReissueCardUM) {
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
                    onClick = state.onDismissRequest,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
        Content(state)
    }
}

private class TangemPayReissueCardUMPreviewProvider : CollectionPreviewParameterProvider<TangemPayReissueCardUM>(
    collection = listOf(
        TangemPayReissueCardUM.stub(error = null),
        TangemPayReissueCardUM.stub(
            error = TangemPayReissueCardError.InsufficientFunds,
            cardBalance = "$0.05",
            feeAmount = "$4.25",
        ),
        TangemPayReissueCardUM.stub(
            error = TangemPayReissueCardError.InitialDataLoading,
        ),
    ),
)