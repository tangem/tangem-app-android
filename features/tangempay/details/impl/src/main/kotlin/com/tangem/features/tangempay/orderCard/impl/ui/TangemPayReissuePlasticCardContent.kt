package com.tangem.features.tangempay.orderCard.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_32
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayReissuePlasticCardUM

@Composable
internal fun TangemPayReissuePlasticCardContent(state: TangemPayReissuePlasticCardUM) {
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
                endContent = { TangemButton.Close(onClick = state.onDismissRequest) },
            )
        },
        content = { Content(state) },
    )
}

@Composable
private fun Content(state: TangemPayReissuePlasticCardUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpacerH(16.dp)
        when (state) {
            is TangemPayReissuePlasticCardUM.Error -> ErrorContent(state)
            is TangemPayReissuePlasticCardUM.Loading -> ReplaceContent(
                content = null,
                onDismissRequest = state.onDismissRequest,
            )
            is TangemPayReissuePlasticCardUM.Content -> ReplaceContent(
                content = state,
                onDismissRequest = state.onDismissRequest,
            )
        }
    }
}

@Composable
private fun ReplaceContent(content: TangemPayReissuePlasticCardUM.Content?, onDismissRequest: () -> Unit) {
    StatusIcon(
        icon = Icons.ic_arrow_refresh_32,
        iconColor = TangemTheme.colors3.icon.status.info,
        backgroundColor = TangemTheme.colors3.bg.status.infoSubtle,
    )
    SpacerH(32.dp)
    CenteredText(
        text = resourceReference(R.string.tangempay_reissue_plastic_title),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
    )
    CenteredText(
        text = resourceReference(R.string.tangempay_reissue_plastic_description),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
    )
    SpacerH(32.dp)
    DeliveryRows(content = content)
    SpacerH(16.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            variant = TangemButton.Variant.Secondary,
            onClick = onDismissRequest,
            text = resourceReference(R.string.common_cancel),
        )
        SpacerH(8.dp)
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            variant = TangemButton.Variant.Primary,
            onClick = content?.onReplaceClick ?: {},
            isEnabled = content?.isReplaceEnabled == true,
            text = resourceReference(R.string.tangempay_card_details_reissue_card),
        )
    }
    SpacerH(16.dp)
}

@Composable
private fun DeliveryRows(content: TangemPayReissuePlasticCardUM.Content?) {
    InfoRow(
        title = resourceReference(R.string.tangempay_order_type_delivery_to),
        value = content?.let { stringReference(it.country) },
        divider = true,
    )
    TangemRow(
        divider = true,
        contentLead = TangemRowContentLead.End,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.tangempay_order_type_delivery_fee),
                role = TangemRowTextRole.Title,
            )
        },
        subtitleSlot = if (content?.isInsufficientFunds == true) {
            {
                Text(
                    text = resourceReference(R.string.tangempay_order_type_not_enough_money).resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.status.warning,
                )
            }
        } else {
            null
        },
        valueSlot = { RowValue(text = content?.let { stringReference(it.deliveryFee) }) },
    )
    InfoRow(
        title = resourceReference(R.string.tangempay_order_type_delivery_time),
        value = content?.let { loaded ->
            pluralReference(
                id = R.plurals.tangempay_order_type_delivery_eta,
                count = loaded.deliveryEtaMaxBusinessDays,
                formatArgs = wrappedList(loaded.deliveryEtaMaxBusinessDays),
            )
        },
    )
}

@Composable
private fun InfoRow(title: TextReference, value: TextReference?, divider: Boolean = false) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.End,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = { RowValue(text = value) },
    )
}

@Composable
private fun RowValue(text: TextReference?) {
    if (text == null) {
        TangemShimmer(
            modifier = Modifier.width(80.dp),
            style = TangemTheme.typography3.body.medium,
            textAlign = TextAlign.End,
        )
    } else {
        TangemRowText(text = text, role = TangemRowTextRole.Value)
    }
}

@Composable
private fun ErrorContent(state: TangemPayReissuePlasticCardUM.Error) {
    StatusIcon(
        icon = Icons.ic_error_28,
        iconColor = TangemTheme.colors3.icon.status.warning,
        backgroundColor = TangemTheme.colors3.bg.status.warningSubtle,
    )
    SpacerH(32.dp)
    CenteredText(
        text = resourceReference(R.string.tangempay_reissue_card_fee_unreachable_error_title),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
    )
    CenteredText(
        text = resourceReference(R.string.send_fee_unreachable_error_text),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
    )
    SpacerH(32.dp)
    TangemButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        size = TangemButton.Size.X12,
        variant = TangemButton.Variant.Primary,
        onClick = state.onRetry,
        text = resourceReference(R.string.warning_button_refresh),
    )
}

@Composable
private fun StatusIcon(icon: ImageVector, iconColor: Color, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun CenteredText(text: TextReference, style: TextStyle, color: Color) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = text.resolveReference(),
        style = style,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayReissuePlasticCardContentPreview(
    @PreviewParameter(ReissuePlasticPreviewProvider::class) state: TangemPayReissuePlasticCardUM,
) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = { TangemButton.Close(onClick = state.onDismissRequest) },
            )
            Content(state)
        }
    }
}

private class ReissuePlasticPreviewProvider : CollectionPreviewParameterProvider<TangemPayReissuePlasticCardUM>(
    collection = listOf(
        previewContent(),
        previewContent(isInsufficientFunds = true),
        TangemPayReissuePlasticCardUM.Loading(onDismissRequest = {}),
        TangemPayReissuePlasticCardUM.Error(onDismissRequest = {}, onRetry = {}),
    ),
)

private fun previewContent(isInsufficientFunds: Boolean = false) = TangemPayReissuePlasticCardUM.Content(
    onDismissRequest = {},
    country = "Afghanistan",
    deliveryFee = "$10",
    deliveryEtaMaxBusinessDays = 20,
    isInsufficientFunds = isInsufficientFunds,
    onReplaceClick = {},
)