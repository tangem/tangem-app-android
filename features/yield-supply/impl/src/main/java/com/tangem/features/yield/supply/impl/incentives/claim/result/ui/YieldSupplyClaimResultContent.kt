package com.tangem.features.yield.supply.impl.incentives.claim.result.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.res.R as CoreResR

/** The Yield screen is on the legacy theme, while the status template needs the redesigned palette. */
@Composable
internal fun YieldSupplyClaimResultContent(
    state: YieldSupplyClaimResultUM,
    onGoToTokenClick: () -> Unit,
    onGoToYieldClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.status

    TangemThemeRedesign {
        TangemStatusScreen(
            modifier = modifier,
            variant = when (status) {
                is YieldSupplyClaimResultUM.Status.Submitted -> TangemStatusScreen.Variant.Info
                is YieldSupplyClaimResultUM.Status.Confirmed -> TangemStatusScreen.Variant.Success
                is YieldSupplyClaimResultUM.Status.Failed -> TangemStatusScreen.Variant.Error
            },
            title = state.title,
            description = status.description,
            asset = { StatusIcon(status = status) },
            details = { if (status !is YieldSupplyClaimResultUM.Status.Failed) Fees(state = state) },
            footer = { Footer(onGoToTokenClick = onGoToTokenClick, onGoToYieldClick = onGoToYieldClick) },
        )
    }
}

@Composable
private fun StatusIcon(status: YieldSupplyClaimResultUM.Status) {
    val modifier = Modifier.size(40.dp)
    when (status) {
        is YieldSupplyClaimResultUM.Status.Submitted -> TangemStatusIcon(
            kind = TangemStatusIcon.Kind.Checkmark,
            tint = TangemTheme.colors3.icon.status.info,
            modifier = modifier,
        )
        is YieldSupplyClaimResultUM.Status.Confirmed ->
            TangemStatusIcon(kind = TangemStatusIcon.Kind.Checkmark, modifier = modifier)
        is YieldSupplyClaimResultUM.Status.Failed ->
            TangemStatusIcon(kind = TangemStatusIcon.Kind.HeartBroken, modifier = modifier)
    }
}

@Composable
private fun Fees(state: YieldSupplyClaimResultUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FeeRow(
            title = resourceReference(CoreResR.string.common_network_fee_title),
            value = state.networkFee,
            divider = true,
        )
        FeeRow(
            title = resourceReference(CoreResR.string.yield_module_claim_service_fee_title),
            value = state.serviceFee,
        )
    }
}

@Composable
private fun FeeRow(title: TextReference, value: TextReference?, divider: Boolean = false) {
    TangemRow(
        divider = divider,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = {
            Text(
                text = value?.resolveReference() ?: DASH_SIGN,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
    )
}

@Composable
private fun Footer(onGoToTokenClick: () -> Unit, onGoToYieldClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        TangemButton(
            modifier = Modifier.weight(1f),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreResR.string.common_go_to_token),
            onClick = onGoToTokenClick,
        )
        TangemButton(
            modifier = Modifier.weight(1f),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.yield_module_claim_go_to_yield_button),
            onClick = onGoToYieldClick,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyClaimResultSubmittedPreview() {
    TangemThemePreviewRedesign {
        YieldSupplyClaimResultContent(
            state = YieldSupplyClaimResultUM(
                status = YieldSupplyClaimResultUM.Status.Submitted,
                total = stringReference("$45.50"),
                serviceFee = stringReference("(15%) $1.40"),
                networkFee = stringReference("$1.40"),
            ),
            onGoToTokenClick = {},
            onGoToYieldClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyClaimResultConfirmedPreview() {
    TangemThemePreviewRedesign {
        YieldSupplyClaimResultContent(
            state = YieldSupplyClaimResultUM(
                status = YieldSupplyClaimResultUM.Status.Confirmed(timestamp = 1_714_640_460_000L),
                total = stringReference("$45.50"),
                serviceFee = stringReference("(15%) $1.40"),
                networkFee = stringReference("$1.40"),
            ),
            onGoToTokenClick = {},
            onGoToYieldClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyClaimResultFailedPreview() {
    TangemThemePreviewRedesign {
        YieldSupplyClaimResultContent(
            state = YieldSupplyClaimResultUM(
                status = YieldSupplyClaimResultUM.Status.Failed,
                total = stringReference("$45.50"),
                serviceFee = stringReference("(15%) $1.40"),
                networkFee = stringReference("$1.40"),
            ),
            onGoToTokenClick = {},
            onGoToYieldClick = {},
        )
    }
}