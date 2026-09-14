package com.tangem.features.tangempay.withdraw.bank.amount.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteItemUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.TokenItemUM
import com.tangem.features.tangempay.withdraw.bank.ui.BankRailIcon

@Composable
internal fun BankWithdrawAmountSheets(sheet: SheetUM) {
    val tokens = rememberLastNonNull(sheet as? SheetUM.Tokens)
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        presentation = TangemModal.Presentation.Dialog,
        config = TangemBottomSheetConfig(
            isShown = sheet is SheetUM.Tokens,
            onDismissRequest = sheet.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = { SheetHeader(title = R.string.common_choose_network, onClose = sheet.onDismiss) },
    ) {
        tokens?.items?.fastForEach { item -> TokenRow(item = item) }
    }

    val routes = rememberLastNonNull(sheet as? SheetUM.Routes)
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        presentation = TangemModal.Presentation.Dialog,
        config = TangemBottomSheetConfig(
            isShown = sheet is SheetUM.Routes,
            onDismissRequest = sheet.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = { SheetHeader(title = R.string.tangempay_withdraw_bank_select_route, onClose = sheet.onDismiss) },
    ) {
        routes?.items?.fastForEach { item -> RouteRow(item = item) }
    }
}

@Composable
private fun SheetHeader(title: Int, onClose: () -> Unit) {
    TangemTopNavigation(
        title = resourceReference(title),
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        windowInsets = WindowInsets(0.dp),
        fadeEnabled = false,
        onClose = onClose,
    )
}

@Composable
private fun TokenRow(item: TokenItemUM) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { CurrencyIcon(state = item.icon, iconSize = 40.dp, networkBadgeSize = 12.dp) },
        titleSlot = { TangemRowText(text = item.symbol, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = item.subtitle.resolveReference(), role = TangemRowTextRole.Subtitle) },
        endSlot = if (item.isSelected) {
            { TangemCheckmark(checked = true, onCheckedChange = null) }
        } else {
            null
        },
        onClick = item.onClick,
    )
}

@Composable
private fun RouteRow(item: RouteItemUM) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { BankRailIcon(rail = item.rail, size = 40.dp) },
        titleSlot = { TangemRowText(text = item.title.resolveReference(), role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = item.eta.resolveReference(), role = TangemRowTextRole.Subtitle) },
        valueSlot = { TangemRowText(text = item.fee, role = TangemRowTextRole.Value) },
        endSlot = { TangemCheckmark(checked = item.isSelected, onCheckedChange = null) },
        onClick = item.onClick,
    )
}