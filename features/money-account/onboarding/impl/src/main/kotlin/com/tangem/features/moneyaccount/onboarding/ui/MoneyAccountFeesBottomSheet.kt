package com.tangem.features.moneyaccount.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.features.moneyaccount.onboarding.impl.R
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM.FeeRowUM
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM.FeesUM
import kotlinx.collections.immutable.persistentListOf

private const val FEES_PLACEHOLDER_ROWS = 2

@Composable
internal fun MoneyAccountFeesBottomSheet(fees: FeesUM?, onDismiss: () -> Unit) {
    val displayedFees = rememberLastNonNull(fees) ?: return
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = fees != null,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            TangemModal.TopNavigation(
                title = stringReference("Fees"),
                onClose = onDismiss,
            )
        },
    ) {
        FeesContent(fees = displayedFees)
    }
}

@Composable
private fun FeesContent(fees: FeesUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (fees) {
            is FeesUM.Error -> FeesError(onRetryClick = fees.onRetryClick)
            FeesUM.Loading -> FeesTable {
                repeat(FEES_PLACEHOLDER_ROWS) { index ->
                    FeeRowPlaceholder()
                    if (index != FEES_PLACEHOLDER_ROWS - 1) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = TangemTheme.colors3.border.primary,
                        )
                    }
                }
            }
            is FeesUM.Content -> FeesTable {
                fees.rows.forEachIndexed { index, row ->
                    FeeRow(row = row)
                    if (index != fees.rows.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = TangemTheme.colors3.border.primary,
                        )
                    }
                }
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            text = "SWIFT transfers aren't supported. If you send one, it will be returned",
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeesTable(rows: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        FeesTableRow(
            modifier = Modifier.padding(vertical = 12.dp),
            provider = { CaptionText(text = "Provider", textAlign = TextAlign.Start) },
            receive = { CaptionText(text = "Receive", textAlign = TextAlign.End) },
            send = { CaptionText(text = "Send", textAlign = TextAlign.End) },
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = TangemTheme.colors3.border.primary,
        )
        rows()
    }
}

@Composable
private fun FeeRow(row: FeeRowUM) {
    FeesTableRow(
        modifier = Modifier.padding(vertical = 16.dp),
        provider = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TangemIcon(
                    tangemIconUM = row.icon,
                    modifier = Modifier
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape),
                )
                BodyText(
                    text = row.provider,
                    textAlign = TextAlign.Start,
                )
            }
        },
        receive = {
            BodyText(
                text = row.receive,
                textAlign = TextAlign.End,
            )
        },
        send = {
            BodyText(
                text = row.send,
                textAlign = TextAlign.End,
            )
        },
    )
}

@Composable
private fun FeeRowPlaceholder() {
    val style = TangemTheme.typography3.body.medium
    FeesTableRow(
        modifier = Modifier.padding(vertical = 16.dp),
        provider = {
            TangemShimmer(
                style = style,
                textAlign = TextAlign.Start,
            )
        },
        receive = {
            TangemShimmer(
                style = style,
                textAlign = TextAlign.End,
            )
        },
        send = {
            TangemShimmer(
                style = style,
                textAlign = TextAlign.End,
            )
        },
    )
}

@Composable
private fun FeesTableRow(
    modifier: Modifier = Modifier,
    provider: @Composable () -> Unit,
    receive: @Composable () -> Unit,
    send: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(120.dp)) { provider() }
        Box(modifier = Modifier.weight(1f)) { receive() }
        Box(modifier = Modifier.weight(1f)) { send() }
    }
}

@Composable
private fun FeesError(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp)
            .padding(horizontal = 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X10,
            iconStart = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
            onClick = onRetryClick,
        )
        CaptionText(
            text = "Failed to load fees information.\nTap to reload",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CaptionText(text: String, textAlign: TextAlign) {
    TableText(text = text, style = TangemTheme.typography3.caption.medium, isSecondary = true, textAlign = textAlign)
}

@Composable
private fun BodyText(text: String, textAlign: TextAlign) {
    TableText(text = text, style = TangemTheme.typography3.body.medium, isSecondary = false, textAlign = textAlign)
}

@Composable
private fun TableText(text: String, style: TextStyle, isSecondary: Boolean, textAlign: TextAlign) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = style,
        color = if (isSecondary) TangemTheme.colors3.text.secondary else TangemTheme.colors3.text.primary,
        textAlign = textAlign,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountFeesBottomSheetPreview(@PreviewParameter(MoneyAccountFeesStateProvider::class) fees: FeesUM) {
    TangemThemePreviewRedesign {
        MoneyAccountFeesBottomSheet(fees = fees, onDismiss = {})
    }
}

private class MoneyAccountFeesStateProvider : CollectionPreviewParameterProvider<FeesUM>(
    listOf(
        FeesUM.Content(
            rows = persistentListOf(
                previewFeeRow(provider = "FedWire"),
                previewFeeRow(provider = "ACH"),
            ),
        ),
        FeesUM.Loading,
        FeesUM.Error(onRetryClick = {}),
    ),
)

private fun previewFeeRow(provider: String) = FeeRowUM(
    icon = TangemIconUM.Image(R.drawable.img_flag_usa_44),
    provider = provider,
    receive = "$1000000",
    send = "$12345.67890",
)