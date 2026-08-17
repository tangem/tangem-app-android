package com.tangem.features.tangempay.addfunds.va.deposit

import android.content.res.Configuration
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_32
import com.tangem.features.tangempay.details.impl.R
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IntroIcons(modifier = Modifier.padding(top = TangemTheme.dimens2.x4))
        TitleText(
            text = resourceReference(R.string.tangempay_bank_transfer_intro_title),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x8),
        )
        SubtitleText(
            text = resourceReference(R.string.tangempay_bank_transfer_intro_subtitle),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x2),
        )
        FeesBlock(
            fees = state.fees,
            modifier = Modifier.padding(top = TangemTheme.dimens2.x6),
        )
        InfoNotification(
            text = resourceReference(R.string.tangempay_bank_transfer_swift_warning),
            modifier = Modifier.padding(top = TangemTheme.dimens2.x4),
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens2.x4),
            text = resourceReference(R.string.tangempay_bank_transfer_show_details),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            isLoading = state.isLoading,
            isEnabled = !state.isLoading,
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
private fun FeesBlock(fees: TangemPayVirtualAccountDepositUM.FeesUM, modifier: Modifier = Modifier) {
    when (fees) {
        is TangemPayVirtualAccountDepositUM.FeesUM.Error -> FeesErrorBanner(
            onRetryClick = fees.onRetryClick,
            modifier = modifier,
        )
        TangemPayVirtualAccountDepositUM.FeesUM.Loading -> FeesColumn(modifier = modifier) {
            val placeholderRows = 2
            repeat(placeholderRows) { index ->
                FeeRowPlaceholder(divider = index != placeholderRows - 1)
            }
        }
        is TangemPayVirtualAccountDepositUM.FeesUM.Content -> {
            if (fees.rows.isEmpty()) return
            FeesColumn(modifier = modifier) {
                fees.rows.forEachIndexed { index, fee ->
                    TangemRow(
                        contentLead = TangemRowContentLead.Equal,
                        verticalAlignment = TangemRowVerticalAlignment.Center,
                        divider = index != fees.rows.lastIndex,
                        titleSlot = {
                            TangemRowText(
                                text = fee.title,
                                role = TangemRowTextRole.Title,
                            )
                        },
                        valueSlot = {
                            TangemRowText(
                                text = stringReference(fee.value),
                                role = TangemRowTextRole.Value,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeesColumn(modifier: Modifier = Modifier, rows: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(
                start = TangemTheme.dimens2.x4,
                bottom = TangemTheme.dimens2.x2,
            ),
            text = stringResourceSafe(R.string.tangempay_bank_transfer_fee_header),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        rows()
    }
}

@Composable
private fun FeesErrorBanner(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        modifier = modifier.fillMaxWidth(),
        variant = TangemMessageBanner.Variant.Error,
        title = resourceReference(R.string.tangempay_bank_transfer_fee_error_title),
        description = resourceReference(R.string.tangempay_bank_transfer_fee_error_subtitle),
        onClick = onRetryClick,
        slotEnd = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.ic_arrow_refresh_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        },
    )
}

@Composable
private fun FeeRowPlaceholder(divider: Boolean, modifier: Modifier = Modifier) {
    @Composable
    fun Shimmer(text: String) {
        TextShimmer(
            style = TangemTheme.typography3.body.medium,
            text = text,
            textSizeHeight = true,
            radius = 999.dp,
        )
    }
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Equal,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        divider = divider,
        titleSlot = { Shimmer("Commission Name") },
        valueSlot = { Shimmer("Value") },
    )
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
    val termsTitle = stringResourceSafe(R.string.tangempay_bank_transfer_terms_of_use)
    val privacyTitle = stringResourceSafe(R.string.common_privacy_policy)
    val fullText = stringResourceSafe(R.string.tangempay_bank_transfer_legal, termsTitle, privacyTitle)

    // Locate each link title in the resolved (localized) string and splice them in appearance order.
    // Handles translations that reorder the %1$s/%2$s placeholders and skips a title that a translation
    // does not contain verbatim — falling back to plain text instead of crashing on an invalid substring range.
    val links = listOf(
        Triple(fullText.indexOf(termsTitle), termsTitle, onTermsClick),
        Triple(fullText.indexOf(privacyTitle), privacyTitle, onPrivacyClick),
    )
        .filter { it.first >= 0 }
        .sortedBy { it.first }

    val text = buildAnnotatedString {
        var cursor = 0
        links.forEach { (index, title, onClick) ->
            if (index < cursor) return@forEach
            append(fullText.substring(cursor, index))
            withLink(LinkAnnotation.Clickable(tag = title, linkInteractionListener = { onClick() })) {
                withStyle(linkStyle) { append(title) }
            }
            cursor = index + title.length
        }
        append(fullText.substring(cursor))
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
                modifier = Modifier.size(TangemTheme.dimens2.x6),
                painter = painterResource(CoreUiR.drawable.ic_polygon_22),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.staticDark,
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

private fun previewState(
    shouldShowTermsAndConditions: Boolean,
    fees: TangemPayVirtualAccountDepositUM.FeesUM = TangemPayVirtualAccountDepositUM.FeesUM.Content(
        rows = persistentListOf(
            TangemPayVirtualAccountDepositUM.FeeRow(
                title = resourceReference(R.string.tangempay_bank_transfer_fee_ach),
                value = "$1",
            ),
            TangemPayVirtualAccountDepositUM.FeeRow(
                title = resourceReference(R.string.tangempay_bank_transfer_fee_fedwire),
                value = "$11",
            ),
        ),
    ),
) = TangemPayVirtualAccountDepositUM(
    fees = fees,
    shouldShowTermsAndConditions = shouldShowTermsAndConditions,
    isLoading = false,
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DepositFeesLoadingPreview() {
    TangemThemePreviewRedesign {
        DepositContent(
            state = previewState(
                shouldShowTermsAndConditions = false,
                fees = TangemPayVirtualAccountDepositUM.FeesUM.Loading,
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DepositFeesErrorPreview() {
    TangemThemePreviewRedesign {
        DepositContent(
            state = previewState(
                shouldShowTermsAndConditions = false,
                fees = TangemPayVirtualAccountDepositUM.FeesUM.Error(onRetryClick = {}),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}