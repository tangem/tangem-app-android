package com.tangem.features.send.impl.presentation.ui.send

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.previewdata.FeeStatePreviewData

@Composable
internal fun FeeBlock(feeState: SendStates.FeeState, isSuccess: Boolean, onClick: () -> Unit) {
    val fee = feeState.fee ?: return
    val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return
    val (title, icon) = when (feeSelectorState.selectedFee) {
        FeeType.Slow -> R.string.common_fee_selector_option_slow to R.drawable.ic_tortoise_24
        FeeType.Market -> R.string.common_fee_selector_option_market to R.drawable.ic_bird_24
        FeeType.Fast -> R.string.common_fee_selector_option_fast to R.drawable.ic_hare_24
        FeeType.Custom -> R.string.common_fee_selector_option_custom to R.drawable.ic_edit_24
    }
    val feeCryptoValue = BigDecimalFormatter.formatCryptoAmount(
        cryptoAmount = fee.amount.value,
        cryptoCurrency = fee.amount.currencySymbol,
        decimals = fee.amount.decimals,
    )
    val feeFiatValue = BigDecimalFormatter.formatFiatAmount(
        fiatAmount = feeState.rate?.let { fee.amount.value?.multiply(it) },
        fiatCurrencyCode = feeState.appCurrency.code,
        fiatCurrencySymbol = feeState.appCurrency.symbol,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isSuccess) { onClick() }
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResource(R.string.common_network_fee_title),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )
        SelectorRowItem(
            titleRes = title,
            iconRes = icon,
            preDot = stringReference(feeCryptoValue),
            postDot = stringReference(feeFiatValue),
            ellipsizeOffset = fee.amount.currencySymbol.length,
            isSelected = true,
            showDivider = false,
            paddingValues = PaddingValues(),
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
        )
    }
}

// region Preview
@Preview
@Composable
private fun FeeBlockPreview_Light(@PreviewParameter(FeeBlockPreviewProvider::class) value: SendStates.FeeState) {
    TangemTheme {
        FeeBlock(
            feeState = value,
            isSuccess = true,
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
            isSuccess = true,
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