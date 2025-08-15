package com.tangem.features.staking.impl.presentation.ui.block

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.R
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
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal

@Composable
internal fun StakingFeeBlock(feeState: FeeState) {
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
            when (feeState) {
                is FeeState.Content -> {
                    val feeAmount = feeState.fee?.amount
                    SelectorRowItem(
                        titleRes = R.string.common_fee_selector_option_market,
                        iconRes = R.drawable.ic_bird_24,
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
                }
                is FeeState.Loading -> {
                    SelectorRowItem(
                        titleRes = R.string.common_fee_selector_option_market,
                        iconRes = R.drawable.ic_bird_24,
                        isSelected = true,
                        paddingValues = PaddingValues(),
                        showDivider = false,
                    )
                    FeeLoading(feeState)
                }
                is FeeState.Error -> {
                    SelectorRowItem(
                        titleRes = R.string.common_fee_selector_option_market,
                        iconRes = R.drawable.ic_bird_24,
                        isSelected = true,
                        paddingValues = PaddingValues(),
                        showDivider = false,
                    )
                    FeeError(feeState)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FeeLoading(feeState: FeeState) {
    AnimatedContent(
        targetState = feeState,
        label = "Fee Loading State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == FeeState.Loading) {
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier.size(
                    height = TangemTheme.dimens.size24,
                    width = TangemTheme.dimens.size90,
                ),
            )
        }
    }
}

@Composable
private fun BoxScope.FeeError(feeState: FeeState) {
    AnimatedContent(
        targetState = feeState,
        label = "Fee Error State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == FeeState.Error) {
            Text(
                text = DASH_SIGN,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body1,
            )
        }
    }
}

// region Preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeBlockPreview(@PreviewParameter(FeeBlockPreviewProvider::class) value: FeeState) {
    TangemThemePreview {
        StakingFeeBlock(feeState = value)
    }
}

private class FeeBlockPreviewProvider : PreviewParameterProvider<FeeState> {

    override val values: Sequence<FeeState>
        get() = sequenceOf(
            contentState,
            FeeState.Loading,
            FeeState.Error,
        )

    private val fee = Fee.Common(
        amount = Amount(
            currencySymbol = "MATIC",
            value = BigDecimal(0.159806),
            decimals = 18,
            type = AmountType.Coin,
        ),
    )

    private val contentState = FeeState.Content(
        fee = fee,
        rate = BigDecimal.ONE,
        appCurrency = AppCurrency.Default,
        isFeeApproximate = false,
        isFeeConvertibleToFiat = true,
    )
}

// endregion