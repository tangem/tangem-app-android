package com.tangem.common.ui.amountScreen.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BaseAmountBlockTestTags
import java.math.BigDecimal

@Composable
fun AmountBlock(amountState: AmountState, isClickDisabled: Boolean, isEditingDisabled: Boolean, onClick: () -> Unit) {
    if (amountState !is AmountState.Data) return
    val amount = amountState.amountTextField

    val cryptoAmount = formatWithSymbol(amount.value, amount.cryptoAmount.currencySymbol)
    val fiatAmount = amount.fiatAmount.value.format {
        fiat(
            fiatCurrencySymbol = amount.fiatAmount.currencySymbol,
            fiatCurrencyCode = amountState.appCurrency.code,
        )
    }

    val (firstAmount, secondAmount) = if (amount.isFiatValue) {
        fiatAmount to cryptoAmount
    } else {
        cryptoAmount to fiatAmount
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isClickDisabled && !isEditingDisabled, onClick = onClick)
            .padding(TangemTheme.dimens.spacing16),
    ) {
        CurrencyIcon(state = amountState.tokenIconState)
        ResizableText(
            text = firstAmount,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing24)
                .testTag(BaseAmountBlockTestTags.PRIMARY_AMOUNT),
        )
        Text(
            text = secondAmount,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing8)
                .testTag(BaseAmountBlockTestTags.SECONDARY_AMOUNT),
        )
    }
}

fun formatWithSymbol(amount: String, symbol: String) =
    BigDecimal.ZERO.format { crypto(symbol, 0).anyDecimals() }.replace("0", amount)

// region Preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AmountBlockPreview(@PreviewParameter(AmountBlockPreviewProvider::class) value: AmountState) {
    TangemThemePreview {
        AmountBlock(
            amountState = value,
            isClickDisabled = false,
            isEditingDisabled = false,
            onClick = {},
        )
    }
}

private class AmountBlockPreviewProvider : PreviewParameterProvider<AmountState> {
    override val values: Sequence<AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.amountState,
        )
}
// endregion