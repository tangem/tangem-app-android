package com.tangem.features.tangempay.withdraw.bank.amount.ui

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.AmountFieldUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM

internal object TangemPayBankWithdrawAmountPreviewData {

    private val header = BankWithdrawHeaderUM(
        title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
        subtitle = stringReference("1 of 5"),
        onCloseClick = {},
    )

    private val amount = AmountFieldUM(
        value = "500",
        decimals = 2,
        currencyCode = "USD",
        currencySymbol = "$",
        error = null,
        onValueChange = {},
    )

    private val source = SourceRowUM(
        icon = CurrencyIconState.Loading,
        symbol = "USDC",
        subtitle = stringReference("Polygon · $2,325.55"),
        onMaxClick = {},
        onClick = {},
    )

    private val route = RouteRowUM(
        rail = BankRail.ACH,
        title = resourceReference(R.string.tangempay_bank_transfer_fee_ach),
        eta = stringReference("1–3 business days"),
        fee = "$1",
        onClick = {},
    )

    val empty = TangemPayBankWithdrawAmountUM(
        header = header,
        amount = amount.copy(value = ""),
        source = source,
        route = route,
        isContinueEnabled = false,
        sheet = SheetUM.Hidden,
        onBackClick = {},
        onContinueClick = {},
    )

    val content = TangemPayBankWithdrawAmountUM(
        header = header,
        amount = amount,
        source = source,
        route = route,
        isContinueEnabled = true,
        sheet = SheetUM.Hidden,
        onBackClick = {},
        onContinueClick = {},
    )

    val contentWithError = content.copy(
        amount = amount.copy(
            value = "0.5",
            error = stringReference("Please set amount from $1 to $10,000"),
        ),
        isContinueEnabled = false,
    )
}