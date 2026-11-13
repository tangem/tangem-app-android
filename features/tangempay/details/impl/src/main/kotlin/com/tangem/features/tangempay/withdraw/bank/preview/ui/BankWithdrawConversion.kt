package com.tangem.features.tangempay.withdraw.bank.preview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RateLockUM
import com.tangem.features.tangempay.withdraw.bank.ui.BankWithdrawValueShimmer

@Composable
internal fun BankWithdrawConversion(conversion: TangemPayBankWithdrawPreviewUM.ConversionUM) {
    BankWithdrawSectionTitle(text = resourceReference(R.string.money_account_send_conversion))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        CaptionedRow(label = resourceReference(R.string.common_rate), value = conversion.rate, divider = true)
        CaptionedRow(
            label = resourceReference(R.string.money_account_send_recipient_gets),
            value = conversion.recipientGets,
            divider = false,
        )
    }
    RateLock(rateLock = conversion.rateLock)
    BankWithdrawSectionTitle(text = resourceReference(R.string.money_account_send_fees_title))
    Column(modifier = Modifier.fillMaxWidth()) {
        conversion.fees.fastForEach { fee -> FeeRow(label = fee.label, value = fee.value, divider = true) }
        FeeRow(
            label = resourceReference(R.string.money_account_send_fees_total),
            value = conversion.total,
            divider = false,
        )
    }
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = resourceReference(R.string.money_account_send_fees_deducted).resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
internal fun BankWithdrawSectionTitle(text: TextReference) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
private fun CaptionedRow(label: TextReference, value: String?, divider: Boolean) {
    // Figma puts the caption label above the body value, so the row roles are swapped on purpose.
    TangemRow(
        divider = divider,
        titleSlot = { TangemRowText(text = label, role = TangemRowTextRole.Subtitle) },
        subtitleSlot = {
            if (value == null) {
                TangemShimmer(style = TangemTheme.typography3.body.medium)
            } else {
                TangemRowText(text = value, role = TangemRowTextRole.Title)
            }
        },
    )
}

@Composable
private fun RateLock(rateLock: RateLockUM) {
    val modifier = Modifier.padding(horizontal = 16.dp)
    when (rateLock) {
        RateLockUM.Loading -> TangemShimmer(style = TangemTheme.typography3.caption.medium, modifier = modifier)
        RateLockUM.Hidden -> Unit
        is RateLockUM.Countdown -> Text(
            modifier = modifier,
            text = rateLock.text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.accent.blue,
        )
    }
}

@Composable
private fun FeeRow(label: TextReference, value: String?, divider: Boolean) {
    TangemRow(
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        divider = divider,
        titleSlot = {
            Text(
                text = label.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
        valueSlot = {
            if (value == null) {
                BankWithdrawValueShimmer(modifier = Modifier.padding(vertical = 2.dp))
            } else {
                TangemRowText(text = value, role = TangemRowTextRole.Value)
            }
        },
    )
}