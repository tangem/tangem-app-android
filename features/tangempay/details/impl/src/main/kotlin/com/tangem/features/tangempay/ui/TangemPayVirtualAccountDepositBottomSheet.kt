package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_32
import com.tangem.features.tangempay.entity.TangemPayVirtualAccountDepositUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TangemPayVirtualAccountDepositBottomSheet(state: TangemPayVirtualAccountDepositUM) {
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
                title = null,
                endContent = { TangemButton.Close(onClick = state.onDismiss) },
            )
        },
        content = { _ -> DepositContent(state) },
    )
}

@Composable
private fun DepositContent(state: TangemPayVirtualAccountDepositUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IntroIcons(modifier = Modifier.padding(top = TangemTheme.dimens2.x4))
        TitleText(
            text = stringReference("Bank transfer might take 1-2 business days"),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x8),
        )
        SubtitleText(
            text = stringReference("Received USD will be converted to USDC by 1:1 rate"),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x2),
        )
        FeesBlock(
            fees = state.fees,
            modifier = Modifier.padding(top = TangemTheme.dimens2.x6),
        )
        InfoNotification(
            text = stringReference("Deposit via ACH or FedWire only. SWIFT transfers will be returned."),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x4),
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens2.x4),
            text = stringReference("Show details"),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            onClick = state.onShowDetailsClick,
        )
        if (state.shouldShowTermsAndConditions) {
            TermsFooter(
                onTermsClick = state.onTermsClick,
                onPrivacyClick = state.onPrivacyClick,
                modifier = Modifier.padding(top = TangemTheme.dimens2.x3),
            )
        }
    }
}

@Composable
private fun FeesBlock(fees: ImmutableList<TangemPayVirtualAccountDepositUM.FeeRow>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(
                start = TangemTheme.dimens2.x4,
                bottom = TangemTheme.dimens2.x2,
            ),
            text = stringReference("Fee for onramp").resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        fees.forEachIndexed { index, fee ->
            TangemRow(
                contentLead = TangemRowContentLead.Equal,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                divider = index != fees.lastIndex,
                titleSlot = { TangemRowText(text = fee.title, role = TangemRowTextRole.Title) },
                valueSlot = { TangemRowText(text = stringReference(fee.value), role = TangemRowTextRole.Value) },
            )
        }
    }
}

@Composable
private fun InfoNotification(text: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens2.x4))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .padding(TangemTheme.dimens2.x4),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x5),
            imageVector = Icons.ic_info_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.info,
        )
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun TermsFooter(onTermsClick: () -> Unit, onPrivacyClick: () -> Unit, modifier: Modifier = Modifier) {
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)
    val text = buildAnnotatedString {
        append("By using service, you agree with provider ")
        withLink(LinkAnnotation.Clickable(tag = "terms", linkInteractionListener = { onTermsClick() })) {
            withStyle(linkStyle) { append("Terms of Use") }
        }
        append(" and ")
        withLink(LinkAnnotation.Clickable(tag = "privacy", linkInteractionListener = { onPrivacyClick() })) {
            withStyle(linkStyle) { append("Privacy Policy") }
        }
    }
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun IntroIcons(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-TangemTheme.dimens2.x4),
    ) {
        UsdIcon()
        UsdcIcon()
    }
}

@Composable
private fun UsdIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x20)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.opaque.primary)
            .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x8),
            imageVector = Icons.ic_sign_usd_32,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
    }
}

@Composable
private fun UsdcIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(TangemTheme.dimens2.x20)) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
            painter = painterResource(CoreUiR.drawable.img_usdc_16),
            contentDescription = null,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(TangemTheme.dimens2.x6)
                .background(color = TangemTheme.colors3.bg.accent.violet, shape = CircleShape)
                .border(
                    width = TangemTheme.dimens2.x0_5,
                    color = TangemTheme.colors3.bg.secondary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens2.x4),
                painter = painterResource(CoreUiR.drawable.ic_polygon_22),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.inverse,
            )
        }
    }
}

@Composable
private fun TitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SubtitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

private fun previewState(shouldShowTermsAndConditions: Boolean) = TangemPayVirtualAccountDepositUM(
    fees = persistentListOf(
        TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("ACH"), value = "$1"),
        TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("FedWire"), value = "$11"),
    ),
    shouldShowTermsAndConditions = shouldShowTermsAndConditions,
    onShowDetailsClick = {},
    onDismiss = {},
    onTermsClick = {},
    onPrivacyClick = {},
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DepositEligiblePreview() {
    TangemThemePreviewRedesign {
        DepositContent(
            state = previewState(shouldShowTermsAndConditions = true),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DepositAvailablePreview() {
    TangemThemePreviewRedesign {
        DepositContent(
            state = previewState(shouldShowTermsAndConditions = false),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}