package com.tangem.features.tangempay.withdraw.bank.amount.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.TangemAmountTextFieldColors
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.ui.BankFromRow
import com.tangem.features.tangempay.withdraw.bank.ui.BankRouteRow
import com.tangem.features.tangempay.withdraw.bank.ui.ChevronDownIcon
import kotlinx.coroutines.delay

@Composable
internal fun TangemPayBankWithdrawAmountScreen(state: TangemPayBankWithdrawAmountUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .imePadding(),
    ) {
        TangemTopNavigation(
            title = state.header.title,
            subtitle = state.header.subtitle,
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onBack = state.onBackClick,
            onClose = state.header.onCloseClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AmountCard(amount = state.amount, source = state.source)
            RouteRow(route = state.route)
        }
        TangemFooter(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_continue),
                isEnabled = state.isContinueEnabled,
                onClick = state.onContinueClick,
            )
        }
    }
    BankWithdrawAmountSheets(sheet = state.sheet)
}

@Composable
private fun AmountCard(amount: TangemPayBankWithdrawAmountUM.AmountFieldUM, source: SourceRowUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(268.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BankFromRow(balance = null)
        AmountInput(amount = amount, modifier = Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = TangemTheme.colors3.border.secondary,
        )
        SourceRow(source = source)
    }
}

// The keyboard is requested once the screen's entrance transition has settled, as on the Send amount field.
private const val AMOUNT_FOCUS_DELAY_MILLIS = 200L

@Composable
private fun AmountInput(amount: TangemPayBankWithdrawAmountUM.AmountFieldUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(AMOUNT_FOCUS_DELAY_MILLIS)
        focusRequester.requestFocus()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (amount.value.isEmpty()) {
                AmountPlaceholder(currencySymbol = amount.currencySymbol)
            }
            val colors = TangemAmountTextFieldColors.copy(
                textColor = TangemTheme.colors3.text.primary,
                disabledTextColor = TangemTheme.colors3.text.tertiary,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            )
            AmountTextField(
                value = amount.value,
                decimals = amount.decimals,
                onValueChange = amount.onValueChange,
                textStyle = TangemTheme.typography3.display.medium.copy(textAlign = TextAlign.Center),
                colors = colors,
                visualTransformation = AmountVisualTransformation(
                    decimals = amount.decimals,
                    symbol = amount.currencySymbol.takeIf { amount.value.isNotEmpty() },
                    currencyCode = amount.currencyCode,
                    decimalFormat = rememberDecimalFormat(),
                    symbolColor = TangemTheme.colors3.text.tertiary,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isAutoResize = true,
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
        val error = amount.error
        if (error != null) {
            SpacerH(8.dp)
            Text(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                text = error.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AmountPlaceholder(currencySymbol: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currencySymbol,
            style = TangemTheme.typography3.display.medium,
            color = TangemTheme.colors3.text.tertiary,
        )
        Text(
            text = "0",
            style = TangemTheme.typography3.display.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun SourceRow(source: SourceRowUM) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            CurrencyIcon(state = source.icon, iconSize = 40.dp, networkBadgeSize = 12.dp)
        },
        titleSlot = { TangemRowText(text = source.symbol, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(text = source.subtitle.resolveReference(), role = TangemRowTextRole.Subtitle)
        },
        endSlot = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X8,
                    text = resourceReference(R.string.send_max_amount),
                    onClick = source.onMaxClick,
                )
                ChevronDownIcon()
            }
        },
        onClick = source.onClick,
    )
}

@Composable
private fun RouteRow(route: RouteRowUM) {
    BankRouteRow(
        rail = route.rail,
        title = route.title,
        eta = route.eta,
        fee = route.fee,
        onClick = route.onClick,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBankWithdrawAmountScreenPreview(
    @PreviewParameter(AmountPreviewProvider::class) state: TangemPayBankWithdrawAmountUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawAmountScreen(state = state)
    }
}

private class AmountPreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawAmountUM> {
    override val values: Sequence<TangemPayBankWithdrawAmountUM> = sequenceOf(
        TangemPayBankWithdrawAmountPreviewData.empty,
        TangemPayBankWithdrawAmountPreviewData.content,
        TangemPayBankWithdrawAmountPreviewData.contentWithError,
    )
}