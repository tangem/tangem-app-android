package com.tangem.features.earn.impl.sheet.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.inputrow.inner.InputRowAsyncImage
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.sheet.ui.state.EarnYieldRowUM
import com.tangem.features.earn.impl.sheet.ui.state.EarnYieldSheetUM
import kotlinx.collections.immutable.persistentListOf

private val PROVIDER_ICON_SIZE = 40.dp
private val CHEVRON_SIZE = 20.dp

@Composable
internal fun EarnYieldSheetContent(state: EarnYieldSheetUM) {
    TangemModal<EarnYieldSheetUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        onBack = state.onDismiss,
        scrollableContent = true,
        title = { TangemModal.TopNavigation(title = state.title, onClose = state.onDismiss) },
        content = { sheet -> Rows(state = sheet) },
    )
}

@Composable
private fun Rows(state: EarnYieldSheetUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        state.active.forEach { row -> key(row.id) { EarnYieldRow(row = row) } }
        if (state.inactive.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                text = state.inactiveTitle.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            state.inactive.forEach { row -> key(row.id) { EarnYieldRow(row = row) } }
        }
    }
}

@Composable
private fun EarnYieldRow(row: EarnYieldRowUM) {
    if (row.isActive) {
        ActiveRow(row = row)
    } else {
        InactiveRow(row = row)
    }
}

@Composable
private fun ActiveRow(row: EarnYieldRowUM) {
    YieldRowLayout(
        row = row,
        subtitleSlot = {
            AccentRowText(
                text = row.rate,
                style = TangemTheme.typography3.caption.medium,
                textAlign = TextAlign.Start,
            )
        },
        valueSlot = row.amount?.let { amount ->
            { TangemRowText(text = amount, role = TangemRowTextRole.Value) }
        },
        subvalueSlot = row.amountFiat?.let { amountFiat ->
            { TangemRowText(text = amountFiat, role = TangemRowTextRole.Subvalue) }
        },
    )
}

@Composable
private fun InactiveRow(row: EarnYieldRowUM) {
    YieldRowLayout(
        row = row,
        subtitleSlot = row.risk?.let { risk -> { RiskBadge(badge = risk) } },
        valueSlot = {
            AccentRowText(
                text = row.rate,
                style = TangemTheme.typography3.body.medium,
                textAlign = TextAlign.End,
            )
        },
        subvalueSlot = row.caption?.let { caption ->
            { TangemRowText(text = caption, role = TangemRowTextRole.Subvalue) }
        },
    )
}

@Composable
private fun YieldRowLayout(
    row: EarnYieldRowUM,
    subtitleSlot: (@Composable RowScope.() -> Unit)?,
    valueSlot: (@Composable RowScope.() -> Unit)?,
    subvalueSlot: (@Composable RowScope.() -> Unit)?,
) {
    TangemRow(
        modifier = Modifier.fillMaxWidth(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { ProviderIcon(icon = row.icon) },
        titleSlot = { TangemRowText(text = row.title, role = TangemRowTextRole.Title) },
        subtitleSlot = subtitleSlot,
        valueSlot = valueSlot,
        subvalueSlot = subvalueSlot,
        endSlot = {
            Icon(
                modifier = Modifier.size(CHEVRON_SIZE),
                imageVector = Icons.ic_chevron_right_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.tertiary,
            )
        },
        onClick = row.onClick,
    )
}

@Composable
private fun ProviderIcon(icon: ImageReference) {
    val modifier = Modifier
        .size(PROVIDER_ICON_SIZE)
        .clip(CircleShape)

    when (icon) {
        is ImageReference.Url -> InputRowAsyncImage(imageUrl = icon.url, modifier = modifier)
        is ImageReference.Res -> Image(
            modifier = modifier,
            painter = painterResource(id = icon.resId),
            contentDescription = null,
        )
    }
}

@Composable
private fun AccentRowText(text: TextReference, style: TextStyle, textAlign: TextAlign) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors3.text.accent.blue,
        style = style,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RiskBadge(badge: TangemBadgeUM) {
    TangemBadge(
        text = badge.text,
        variant = badge.variant,
        status = badge.status,
        size = badge.size,
        iconStart = badge.iconStart,
        iconEnd = badge.iconEnd,
        contentDescription = badge.contentDescription,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 402, heightDp = 640)
@Preview(showBackground = true, widthDp = 402, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnYieldSheetContentPreview() {
    TangemThemePreviewRedesign {
        EarnYieldSheetContent(
            state = EarnYieldSheetUM(
                title = stringReference("ETH yield"),
                inactiveTitle = stringReference("Not active yet"),
                active = persistentListOf(
                    EarnYieldRowUM(
                        id = EarnProductId("lending"),
                        title = stringReference("Lending on AAVE"),
                        icon = ImageReference.Res(R.drawable.ic_aave_36),
                        rate = stringReference("1,6% net APY"),
                        isActive = true,
                        amount = stringReference("0.5031 ETH"),
                        amountFiat = stringReference("\$2,012.4"),
                        caption = null,
                        risk = null,
                        onClick = {},
                    ),
                ),
                inactive = persistentListOf(
                    EarnYieldRowUM(
                        id = EarnProductId("pooled_staking"),
                        title = stringReference("Pooled staking"),
                        icon = ImageReference.Res(R.drawable.ic_p2p_logo),
                        rate = stringReference("~1.5-2%"),
                        isActive = false,
                        amount = null,
                        amountFiat = null,
                        caption = stringReference("Start earning"),
                        risk = TangemBadgeUM(
                            text = stringReference("Low risk"),
                            status = TangemBadge.Status.Success,
                            size = TangemBadge.Size.X4,
                        ),
                        onClick = {},
                    ),
                    EarnYieldRowUM(
                        id = EarnProductId("high_yield_vault"),
                        title = stringReference("High Yield Vault"),
                        icon = ImageReference.Url(""),
                        rate = stringReference("5.5% APY"),
                        isActive = false,
                        amount = null,
                        amountFiat = null,
                        caption = stringReference("Start earning"),
                        risk = TangemBadgeUM(
                            text = stringReference("Medium risk"),
                            status = TangemBadge.Status.Warning,
                            size = TangemBadge.Size.X4,
                        ),
                        onClick = {},
                    ),
                ),
                onDismiss = {},
            ),
        )
    }
}
// endregion