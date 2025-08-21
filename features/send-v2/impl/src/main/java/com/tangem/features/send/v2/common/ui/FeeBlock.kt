package com.tangem.features.send.v2.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.impl.R

@Composable
internal fun FeeBlock(feeSelectorUM: FeeSelectorUM) {
    if (feeSelectorUM !is FeeSelectorUM.Content) return
    val feeExtraInfo = feeSelectorUM.feeExtraInfo
    val feeFiatRateUM = feeSelectorUM.feeFiatRateUM
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )

        Box(modifier = Modifier.padding(top = TangemTheme.dimens.spacing8)) {
            val feeItemUM = feeSelectorUM.selectedFeeItem
            val feeAmount = feeItemUM.fee.amount
            SelectorRowItem(
                title = feeItemUM.title,
                iconRes = feeItemUM.iconRes,
                preDot = remember {
                    stringReference(
                        feeAmount.value.format {
                            crypto(
                                symbol = feeAmount.currencySymbol,
                                decimals = feeAmount.decimals,
                            ).fee(canBeLower = feeExtraInfo.isFeeApproximate)
                        },
                    )
                },
                postDot = remember {
                    if (feeExtraInfo.isFeeConvertibleToFiat && feeFiatRateUM != null) {
                        getFiatReference(feeAmount.value, feeFiatRateUM.rate, feeFiatRateUM.appCurrency)
                    } else {
                        null
                    }
                },
                ellipsizeOffset = feeAmount.currencySymbol.length,
                isSelected = true,
                showDivider = false,
                showSelectedAppearance = false,
                paddingValues = PaddingValues(),
            )
        }
    }
}