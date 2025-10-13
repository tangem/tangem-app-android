package com.tangem.features.yield.supply.impl.subcomponents.feepolicy.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.common.ui.YieldSupplyFeeRow
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun YieldSupplyFeePolicyContent(
    yieldSupplyFeeUM: YieldSupplyFeeUM,
    tokenSymbol: String,
    networkName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 8.dp,
            ),
    ) {
        Text(
            text = stringResourceSafe(R.string.yield_module_fee_policy_sheet_title),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH8()
        Text(
            text = stringResourceSafe(R.string.yield_module_fee_policy_sheet_description, tokenSymbol),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SpacerH24()
        FooterContainer(
            footer = resourceReference(
                id = R.string.yield_module_fee_policy_sheet_current_fee_note,
                formatArgs = wrappedList(networkName),
            ),
            paddingValues = PaddingValues(
                top = 8.dp,
                start = 12.dp,
                end = 12.dp,
            ),
        ) {
            val currentFee = when (yieldSupplyFeeUM) {
                is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.currentNetworkFeeValue
                YieldSupplyFeeUM.Error -> stringReference(StringsSigns.DASH_SIGN)
                YieldSupplyFeeUM.Loading -> null
            }
            YieldSupplyFeeRow(
                title = resourceReference(R.string.yield_module_fee_policy_sheet_current_fee_title),
                value = currentFee,
            )
        }
        SpacerH16()
        FooterContainer(
            footer = resourceReference(R.string.yield_module_fee_policy_sheet_max_fee_note),
            paddingValues = PaddingValues(
                top = 8.dp,
                start = 12.dp,
                end = 12.dp,
            ),
        ) {
            val maxFee = when (yieldSupplyFeeUM) {
                is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.maxNetworkFeeValue
                YieldSupplyFeeUM.Error -> stringReference(StringsSigns.DASH_SIGN)
                YieldSupplyFeeUM.Loading -> null
            }
            YieldSupplyFeeRow(
                title = resourceReference(R.string.yield_module_fee_policy_sheet_max_fee_title),
                value = maxFee,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyFeePolicyContent_Preview() {
    TangemThemePreview {
        YieldSupplyFeePolicyContent(
            yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                transactionDataList = persistentListOf(),
                feeValue = stringReference("0.0001 ETH • \$1.45"),
                maxNetworkFeeValue = stringReference("8.50 USDT • \$8.50"),
                currentNetworkFeeValue = stringReference("1.45 USDT • \$1.45"),
            ),
            tokenSymbol = "USDT",
            networkName = "Ethereum",
            modifier = Modifier.background(TangemTheme.colors.background.primary),

        )
    }
}
// endregion