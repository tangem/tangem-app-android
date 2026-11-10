package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.amount.state.totalFee
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency

internal enum class AmountSheet { TOKENS, ROUTES }

internal sealed interface NetworkFeeState {

    data object Loading : NetworkFeeState

    data object Unreachable : NetworkFeeState

    data object NotEnoughFunds : NetworkFeeState

    data class Loaded(val fee: BigDecimal) : NetworkFeeState
}

/** Raw inputs of the Amount step; [resolve] turns them into the selection the UI and the Continue action work with. */
internal data class AmountState(
    val value: String? = null,
    val selectedSourceId: String? = null,
    val selectedRail: BankRail? = null,
    val sheet: AmountSheet? = null,
    val networkFee: NetworkFeeState = NetworkFeeState.Loading,
) {

    fun resolve(draft: BankWithdrawDraft, options: BankWithdrawOptions): AmountSelection {
        val destinations = draft.recipient?.let { options.destinationsFor(it.details.rail) } ?: options.destinations
        val preferredRail = draft.recipient?.details?.rail ?: selectedRail ?: draft.destination?.rail
        val destination = destinations.firstOrNull { it.rail == preferredRail } ?: destinations.first()
        val preferredSourceId = selectedSourceId ?: draft.source?.id
        val source = options.sources.firstOrNull { it.isAvailable && it.id == preferredSourceId }
            ?: options.sources.first(BankWithdrawSource::isAvailable)
        val displayValue = displayValue(draft = draft, currency = options.currency)
        val amount = displayValue.parseBigDecimalOrNull()
        val networkFee = (networkFee as? NetworkFeeState.Loaded)?.fee.orZero()
        val fees = destination.totalFee(networkFee)
        val limits = options.amountLimitsFor(destination)
        return AmountSelection(
            currency = options.currency,
            minAmount = limits.min,
            maxAmount = limits.max,
            maxInputAmount = BankWithdrawAmountValidator.maxInputAmount(
                maxAmount = limits.max,
                balance = source.fiatBalance,
                fee = fees,
            ),
            sources = options.sources,
            destinations = destinations.toImmutableList(),
            source = source,
            destination = destination,
            networkFee = networkFee,
            isNetworkFeeQuoted = this.networkFee is NetworkFeeState.Loaded,
            displayValue = displayValue,
            amount = amount,
            validation = BankWithdrawAmountValidator.validate(
                amount = amount,
                minAmount = limits.min,
                maxAmount = limits.max,
                balance = source.fiatBalance,
                fee = fees,
            ).withNetworkFeeCoverage(),
        )
    }

    private fun BankWithdrawAmountValidator.Result.withNetworkFeeCoverage(): BankWithdrawAmountValidator.Result =
        if (this == BankWithdrawAmountValidator.Result.Valid && networkFee == NetworkFeeState.NotEnoughFunds) {
            BankWithdrawAmountValidator.Result.NotEnoughFunds
        } else {
            this
        }

    /**
     * The user's own typing on this screen — even once cleared back to blank — or, before they have typed
     * anything at all, the draft's amount.
     */
    private fun displayValue(draft: BankWithdrawDraft, currency: Currency): String =
        value ?: draft.amount?.parseBigDecimal(decimals = currency.defaultFractionDigits).orEmpty()
}

/** The prepared options narrowed down to what this screen shows and what Continue would submit. */
internal data class AmountSelection(
    val currency: Currency,
    val minAmount: BigDecimal,
    val maxAmount: BigDecimal?,
    val maxInputAmount: BigDecimal,
    val sources: ImmutableList<BankWithdrawSource>,
    val destinations: ImmutableList<OfframpDestination>,
    val source: BankWithdrawSource,
    val destination: OfframpDestination,
    val networkFee: BigDecimal,
    val isNetworkFeeQuoted: Boolean,
    val displayValue: String,
    val amount: BigDecimal?,
    val validation: BankWithdrawAmountValidator.Result,
) {

    val isContinueEnabled: Boolean
        get() = validation == BankWithdrawAmountValidator.Result.Valid

    fun toResult(): BankWithdrawAmountResult? {
        val amount = amount ?: return null
        if (!isContinueEnabled) return null
        return BankWithdrawAmountResult(
            amount = amount,
            source = source,
            destination = destination,
            networkFee = networkFee,
        )
    }
}