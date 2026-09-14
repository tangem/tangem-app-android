package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSelection
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSheet
import com.tangem.features.tangempay.withdraw.bank.amount.model.BankWithdrawAmountValidator.Result
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.AmountFieldUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteItemUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.TokenItemUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.collections.immutable.toImmutableList
import java.util.Currency

internal class TangemPayBankWithdrawAmountUMFactory(
    private val step: TangemPayBankWithdrawInnerRoute,
    private val callbacks: Callbacks,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false)

    fun create(
        draft: BankWithdrawDraft,
        selection: AmountSelection,
        sheet: AmountSheet?,
    ): TangemPayBankWithdrawAmountUM {
        return TangemPayBankWithdrawAmountUM(
            header = BankWithdrawHeaderFactory.create(draft = draft, step = step),
            amount = createAmountField(selection = selection),
            source = createSourceRow(source = selection.source, currency = selection.currency),
            route = createRouteRow(selection = selection),
            isContinueEnabled = selection.isContinueEnabled,
            sheet = createSheet(sheet = sheet, selection = selection),
            onBackClick = callbacks.onBackClick,
            onContinueClick = callbacks.onContinueClick,
        )
    }

    private fun createAmountField(selection: AmountSelection): AmountFieldUM = AmountFieldUM(
        value = selection.displayValue,
        decimals = selection.currency.defaultFractionDigits,
        currencyCode = selection.currency.currencyCode,
        currencySymbol = selection.currency.symbol,
        error = createError(selection),
        onValueChange = callbacks.onValueChange,
    )

    private fun createError(selection: AmountSelection): TextReference? = when (selection.validation) {
        Result.Empty,
        Result.Valid,
        -> null
        Result.OutOfRange -> resourceReference(
            R.string.tangempay_withdraw_bank_amount_error_range,
            wrappedList(
                selection.limits.min.formatFiat(selection.currency),
                selection.limits.max.formatFiat(selection.currency),
            ),
        )
        Result.NotEnoughFunds -> resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds)
    }

    private fun createSourceRow(source: BankWithdrawSource, currency: Currency): SourceRowUM = SourceRowUM(
        icon = iconConverter.convert(source.token),
        symbol = source.token.symbol,
        subtitle = createSourceSubtitle(source = source, currency = currency),
        onMaxClick = callbacks.onMaxClick,
        onClick = callbacks.onSourceClick,
    )

    private fun createSourceSubtitle(source: BankWithdrawSource, currency: Currency): TextReference = resourceReference(
        R.string.tangempay_withdraw_bank_source_subtitle,
        wrappedList(source.network.name, source.fiatBalance.formatFiat(currency)),
    )

    private fun createRouteRow(selection: AmountSelection): RouteRowUM = RouteRowUM(
        rail = selection.destination.rail,
        title = selection.destination.rail.toTitleReference(),
        eta = selection.destination.eta.toTextReference(),
        fee = selection.destination.fee.toText(),
        onClick = callbacks.onRouteClick.takeIf { selection.destinations.size > 1 },
    )

    private fun createSheet(sheet: AmountSheet?, selection: AmountSelection): SheetUM = when (sheet) {
        null -> SheetUM.Hidden
        AmountSheet.TOKENS -> SheetUM.Tokens(
            items = selection.sources.map { source ->
                TokenItemUM(
                    id = source.id,
                    icon = iconConverter.convert(source.token),
                    symbol = source.token.symbol,
                    subtitle = createSourceSubtitle(source = source, currency = selection.currency),
                    isSelected = source.id == selection.source.id,
                    onClick = { callbacks.onSourceSelected(source.id) },
                )
            }.toImmutableList(),
            onDismiss = callbacks.onSheetDismiss,
        )
        AmountSheet.ROUTES -> SheetUM.Routes(
            items = selection.destinations.map { destination ->
                RouteItemUM(
                    rail = destination.rail,
                    title = destination.rail.toTitleReference(),
                    eta = destination.eta.toTextReference(),
                    fee = destination.fee.toText(),
                    isSelected = destination.rail == selection.destination.rail,
                    onClick = { callbacks.onRouteSelected(destination.rail) },
                )
            }.toImmutableList(),
            onDismiss = callbacks.onSheetDismiss,
        )
    }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onValueChange: (String) -> Unit,
        val onMaxClick: () -> Unit,
        val onSourceClick: () -> Unit,
        val onSourceSelected: (sourceId: String) -> Unit,
        val onRouteClick: () -> Unit,
        val onRouteSelected: (BankRail) -> Unit,
        val onSheetDismiss: () -> Unit,
        val onContinueClick: () -> Unit,
    )
}