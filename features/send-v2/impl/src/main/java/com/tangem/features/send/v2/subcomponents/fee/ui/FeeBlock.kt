package com.tangem.features.send.v2.subcomponents.fee.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

@Composable
internal fun FeeBlock(feeUM: FeeUM, isClickEnabled: Boolean, onClick: () -> Unit) {
    val feeUM = feeUM as? FeeUM.Content ?: return
    val feeSelectorUM = feeUM.feeSelectorUM as? FeeSelectorUM.Content
    val isEditingDisabled = feeSelectorUM?.fees is TransactionFee.Single

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = isClickEnabled && !isEditingDisabled, onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.secondary,
        )

        Box(
            modifier = Modifier.padding(top = 8.dp),
        ) {
            val feeSelectorUM = feeUM.feeSelectorUM
            val feeAmount = (feeSelectorUM as? FeeSelectorUM.Content)?.selectedFee?.amount
            val (title, icon) = if (feeSelectorUM is FeeSelectorUM.Content) {
                when (feeSelectorUM.selectedType) {
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
                        ).fee(canBeLower = feeUM.isFeeApproximate)
                    },
                ),
                postDot = if (feeUM.isFeeConvertibleToFiat) {
                    getFiatReference(feeAmount?.value, feeUM.rate, feeUM.appCurrency)
                } else {
                    null
                },
                ellipsizeOffset = feeAmount?.currencySymbol?.length,
                isSelected = true,
                showDivider = false,
                showSelectedAppearance = false,
                paddingValues = PaddingValues(),
            )
            FeeLoading(feeSelectorUM)
            FeeError(feeSelectorUM)
        }
    }
}

@Composable
private fun BoxScope.FeeLoading(feeSelectorUM: FeeSelectorUM) {
    AnimatedContent(
        targetState = feeSelectorUM,
        label = "Fee Loading State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it == FeeSelectorUM.Loading) {
            RectangleShimmer(
                radius = 3.dp,
                modifier = Modifier.size(
                    height = 12.dp,
                    width = 90.dp,
                ),
            )
        }
    }
}

@Composable
private fun BoxScope.FeeError(feeSelectorUM: FeeSelectorUM) {
    AnimatedContent(
        targetState = feeSelectorUM,
        label = "Fee Error State Change",
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        if (it is FeeSelectorUM.Error) {
            Text(
                text = EMPTY_BALANCE_SIGN,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body2,
            )
        }
    }
}