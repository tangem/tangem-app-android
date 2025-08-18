package com.tangem.features.send.v2.subcomponents.fee.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

@Composable
internal fun SendSpeedSelectorItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    feeType: FeeType,
    state: FeeUM.Content,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feeSelectorState = state.feeSelectorUM
    val content = feeSelectorState as? FeeSelectorUM.Content
    val amount = content?.getAmount(feeType)
    val (showDivider, isVisible) = content.getDividerAndVisibility(feeType)
    AnimatedVisibility(
        visible = isVisible,
        label = "Fee Selector Visibility Animation",
        enter = expandVertically().plus(fadeIn()),
        exit = shrinkVertically().plus(fadeOut()),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        ) {
            SelectorRowItem(
                title = resourceReference(titleRes),
                iconRes = iconRes,
                onSelect = onSelect,
                modifier = modifier,
                preDot = stringReference(
                    amount?.value.format {
                        crypto(
                            symbol = amount?.currencySymbol.orEmpty(),
                            decimals = amount?.decimals ?: 0,
                        ).fee(canBeLower = state.isFeeApproximate)
                    },
                ),
                postDot = if (state.isFeeConvertibleToFiat) {
                    getFiatReference(amount?.value, state.rate, state.appCurrency)
                } else {
                    null
                },
                ellipsizeOffset = amount?.currencySymbol?.length,
                isSelected = content?.selectedType == feeType,
                showDivider = showDivider,
            )
            FeeLoading(feeSelectorState)
            FeeError(feeSelectorState)
        }
    }
}

@Composable
private fun FeeLoading(feeSelectorState: FeeSelectorUM) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = feeSelectorState == FeeSelectorUM.Loading,
            label = "Fee Loading State Change",
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            RectangleShimmer(
                radius = 3.dp,
                modifier = Modifier
                    .padding(
                        vertical = 18.dp,
                        horizontal = 12.dp,
                    )
                    .size(
                        height = 12.dp,
                        width = 90.dp,
                    ),
            )
        }
    }
}

@Composable
private fun FeeError(feeSelectorState: FeeSelectorUM) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = feeSelectorState is FeeSelectorUM.Error,
            label = "Fee Error State Change",
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            Text(
                text = EMPTY_BALANCE_SIGN,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body2,
                modifier = Modifier
                    .padding(
                        vertical = 14.dp,
                        horizontal = 12.dp,
                    ),
            )
        }
    }
}

private fun FeeSelectorUM.Content.getAmount(feeType: FeeType): Amount? {
    val choosableFees = fees as? TransactionFee.Choosable
    val decimals = fees.normal.amount.decimals
    val customValue = this.customValues.firstOrNull()?.value?.parseToBigDecimal(decimals)
    val customAmount = fees.normal.amount.copy(value = customValue)
    return when (feeType) {
        FeeType.Slow -> choosableFees?.minimum?.amount
        FeeType.Market -> fees.normal.amount
        FeeType.Fast -> choosableFees?.priority?.amount
        FeeType.Custom -> customAmount
    }
}

private fun FeeSelectorUM.Content?.getDividerAndVisibility(feeType: FeeType): Pair<Boolean, Boolean> {
    val hasCustomValues = !this?.customValues.isNullOrEmpty()
    val isNotSingle = this?.fees !is TransactionFee.Single
    return when (feeType) {
        FeeType.Slow -> true to isNotSingle
        FeeType.Market -> (isNotSingle || hasCustomValues) to true
        FeeType.Fast -> hasCustomValues to isNotSingle
        FeeType.Custom -> false to hasCustomValues
    }
}