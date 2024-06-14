package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.utils.getCryptoReference
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.staking.impl.presentation.state.StakingFeeSelectorState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import java.math.BigDecimal

@Composable
internal fun FeeBlock(feeState: StakingStates.FeeState, isClickDisabled: Boolean, onClick: () -> Unit) {
    if (feeState !is StakingStates.FeeState.Data) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isClickDisabled, onClick = onClick)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResource(R.string.common_network_fee_title),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )

        Box(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
        ) {
            val feeAmount = feeState.fee?.amount
            val (title, icon) = R.string.common_fee_selector_option_market to R.drawable.ic_bird_24
            SelectorRowItem(
                titleRes = title,
                iconRes = icon,
                preDot = getCryptoReference(feeAmount, feeState.isFeeApproximate),
                postDot = if (feeState.isFeeConvertibleToFiat) {
                    getFiatReference(feeAmount?.value, feeState.rate, feeState.appCurrency)
                } else {
                    null
                },
                ellipsizeOffset = feeAmount?.currencySymbol?.length,
                isSelected = true,
                showDivider = false,
                showSelectedAppearance = false,
                paddingValues = PaddingValues(),
            )
            FeeLoading(feeState.feeSelectorState)
            FeeError(feeState.feeSelectorState)
        }
    }
}

@Composable
private fun BoxScope.FeeLoading(feeSelectorState: StakingFeeSelectorState) {
    AnimatedContent(
        targetState = feeSelectorState,
        label = "Fee Loading State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == StakingFeeSelectorState.Loading) {
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier.size(
                    height = TangemTheme.dimens.size12,
                    width = TangemTheme.dimens.size90,
                ),
            )
        }
    }
}

@Composable
private fun BoxScope.FeeError(feeSelectorState: StakingFeeSelectorState) {
    AnimatedContent(
        targetState = feeSelectorState,
        label = "Fee Error State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == StakingFeeSelectorState.Error) {
            Text(
                text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

// region Preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeBlockPreview(@PreviewParameter(FeeBlockPreviewProvider::class) value: StakingStates.FeeState) {
    TangemThemePreview {
        FeeBlock(
            feeState = value,
            isClickDisabled = true,
            onClick = {},
        )
    }
}

private class FeeBlockPreviewProvider : PreviewParameterProvider<StakingStates.FeeState> {

    override val values: Sequence<StakingStates.FeeState>
        get() = sequenceOf(
            feeState,
        )

    private val fee = Fee.Common(
        amount = Amount(
            currencySymbol = "MATIC",
            value = BigDecimal(0.159806),
            decimals = 18,
            type = AmountType.Coin,
        ),
    )

    private val feeState = StakingStates.FeeState.Data(
        isPrimaryButtonEnabled = false,
        feeSelectorState = StakingFeeSelectorState.Content(TransactionFee.Single(normal = fee)),
        fee = fee,
        rate = BigDecimal.ONE,
        appCurrency = AppCurrency.Default,
        isFeeApproximate = false,
        isFeeConvertibleToFiat = true,
    )
}

// endregion
