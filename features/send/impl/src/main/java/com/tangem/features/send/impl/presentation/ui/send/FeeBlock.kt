package com.tangem.features.send.impl.presentation.ui.send

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
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.previewdata.FeeStatePreviewData
import com.tangem.features.send.impl.presentation.utils.getCryptoReference
import com.tangem.features.send.impl.presentation.utils.getFiatReference

@Composable
internal fun FeeBlock(feeState: SendStates.FeeState, isClickDisabled: Boolean, onClick: () -> Unit) {
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
            val feeSelectorState = feeState.feeSelectorState
            val feeAmount = feeState.fee?.amount
            val (title, icon) = if (feeSelectorState is FeeSelectorState.Content) {
                when (feeSelectorState.selectedFee) {
                    FeeType.Slow -> R.string.common_fee_selector_option_slow to R.drawable.ic_tortoise_24
                    FeeType.Market -> R.string.common_fee_selector_option_market to R.drawable.ic_bird_24
                    FeeType.Fast -> R.string.common_fee_selector_option_fast to R.drawable.ic_hare_24
                    FeeType.Custom -> R.string.common_fee_selector_option_custom to R.drawable.ic_edit_24
                }
            } else {
                R.string.common_fee_selector_option_market to R.drawable.ic_bird_24
            }
            SelectorRowItem(
                titleRes = title,
                iconRes = icon,
                preDot = getCryptoReference(feeAmount, feeState.isFeeApproximate),
                postDot = getFiatReference(feeAmount?.value, feeState.rate, feeState.appCurrency),
                ellipsizeOffset = feeAmount?.currencySymbol?.length,
                isSelected = true,
                showDivider = false,
                paddingValues = PaddingValues(),
            )
            FeeLoading(feeSelectorState)
            FeeError(feeSelectorState)
        }
    }
}

@Composable
private fun BoxScope.FeeLoading(feeSelectorState: FeeSelectorState) {
    AnimatedContent(
        targetState = feeSelectorState,
        label = "Fee Loading State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == FeeSelectorState.Loading) {
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
private fun BoxScope.FeeError(feeSelectorState: FeeSelectorState) {
    AnimatedContent(
        targetState = feeSelectorState,
        label = "Fee Error State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == FeeSelectorState.Error) {
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
@Composable
private fun FeeBlockPreview_Light(@PreviewParameter(FeeBlockPreviewProvider::class) value: SendStates.FeeState) {
    TangemTheme {
        FeeBlock(
            feeState = value,
            isClickDisabled = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun FeeBlockPreview_Dark(@PreviewParameter(FeeBlockPreviewProvider::class) value: SendStates.FeeState) {
    TangemTheme(isDark = true) {
        FeeBlock(
            feeState = value,
            isClickDisabled = true,
            onClick = {},
        )
    }
}

private class FeeBlockPreviewProvider : PreviewParameterProvider<SendStates.FeeState> {

    override val values: Sequence<SendStates.FeeState>
        get() = sequenceOf(
            FeeStatePreviewData.feeState,
        )
}
// endregion
