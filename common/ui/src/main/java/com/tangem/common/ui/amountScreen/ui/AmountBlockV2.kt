package com.tangem.common.ui.amountScreen.ui

import android.content.res.Configuration
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
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.uncapped
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun AmountBlockV2(
    amountState: AmountState,
    isClickDisabled: Boolean,
    isEditingDisabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable () -> Unit = {},
) {
    if (amountState !is AmountState.Data) return
    val amount = amountState.amountTextField

    val cryptoAmount = amount.cryptoAmount.value?.format {
        crypto(
            symbol = "",
            decimals = amount.cryptoAmount.decimals,
        ).uncapped()
    }.orEmpty()

    val fiatAmount = amount.fiatAmount.value.format {
        fiat(
            fiatCurrencySymbol = amountState.appCurrency.symbol,
            fiatCurrencyCode = amountState.appCurrency.code,
        )
    }

    val (firstAmount, secondAmount) = if (amount.isFiatValue) {
        fiatAmount to cryptoAmount
    } else {
        cryptoAmount to fiatAmount
    }
    val currencyTitle = amount.cryptoAmount.currencySymbol

    AmountBlockV2(
        title = amountState.title,
        balance = amountState.availableBalanceCrypto,
        currencyTitle = currencyTitle,
        currencyIconState = amountState.tokenIconState,
        firstAmount = firstAmount,
        secondAmount = secondAmount,
        isClickDisabled = isClickDisabled,
        isEditingDisabled = isEditingDisabled,
        onClick = onClick,
        modifier = modifier,
        extraContent = extraContent,
    )
}

@Suppress("LongParameterList")
@Composable
private fun AmountBlockV2(
    title: TextReference,
    balance: TextReference,
    currencyTitle: String,
    currencyIconState: CurrencyIconState,
    firstAmount: String,
    secondAmount: String,
    isClickDisabled: Boolean,
    isEditingDisabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .conditional(onClick != null) {
                clickable(enabled = !isClickDisabled && !isEditingDisabled, onClick = onClick!!)
            }
            .padding(TangemTheme.dimens.spacing16),
    ) {
        Row {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
            Text(
                text = balance.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp).weight(1f),
            ) {
                ResizableText(
                    text = firstAmount,
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ResizableText(
                        text = secondAmount,
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                        maxLines = 1,
                    )
                    extraContent()
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                CurrencyIcon(
                    state = currencyIconState,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Text(
                    text = currencyTitle,
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

// region Preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AmountBlockPreview(@PreviewParameter(AmountBlockV2PreviewProvider::class) value: AmountState) {
    TangemThemePreview {
        AmountBlockV2(
            amountState = value,
            isClickDisabled = false,
            isEditingDisabled = false,
            onClick = {},
        )
    }
}

private class AmountBlockV2PreviewProvider : PreviewParameterProvider<AmountState> {
    override val values: Sequence<AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.amountState,
        )
}
// endregion