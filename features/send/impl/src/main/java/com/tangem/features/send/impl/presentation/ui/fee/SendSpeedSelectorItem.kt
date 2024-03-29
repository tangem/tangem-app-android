package com.tangem.features.send.impl.presentation.ui.fee

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import java.math.BigDecimal

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
    val hasCustomValues = !content?.customValues.isNullOrEmpty()
    AnimatedVisibility(
        visible = isVisible || hasCustomValues,
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
                postDot = getFiatReference(amount, state.rate, state.appCurrency),
                ellipsizeOffset = amount?.currencySymbol?.length,
                isSelected = content?.selectedFee == feeType,
                showDivider = showDivider,
            )
            SendSpeedSelectorItemError(isError = feeSelectorState is FeeSelectorState.Error)

            if (feeType == FeeType.Custom) {
                val showWarning = state.notifications.any { it is SendNotification.Warning.TooHigh }
                WarningIcon(showWarning = showWarning)
            }
        }
    }
}

// todo remove after refactoring [REDACTED_JIRA]
private fun getCryptoReference(amount: Amount?, isFeeApproximate: Boolean): TextReference? {
    if (amount == null) return null
    return combinedReference(
        if (isFeeApproximate) stringReference("${BigDecimalFormatter.CAN_BE_LOWER_SIGN} ") else TextReference.EMPTY,
        stringReference(
            BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = amount.value,
                cryptoCurrency = amount.currencySymbol,
                decimals = amount.decimals,
            ),
        ),
    )
}

// todo remove after refactoring [REDACTED_JIRA]
private fun getFiatReference(amount: Amount?, rate: BigDecimal?, appCurrency: AppCurrency): TextReference? {
    if (amount == null) return null
    return stringReference(
        BigDecimalFormatter.formatFiatAmount(
            fiatAmount = rate?.let { amount.value?.multiply(it) },
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        ),
    )
}

@Composable
private fun SendSpeedSelectorItemError(isError: Boolean) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = isError,
            label = "Error state indication animation",
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing14,
                        horizontal = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}

@Composable
private fun WarningIcon(showWarning: Boolean = false) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = showWarning,
            label = "Custom fee warning indicator",
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_alert_triangle_20),
                contentDescription = null,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing12,
                        horizontal = TangemTheme.dimens.spacing14,
                    ),
            )
        }
    }
}

private fun FeeSelectorState.Content.getAmount(feeType: FeeType): Amount? {
    val choosableFees = fees as? TransactionFee.Choosable
    return when (feeType) {
        FeeType.Slow -> choosableFees?.minimum?.amount
        FeeType.Market -> fees.normal.amount
        FeeType.Fast -> choosableFees?.priority?.amount
        FeeType.Custom -> null
    }
}

private fun FeeSelectorState.Content?.getDividerAndVisibility(feeType: FeeType): Pair<Boolean, Boolean> {
    val hasCustomValues = !this?.customValues.isNullOrEmpty()
    val isNotSingle = this?.fees !is TransactionFee.Single
    val isSingle = this?.fees is TransactionFee.Single
    return when (feeType) {
        FeeType.Slow -> true to isNotSingle
        FeeType.Market -> isNotSingle to isSingle
        FeeType.Fast -> hasCustomValues to isNotSingle
        FeeType.Custom -> false to isNotSingle
    }
}