package com.tangem.features.send.impl.presentation.ui.fee

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.utils.getCryptoReference
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType

@Composable
internal fun SendSpeedSelectorItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    feeType: FeeType,
    state: SendStates.FeeState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feeSelectorState = state.feeSelectorState
    val content = feeSelectorState as? FeeSelectorState.Content
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
                titleRes = titleRes,
                iconRes = iconRes,
                onSelect = onSelect,
                modifier = modifier,
                preDot = getCryptoReference(amount, state.isFeeApproximate),
                postDot = if (state.isFeeConvertibleToFiat) {
                    getFiatReference(amount?.value, state.rate, state.appCurrency)
                } else {
                    null
                },
                ellipsizeOffset = amount?.currencySymbol?.length,
                isSelected = content?.selectedFee == feeType,
                showDivider = showDivider,
            )
            FeeLoading(feeSelectorState)
            FeeError(feeSelectorState)
        }
    }
}

@Composable
private fun FeeLoading(feeSelectorState: FeeSelectorState) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = feeSelectorState == FeeSelectorState.Loading,
            label = "Fee Loading State Change",
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing18,
                        horizontal = TangemTheme.dimens.spacing12,
                    )
                    .size(
                        height = TangemTheme.dimens.size12,
                        width = TangemTheme.dimens.size90,
                    ),
            )
        }
    }
}

@Composable
private fun FeeError(feeSelectorState: FeeSelectorState) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = feeSelectorState is FeeSelectorState.Error,
            label = "Fee Error State Change",
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            Text(
                text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body2,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing14,
                        horizontal = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}

private fun FeeSelectorState.Content.getAmount(feeType: FeeType): Amount? {
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

private fun FeeSelectorState.Content?.getDividerAndVisibility(feeType: FeeType): Pair<Boolean, Boolean> {
    val hasCustomValues = !this?.customValues.isNullOrEmpty()
    val isNotSingle = this?.fees !is TransactionFee.Single
    return when (feeType) {
        FeeType.Slow -> true to isNotSingle
        FeeType.Market -> (isNotSingle || hasCustomValues) to true
        FeeType.Fast -> hasCustomValues to isNotSingle
        FeeType.Custom -> false to hasCustomValues
    }
}