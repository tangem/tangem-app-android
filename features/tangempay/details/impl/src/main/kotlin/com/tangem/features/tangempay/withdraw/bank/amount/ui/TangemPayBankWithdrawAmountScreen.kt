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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.TangemAmountTextFieldColors
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.components.isOpened
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.ui.BankFromRow
import com.tangem.features.tangempay.withdraw.bank.ui.BankRouteRow
import com.tangem.features.tangempay.withdraw.bank.ui.ChevronDownIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun TangemPayBankWithdrawAmountScreen(state: TangemPayBankWithdrawAmountUM, modifier: Modifier = Modifier) {
    val dismissKeyboardThen = rememberDismissKeyboardThen()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .imePadding()
            .testTag(TangemPayTestTags.BANK_WITHDRAW_AMOUNT_SCREEN),
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
            AmountCard(
                amount = state.amount,
                source = state.source,
                onSourceClick = { dismissKeyboardThen(state.source.onClick) },
            )
            RouteRow(
                route = state.route,
                onClick = state.route.onClick?.let { onRouteClick -> { dismissKeyboardThen(onRouteClick) } },
            )
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
private fun rememberDismissKeyboardThen(): (action: () -> Unit) -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = keyboardAsState()
    val scope = rememberCoroutineScope()
    val pendingAction = remember { mutableStateOf<Job?>(null) }
    return remember(focusManager, keyboard, scope) {
        { action ->
            if (pendingAction.value?.isActive != true) {
                focusManager.clearFocus()
                pendingAction.value = scope.launch {
                    withTimeoutOrNull(KEYBOARD_HIDE_TIMEOUT_MILLIS) {
                        snapshotFlow { keyboard.value.isOpened }.first { isOpened -> !isOpened }
                    }
                    action()
                }
            }
        }
    }
}

private const val KEYBOARD_HIDE_TIMEOUT_MILLIS = 500L

@Composable
private fun AmountCard(
    amount: TangemPayBankWithdrawAmountUM.AmountFieldUM,
    source: SourceRowUM,
    onSourceClick: () -> Unit,
) {
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
        SourceRow(source = source, onClick = onSourceClick)
    }
}

// The keyboard is requested once the screen's entrance transition has settled, as on the Send amount field.
private const val AMOUNT_FOCUS_DELAY_MILLIS = 200L

@Composable
private fun AmountInput(amount: TangemPayBankWithdrawAmountUM.AmountFieldUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AmountField(amount = amount)
        val error = amount.error
        if (error != null) {
            AmountError(error = error, modifier = Modifier.withoutLayoutHeight())
        }
    }
}

@Composable
private fun AmountField(amount: TangemPayBankWithdrawAmountUM.AmountFieldUM) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(AMOUNT_FOCUS_DELAY_MILLIS)
        focusRequester.requestFocus()
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (amount.value.isEmpty()) {
            AmountPlaceholder(currencySymbol = amount.currencySymbol)
        }
        val colors = TangemAmountTextFieldColors.copy(
            textColor = TangemTheme.colors3.text.primary,
            disabledTextColor = TangemTheme.colors3.text.tertiary,
            backgroundColor = Color.Transparent,
        )
        val decimalFormat = rememberDecimalFormat()
        AmountTextField(
            value = amount.value,
            decimals = amount.decimals,
            onValueChange = amount.onValueChange,
            textStyle = TangemTheme.typography3.display.medium.copy(textAlign = TextAlign.Center),
            colors = colors,
            visualTransformation = if (amount.value.isEmpty()) {
                VisualTransformation.None
            } else {
                AmountVisualTransformation(
                    decimals = amount.decimals,
                    symbol = amount.currencySymbol,
                    currencyCode = amount.currencyCode,
                    decimalFormat = decimalFormat,
                    symbolColor = TangemTheme.colors3.text.tertiary,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isAutoResize = true,
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}

@Composable
private fun AmountError(error: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(top = 8.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        text = error.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.status.error,
        textAlign = TextAlign.Center,
    )
}

private fun Modifier.withoutLayoutHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(width = placeable.width, height = 0) { placeable.place(x = 0, y = 0) }
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
private fun SourceRow(source: SourceRowUM, onClick: () -> Unit) {
    TangemRow(
        modifier = Modifier.testTag(TangemPayTestTags.BANK_WITHDRAW_SOURCE_ROW),
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
        onClick = onClick,
    )
}

@Composable
private fun RouteRow(route: RouteRowUM, onClick: (() -> Unit)?) {
    BankRouteRow(
        rail = route.rail,
        title = route.title,
        eta = route.eta,
        fee = route.fee,
        onClick = onClick,
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