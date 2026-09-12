package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSelection
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSheet
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountState
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
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toImmutableList
import java.util.Currency

internal class TangemPayBankWithdrawAmountUMFactory(
    private val step: TangemPayBankWithdrawInnerRoute,
    private val callbacks: Callbacks,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false)

    fun create(draft: BankWithdrawDraft, state: AmountState): TangemPayBankWithdrawAmountUM {
        val selection = state.resolve(draft)
        return TangemPayBankWithdrawAmountUM(
            header = BankWithdrawHeaderFactory.create(draft = draft, step = step),
            amount = createAmountField(value = selection?.displayValue ?: state.value.orEmpty(), selection = selection),
            source = selection?.source?.let { createSourceRow(it, selection.currency) } ?: SourceRowUM.Loading,
            route = selection?.let(::createRouteRow) ?: RouteRowUM.Loading,
            isContinueEnabled = selection?.isContinueEnabled == true,
            sheet = selection?.let { createSheet(sheet = state.sheet, selection = it) } ?: SheetUM.Hidden,
            onBackClick = callbacks.onBackClick,
            onContinueClick = callbacks.onContinueClick,
        )
    }

    private fun createAmountField(value: String, selection: AmountSelection?): AmountFieldUM {
        val currency = selection?.currency
        return AmountFieldUM(
            value = value,
            decimals = currency?.defaultFractionDigits ?: DEFAULT_DECIMALS,
            currencyCode = currency?.currencyCode.orEmpty(),
            currencySymbol = currency?.symbol.orEmpty(),
            error = selection?.let(::createError),
            onValueChange = callbacks.onValueChange,
        )
    }

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

    private fun createSourceRow(source: BankWithdrawSource, currency: Currency): SourceRowUM.Content =
        SourceRowUM.Content(
            icon = iconConverter.convert(source.token),
            symbol = source.token.symbol,
            subtitle = createSourceSubtitle(source = source, currency = currency),
            onMaxClick = callbacks.onMaxClick,
            onClick = callbacks.onSourceClick,
        )

    private fun createSourceSubtitle(source: BankWithdrawSource, currency: Currency): TextReference {
        val balance: Any = if (source.isAvailable) {
            source.fiatBalance.orZero().formatFiat(currency)
        } else {
            resourceReference(R.string.tangempay_withdraw_bank_offramp_unavailable)
        }
        return resourceReference(
            R.string.tangempay_withdraw_bank_source_subtitle,
            wrappedList(source.network.name, balance),
        )
    }

    private fun createRouteRow(selection: AmountSelection): RouteRowUM.Content = RouteRowUM.Content(
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
                    isAvailable = source.isAvailable,
                    isSelected = source.id == selection.source?.id,
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

    private companion object {
        const val DEFAULT_DECIMALS = 2
    }
}