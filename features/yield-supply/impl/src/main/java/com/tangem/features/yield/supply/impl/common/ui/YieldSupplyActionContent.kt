package com.tangem.features.yield.supply.impl.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun YieldSupplyActionContent(
    yieldSupplyActionUM: YieldSupplyActionUM,
    modifier: Modifier = Modifier,
    iconContent: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
    ) {
        iconContent()
        SpacerH24()
        Text(
            text = yieldSupplyActionUM.title.resolveReference(),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH8()
        Text(
            text = yieldSupplyActionUM.subtitle.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SpacerH24()
        FooterContainer(
            footer = yieldSupplyActionUM.footer,
            paddingValues = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TangemTheme.colors.background.action)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResourceSafe(R.string.common_network_fee_title),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerWMax()
                when (val fee = yieldSupplyActionUM.yieldSupplyFeeUM) {
                    is YieldSupplyFeeUM.Content -> Text(
                        text = fee.feeValue.resolveReference(),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.tertiary,
                    )
                    YieldSupplyFeeUM.Error -> Text(
                        text = stringResourceSafe(R.string.common_fee_error),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.attention,
                    )
                    YieldSupplyFeeUM.Loading -> {
                        TextShimmer(
                            style = TangemTheme.typography.body1,
                            text = stringResourceSafe(R.string.common_fee_error),
                        )
                    }
                }
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 344)
@Preview(showBackground = true, widthDp = 344, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyActionContent_Preview(
    @PreviewParameter(YieldSupplyActionContentPreviewProvider::class) params: YieldSupplyActionUM,
) {
    TangemThemePreview {
        YieldSupplyActionContent(
            yieldSupplyActionUM = params,
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
        ) {
            CurrencyIcon(
                state = CurrencyIconState.Loading,
                shouldDisplayNetwork = false,
                iconSize = 48.dp,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

private class YieldSupplyActionContentPreviewProvider : PreviewParameterProvider<YieldSupplyActionUM> {
    override val values: Sequence<YieldSupplyActionUM>
        get() = sequenceOf(
            YieldSupplyActionUM(
                title = resourceReference(R.string.yield_module_start_earning),
                subtitle = resourceReference(
                    R.string.yield_module_start_earning_sheet_description,
                    wrappedList("USDT"),
                ),
                footer = combinedReference(
                    resourceReference(R.string.yield_module_start_earning_sheet_next_deposits),
                    stringReference(StringsSigns.WHITE_SPACE),
                    resourceReference(R.string.yield_module_start_earning_sheet_fee_policy),
                ),
                currencyIconState = CurrencyIconState.Loading,
                yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                    transactionDataList = persistentListOf(),
                    feeValue = stringReference("0.00020 ETH • \$0.99"),
                ),
                isPrimaryButtonEnabled = false,
            ),
        )
}
// endregion