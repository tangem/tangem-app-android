package com.tangem.features.send.impl.presentation.ui.send

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.previewdata.FeeStatePreviewData

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
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.subtitle1,
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
                    FeeType.Custom -> R.string.common_custom to R.drawable.ic_edit_24
                }
            } else {
                R.string.common_fee_selector_option_market to R.drawable.ic_bird_24
            }
            SelectorRowItem(
                titleRes = title,
                iconRes = icon,
                preDot = stringReference(
                    feeAmount?.value.format {
                        crypto(
                            symbol = feeAmount?.currencySymbol.orEmpty(),
                            decimals = feeAmount?.decimals ?: 0,
                        ).fee(canBeLower = feeState.isFeeApproximate)
                    },
                ),
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
        if (it is FeeSelectorState.Error) {
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
private fun FeeBlockPreview(@PreviewParameter(FeeBlockPreviewProvider::class) value: SendStates.FeeState) {
    TangemThemePreview {
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