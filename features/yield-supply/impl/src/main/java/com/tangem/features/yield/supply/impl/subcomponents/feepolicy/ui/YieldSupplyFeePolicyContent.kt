package com.tangem.features.yield.supply.impl.subcomponents.feepolicy.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Suppress("LongMethod")
@Composable
internal fun YieldSupplyFeePolicyContent(
    yieldSupplyFeeUM: YieldSupplyFeeUM,
    tokenSymbol: String,
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
        MinFeeNote(yieldSupplyFeeUM)
        SpacerH16()
        val feeNote = when (yieldSupplyFeeUM) {
            is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.feeNoteValue
            YieldSupplyFeeUM.Error -> null
            YieldSupplyFeeUM.Loading -> null
        }
        FooterContainer(
            footer = feeNote,
            paddingValues = PaddingValues(
                top = 8.dp,
                start = 12.dp,
                end = 12.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TangemTheme.colors.background.action)
                    .padding(horizontal = 16.dp),
            ) {
                val currentFee = when (yieldSupplyFeeUM) {
                    is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.estimatedFiatValue
                    YieldSupplyFeeUM.Error -> stringReference(StringsSigns.DASH_SIGN)
                    YieldSupplyFeeUM.Loading -> null
                }
                YieldSupplyFeeRow(
                    title = resourceReference(R.string.common_estimated_fee),
                    value = currentFee,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                val maxFee = when (yieldSupplyFeeUM) {
                    is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.maxNetworkFeeFiatValue
                    YieldSupplyFeeUM.Error -> stringReference(StringsSigns.DASH_SIGN)
                    YieldSupplyFeeUM.Loading -> null
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = TangemTheme.colors.stroke.primary,
                )
                YieldSupplyFeeRow(
                    title = resourceReference(R.string.yield_module_fee_policy_sheet_max_fee_title),
                    value = maxFee,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        SpacerH8()
        Text(
            text = stringResourceSafe(R.string.yield_module_fee_policy_tangem_service_fee_title),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
        )
    }
}

@Composable
private fun MinFeeNote(yieldSupplyFeeUM: YieldSupplyFeeUM) {
    val minFeeNote = when (yieldSupplyFeeUM) {
        is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.minFeeNoteValue
        YieldSupplyFeeUM.Error -> null
        YieldSupplyFeeUM.Loading -> null
    }
    FooterContainer(
        footer = minFeeNote,
        paddingValues = PaddingValues(
            top = 8.dp,
            start = 12.dp,
            end = 12.dp,
        ),
    ) {
        val minAmount = when (yieldSupplyFeeUM) {
            is YieldSupplyFeeUM.Content -> yieldSupplyFeeUM.minTopUpFiatValue
            YieldSupplyFeeUM.Error -> stringReference(StringsSigns.DASH_SIGN)
            YieldSupplyFeeUM.Loading -> null
        }
        YieldSupplyFeeRow(
            title = resourceReference(R.string.yield_module_fee_policy_sheet_min_amount_title),
            value = minAmount,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TangemTheme.colors.background.action)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
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
                feeFiatValue = stringReference("$1.45"),
                maxNetworkFeeFiatValue = stringReference("$8.50"),
                minTopUpFiatValue = stringReference("$50"),
                feeNoteValue = resourceReference(
                    id = R.string.yield_module_fee_policy_sheet_fee_note,
                    formatArgs = wrappedList(
                        stringReference("$1.45"),
                        stringReference("1.46 USDT"),
                        stringReference("$8.50"),
                        stringReference("8.50 USDT"),
                    ),
                ),
                minFeeNoteValue = resourceReference(
                    id = R.string.yield_module_fee_policy_sheet_min_amount_note,
                    formatArgs = wrappedList(
                        stringReference("$2.45"),
                        stringReference("2.46 USDT"),
                    ),
                ),
                estimatedFiatValue = stringReference("$8.50"),
            ),
            tokenSymbol = "USDT",
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        )
    }
}
// endregion