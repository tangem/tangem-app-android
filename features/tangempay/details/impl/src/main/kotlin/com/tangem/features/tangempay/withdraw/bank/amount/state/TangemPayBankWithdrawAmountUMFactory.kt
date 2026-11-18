package com.tangem.features.tangempay.withdraw.bank.amount.state

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSelection
import com.tangem.features.tangempay.withdraw.bank.amount.model.AmountSheet
import com.tangem.features.tangempay.withdraw.bank.amount.model.BankWithdrawAmountValidator.Result
import com.tangem.features.tangempay.withdraw.bank.amount.model.NetworkFeeState
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.AmountFieldUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteItemUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.RouteRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SheetUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.SourceRowUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM.TokenItemUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFromFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.CustomerNameState
import com.tangem.features.tangempay.withdraw.bank.model.isRailLocked
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.toImmutableList
import java.util.Currency

internal class TangemPayBankWithdrawAmountUMFactory(
    private val step: TangemPayBankWithdrawInnerRoute,
    private val sourceAccount: BankWithdrawSourceAccount,
    private val callbacks: Callbacks,
    @RemoveWithToggle(toggleName = "TWI_1778_MONEY_ACCOUNT_MXN_ENABLED") private val isMxnEnabled: Boolean,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false)

    private val from = BankWithdrawFromFactory.create(sourceAccount)

    private val isTokenSelectable = when (sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> true
        is BankWithdrawSourceAccount.Money -> false
    }

    private val isNetworkFeeCharged = when (sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> false
        is BankWithdrawSourceAccount.Money -> true
    }

    @Suppress("LongParameterList")
    fun create(
        draft: BankWithdrawDraft,
        selection: AmountSelection,
        sheet: AmountSheet?,
        networkFee: NetworkFeeState,
        customerName: CustomerNameState,
        isBalanceHidden: Boolean,
    ): TangemPayBankWithdrawAmountUM {
        val header = BankWithdrawHeaderFactory.create(draft = draft, step = step, onCloseClick = callbacks.onCloseClick)
        // Only a SPEI payout carries the customer's name, so the other rails never wait for it.
        val railCustomerName = customerName.takeIf { selection.destination.rail == BankRail.SPEI }
        if (railCustomerName == CustomerNameState.Unreachable) {
            return unreachable(header = header, messageRes = R.string.tangempay_withdraw_bank_data_unreachable)
        }
        if (railCustomerName == CustomerNameState.Loading && networkFee != NetworkFeeState.Unreachable) {
            return loading(header = header, selection = selection)
        }
        return when (networkFee) {
            NetworkFeeState.Loading -> loading(header = header, selection = selection)
            NetworkFeeState.Unreachable ->
                unreachable(header = header, messageRes = R.string.tangempay_withdraw_bank_network_fee_unreachable)
            NetworkFeeState.NotEnoughFunds,
            is NetworkFeeState.Loaded,
            -> TangemPayBankWithdrawAmountUM.Content(
                header = header,
                from = from,
                amount = createAmountField(selection = selection),
                source = createSourceRow(
                    source = selection.source,
                    currency = selection.currency,
                    isBalanceHidden = isBalanceHidden,
                ),
                route = createRouteRow(selection = selection, isRailLocked = draft.isRailLocked),
                isContinueEnabled = selection.isContinueEnabled,
                sheet = createSheet(sheet = sheet, selection = selection, isBalanceHidden = isBalanceHidden),
                onBackClick = callbacks.onBackClick,
                onContinueClick = callbacks.onContinueClick,
            )
        }
    }

    private fun loading(header: BankWithdrawHeaderUM, selection: AmountSelection) =
        TangemPayBankWithdrawAmountUM.Loading(
            header = header,
            from = from,
            currencySymbol = selection.currency.symbol,
            onBackClick = callbacks.onBackClick,
        )

    private fun unreachable(header: BankWithdrawHeaderUM, @StringRes messageRes: Int) =
        TangemPayBankWithdrawAmountUM.Unreachable(
            header = header,
            message = resourceReference(messageRes),
            onRetryClick = callbacks.onRetryClick,
            onBackClick = callbacks.onBackClick,
        )

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
        Result.OutOfRange -> createRangeError(selection)
        Result.NotEnoughFunds -> resourceReference(R.string.tangempay_withdraw_bank_amount_not_enough_funds)
    }

    private fun createRangeError(selection: AmountSelection): TextReference {
        val minAmount = selection.minAmount.formatFiat(selection.currency)
        val maxAmount = selection.maxAmount ?: return resourceReference(
            R.string.tangempay_withdraw_bank_amount_error_min,
            wrappedList(minAmount),
        )
        return resourceReference(
            R.string.tangempay_withdraw_bank_amount_error_range,
            wrappedList(minAmount, maxAmount.formatFiat(selection.currency)),
        )
    }

    private fun createSourceRow(
        source: BankWithdrawSource,
        currency: Currency,
        isBalanceHidden: Boolean,
    ): SourceRowUM = SourceRowUM(
        icon = iconConverter.convert(source.token),
        symbol = source.token.symbol,
        subtitle = createSourceSubtitle(source = source, currency = currency, isBalanceHidden = isBalanceHidden),
        onMaxClick = callbacks.onMaxClick,
        onClick = callbacks.onSourceClick.takeIf { isTokenSelectable },
    )

    private fun createSourceSubtitle(
        source: BankWithdrawSource,
        currency: Currency,
        isBalanceHidden: Boolean,
    ): TextReference {
        val trailing: Any = if (source.isAvailable) {
            source.fiatBalance.formatFiat(currency).orMaskWithStars(isBalanceHidden)
        } else {
            resourceReference(R.string.tangempay_withdraw_bank_offramp_unavailable)
        }
        return resourceReference(
            R.string.tangempay_withdraw_bank_source_subtitle,
            wrappedList(source.network.name, trailing),
        )
    }

    private fun createRouteRow(selection: AmountSelection, isRailLocked: Boolean): RouteRowUM = RouteRowUM(
        rail = selection.destination.rail,
        title = selection.destination.toTitleReference(),
        eta = selection.destination.toEtaReference(),
        fee = selection.destination.feeText(amount = selection.amount, networkFee = selection.networkFee),
        onClick = callbacks.onRouteClick.takeIf { selection.destinations.size > 1 && !isRailLocked },
    )

    private fun createSheet(sheet: AmountSheet?, selection: AmountSelection, isBalanceHidden: Boolean): SheetUM =
        when (sheet) {
            null -> SheetUM.Hidden
            AmountSheet.TOKENS -> SheetUM.Tokens(
                items = selection.sources.map { source ->
                    TokenItemUM(
                        id = source.id,
                        icon = iconConverter.convert(source.token),
                        symbol = source.token.symbol,
                        subtitle = createSourceSubtitle(
                            source = source,
                            currency = selection.currency,
                            isBalanceHidden = isBalanceHidden,
                        ),
                        isAvailable = source.isAvailable,
                        isSelected = source.id == selection.source.id,
                        onClick = { callbacks.onSourceSelected(source.id) },
                    )
                }.toImmutableList(),
                onDismiss = callbacks.onSheetDismiss,
            )
            AmountSheet.ROUTES -> SheetUM.Routes(
                title = createRoutesTitle(),
                items = selection.destinations.map { destination ->
                    RouteItemUM(
                        rail = destination.rail,
                        title = destination.toTitleReference(),
                        eta = destination.toEtaReference(),
                        fee = destination.totalFeeText(selection.networkFee),
                        isSelected = destination.rail == selection.destination.rail,
                        onClick = { callbacks.onRouteSelected(destination.rail) },
                    )
                }.toImmutableList(),
                networkFeeNote = createNetworkFeeNote(selection),
                onDismiss = callbacks.onSheetDismiss,
            )
        }

    @RemoveWithToggle(toggleName = "TWI_1778_MONEY_ACCOUNT_MXN_ENABLED")
    private fun createRoutesTitle(): TextReference = if (isMxnEnabled) {
        resourceReference(R.string.tangempay_withdraw_bank_choose_transfer_method)
    } else {
        resourceReference(R.string.tangempay_withdraw_bank_select_route)
    }

    private fun createNetworkFeeNote(selection: AmountSelection): TextReference? =
        if (isNetworkFeeCharged && selection.isNetworkFeeQuoted) {
            resourceReference(
                R.string.money_account_send_network_fee_note,
                wrappedList(selection.networkFee.formatFiat(selection.currency)),
            )
        } else {
            null
        }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onValueChange: (String) -> Unit,
        val onMaxClick: () -> Unit,
        val onSourceClick: () -> Unit,
        val onSourceSelected: (sourceId: String) -> Unit,
        val onRouteClick: () -> Unit,
        val onRouteSelected: (BankRail) -> Unit,
        val onSheetDismiss: () -> Unit,
        val onContinueClick: () -> Unit,
        val onRetryClick: () -> Unit,
    )
}