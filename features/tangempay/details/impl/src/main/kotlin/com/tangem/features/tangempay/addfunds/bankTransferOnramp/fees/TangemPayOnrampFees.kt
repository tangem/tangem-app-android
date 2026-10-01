package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

private val PlaceholderTitleIds = listOf(
    R.string.tangempay_bank_transfer_fee_ach,
    R.string.tangempay_bank_transfer_fee_fedwire,
)

@Composable
internal fun TangemPayOnrampFees(
    fees: TangemPayOnrampFeesUM,
    modifier: Modifier = Modifier,
    containerColor: Color = TangemTheme.colors3.bg.secondary,
) {
    val rowsModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(containerColor)
    when (fees) {
        is TangemPayOnrampFeesUM.Error -> FeesErrorBanner(onRetryClick = fees.onRetryClick, modifier = modifier)
        TangemPayOnrampFeesUM.Loading -> ProvideTangemShimmer {
            Column(modifier = rowsModifier) {
                PlaceholderTitleIds.forEachIndexed { index, titleId ->
                    FeeRowPlaceholder(
                        title = resourceReference(titleId),
                        divider = index != PlaceholderTitleIds.lastIndex,
                    )
                }
            }
        }
        is TangemPayOnrampFeesUM.Content -> {
            if (fees.rows.isEmpty()) return
            Column(modifier = rowsModifier) {
                fees.rows.forEachIndexed { index, fee ->
                    FeeRow(fee = fee, divider = index != fees.rows.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun FeeRow(fee: TangemPayOnrampFeesUM.FeeRow, divider: Boolean) {
    TangemRow(
        contentLead = TangemRowContentLead.Equal,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        divider = divider,
        titleSlot = {
            TangemRowText(
                modifier = Modifier.testTag(TangemPayTestTags.vaDepositFeeTitle(fee.type)),
                text = fee.title,
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            TangemPayRowValueText(
                modifier = Modifier.testTag(TangemPayTestTags.vaDepositFeeValue(fee.type)),
                text = fee.value,
            )
        },
    )
}

@Composable
internal fun TangemPayRowValueText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FeeRowPlaceholder(title: TextReference, divider: Boolean) {
    TangemRow(
        contentLead = TangemRowContentLead.Equal,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        divider = divider,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = { TangemShimmer(style = TangemTheme.typography3.body.medium, textAlign = TextAlign.End) },
    )
}

@Composable
private fun FeesErrorBanner(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TangemPayTestTags.VA_DEPOSIT_FEE_ERROR_BANNER),
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