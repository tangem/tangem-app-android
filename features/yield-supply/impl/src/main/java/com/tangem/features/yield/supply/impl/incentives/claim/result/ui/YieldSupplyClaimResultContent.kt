package com.tangem.features.yield.supply.impl.incentives.claim.result.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.ui.YieldSupplyFeeRow
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.utils.StringsSigns.WHITE_SPACE
import com.tangem.core.res.R as CoreResR

/**
 * What became of a signed claim, on the design-system status template: the outcome colours the ambient light and the
 * status asset, the fees sit above the footer, and the two actions share one row.
 *
 * The screen runs on the redesigned palette the template is built from, so it provides it for the legacy fee rows too.
 */
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
            },
            title = title(status = status, total = state.total),
            description = description(status = status),
            asset = {
                TangemStatusIcon(kind = TangemStatusIcon.Kind.Checkmark, modifier = Modifier.size(ASSET_SIZE))
            },
            details = { Fees(state = state) },
            footer = { Footer(onGoToTokenClick = onGoToTokenClick, onGoToYieldClick = onGoToYieldClick) },
        )
    }
}

/**
 * The claimed amount is data, so it is appended rather than interpolated into a translated sentence — gluing it in
 * would fix the English word order for every language.
 */
private fun title(status: YieldSupplyClaimResultUM.Status, total: TextReference): TextReference = when (status) {
    is YieldSupplyClaimResultUM.Status.Submitted -> combinedReference(
        resourceReference(R.string.yield_module_claim_in_progress_title),
        stringReference(LINE_BREAK),
        total,
    )
    is YieldSupplyClaimResultUM.Status.Confirmed -> combinedReference(
        resourceReference(R.string.yield_module_claim_success_toast),
        stringReference(WHITE_SPACE),
        total,
    )
}

private fun description(status: YieldSupplyClaimResultUM.Status): TextReference = when (status) {
    is YieldSupplyClaimResultUM.Status.Submitted -> resourceReference(
        R.string.yield_module_claim_in_progress_description,
    )
    is YieldSupplyClaimResultUM.Status.Confirmed -> resourceReference(
        id = R.string.send_date_format,
        formatArgs = wrappedList(
            status.timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
            status.timestamp.toTimeFormat(),
        ),
    )
}

/** Plain rows rather than the sheet's card: the template already lays the details block out as its own block. */
@Composable
private fun Fees(state: YieldSupplyClaimResultUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        YieldSupplyFeeRow(
            title = resourceReference(CoreResR.string.common_network_fee_title),
            value = state.networkFee,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
        HorizontalDivider(thickness = 0.5.dp, color = TangemTheme.colors.stroke.primary)
        YieldSupplyFeeRow(
            title = resourceReference(CoreResR.string.yield_module_claim_service_fee_title),
            value = state.serviceFee,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
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

private const val LINE_BREAK = "\n"

private val ASSET_SIZE = 40.dp

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