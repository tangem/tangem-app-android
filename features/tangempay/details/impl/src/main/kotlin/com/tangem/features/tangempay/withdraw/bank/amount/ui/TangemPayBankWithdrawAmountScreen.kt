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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.PaymentAccountIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.TangemAmountTextFieldColors
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_20
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM

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
        FromRow()
        AmountInput(amount = amount, modifier = Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = TangemTheme.colors3.border.secondary,
        )
        when (source) {
            SourceRowUM.Loading -> SourceRowShimmer()
            is SourceRowUM.Content -> SourceRow(source = source)
        }
    }
}

@Composable
private fun FromRow() {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_from),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        PaymentAccountIcon(size = AccountIconSize.ExtraSmall)
        Text(
            text = stringResourceSafe(R.string.tangempay_payment_account),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun AmountInput(amount: TangemPayBankWithdrawAmountUM.AmountFieldUM, modifier: Modifier = Modifier) {
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
private fun SourceRow(source: SourceRowUM.Content) {
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
                ChevronDown()
            }
        },
        onClick = source.onClick,
    )
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TangemShimmer(modifier = Modifier.size(width = 120.dp, height = 16.dp), radius = 8.dp)
            TangemShimmer(modifier = Modifier.size(width = 72.dp, height = 12.dp), radius = 6.dp)
        }
    }
}

@Composable
private fun RouteRow(route: RouteRowUM) {
    val shape = CircleShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TangemTheme.colors3.bg.secondary)
            .then(
                if (route is RouteRowUM.Content && route.onClick != null) {
                    Modifier.clickableSingle(role = Role.Button, onClick = route.onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (route) {
            RouteRowUM.Loading -> {
                TangemShimmer(modifier = Modifier.size(20.dp), radius = 10.dp)
                TangemShimmer(modifier = Modifier.size(width = 96.dp, height = 16.dp), radius = 8.dp)
            }
            is RouteRowUM.Content -> {
                BankRailIcon(rail = route.rail, size = 20.dp)
                Text(
                    modifier = Modifier.weight(1f),
                    text = route.title.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                TangemBadge(text = route.eta, size = TangemBadge.Size.X6)
                Text(
                    text = route.fee,
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                if (route.onClick != null) {
                    ChevronDown()
                }
            }
        }
    }
}

@Composable
private fun ChevronDown() {
    TangemIcon(
        modifier = Modifier.size(20.dp),
        tangemIconUM = TangemIconUM.Icon(
            imageVector = Icons.ic_chevron_down_20,
            tintReference = { TangemTheme.colors3.icon.secondary },
        ),
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
        TangemPayBankWithdrawAmountPreviewData.loading,
        TangemPayBankWithdrawAmountPreviewData.content,
        TangemPayBankWithdrawAmountPreviewData.contentWithError,
    )
}