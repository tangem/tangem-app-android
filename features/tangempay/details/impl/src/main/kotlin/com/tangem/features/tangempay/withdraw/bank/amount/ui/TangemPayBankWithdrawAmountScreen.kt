package com.tangem.features.tangempay.withdraw.bank.amount.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFromUM
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
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
        when (state) {
            is TangemPayBankWithdrawAmountUM.Content -> AmountContent(state = state)
            is TangemPayBankWithdrawAmountUM.Loading -> AmountLoading(state = state)
            is TangemPayBankWithdrawAmountUM.FeeUnreachable -> FeeUnreachable(state = state)
        }
    }
    BankWithdrawAmountSheets(sheet = (state as? TangemPayBankWithdrawAmountUM.Content)?.sheet ?: SheetUM.Hidden)
}

@Composable
private fun ColumnScope.AmountContent(state: TangemPayBankWithdrawAmountUM.Content) {
    val dismissKeyboardThen = rememberDismissKeyboardThen()
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
            from = state.from,
            amountSlot = { AmountInput(amount = state.amount, modifier = Modifier.weight(1f)) },
            sourceSlot = {
                SourceRow(
                    source = state.source,
                    onClick = state.source.onClick?.let { onSourceClick -> { dismissKeyboardThen(onSourceClick) } },
                )
            },
        )
        RouteRow(
            route = state.route,
            onClick = state.route.onClick?.let { onRouteClick -> { dismissKeyboardThen(onRouteClick) } },
        )
    }
    TangemFooter(
        windowInsets = WindowInsets(0),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_continue),
            isEnabled = state.isContinueEnabled,
            onClick = state.onContinueClick,
        )
    }
}

@Composable
private fun ColumnScope.AmountLoading(state: TangemPayBankWithdrawAmountUM.Loading) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AmountCard(
            from = state.from,
            amountSlot = {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AmountPlaceholder(currencySymbol = state.currencySymbol)
                }
            },
            sourceSlot = { SourceRowShimmer() },
        )
        RouteRowShimmer()
    }
}

@Composable
private fun ColumnScope.FeeUnreachable(state: TangemPayBankWithdrawAmountUM.FeeUnreachable) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        TangemButton(
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X10,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_refresh_20),
            contentDescription = stringResourceSafe(R.string.warning_button_refresh),
            onClick = state.onRetryClick,
        )
        Text(
            text = stringResourceSafe(R.string.tangempay_withdraw_bank_network_fee_unreachable),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun rememberDismissKeyboardThen(): (action: () -> Unit) -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboard = keyboardAsState()
    val scope = rememberCoroutineScope()
    val pendingAction = remember { mutableStateOf<Job?>(null) }
    return remember(focusManager, keyboardController, keyboard, scope) {
        { action ->
            if (pendingAction.value?.isActive != true) {
                keyboardController?.hide()
                pendingAction.value = scope.launch {
                    withTimeoutOrNull(KEYBOARD_HIDE_TIMEOUT_MILLIS) {
                        snapshotFlow { keyboard.value.isOpened }.first { isOpened -> !isOpened }
                    }
                    focusManager.clearFocus()
                    action()
                }
            }
        }
    }
}

private const val KEYBOARD_HIDE_TIMEOUT_MILLIS = 500L

@Composable
private fun AmountCard(
    from: BankWithdrawFromUM,
    amountSlot: @Composable ColumnScope.() -> Unit,
    sourceSlot: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(268.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BankFromRow(from = from, balance = null)
        amountSlot()
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = TangemTheme.colors3.border.secondary,
        )
        sourceSlot()
    }
}

@Composable
private fun SourceRowShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemShimmer(modifier = Modifier.size(40.dp), radius = 20.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TangemShimmer(style = TangemTheme.typography3.body.medium)
            TangemShimmer(style = TangemTheme.typography3.caption.medium)
        }
    }
}

@Composable
private fun RouteRowShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.secondary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TangemShimmer(style = TangemTheme.typography3.body.medium)
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
private fun SourceRow(source: SourceRowUM, onClick: (() -> Unit)?) {
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
                if (onClick != null) ChevronDownIcon()
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
        TangemPayBankWithdrawAmountPreviewData.moneyAccount,
        TangemPayBankWithdrawAmountPreviewData.loading,
        TangemPayBankWithdrawAmountPreviewData.feeUnreachable,
    )
}