package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.util.Currency

internal enum class AmountSheet { TOKENS, ROUTES }

/** Raw inputs of the Amount step; [resolve] turns them into the selection the UI and the Continue action work with. */
internal data class AmountState(
    val value: String? = null,
    val selectedSourceId: String? = null,
    val selectedRail: BankRail? = null,
    val sheet: AmountSheet? = null,
) {

    fun resolve(draft: BankWithdrawDraft, options: BankWithdrawOptions): AmountSelection {
        val preferredRail = draft.recipient?.details?.rail ?: selectedRail ?: draft.destination?.rail
        val destination = options.destinations.firstOrNull { it.rail == preferredRail } ?: options.destinations.first()
        val preferredSourceId = selectedSourceId ?: draft.source?.id
        val source = options.sources.firstOrNull { it.isAvailable && it.id == preferredSourceId }
            ?: options.sources.first(BankWithdrawSource::isAvailable)
        val displayValue = displayValue(draft = draft, currency = options.currency)
        val amount = displayValue.parseBigDecimalOrNull()
        return AmountSelection(
            currency = options.currency,
            minAmount = options.minAmount,
            maxAmount = options.maxAmount,
            sendableAmount = BankWithdrawAmountValidator.sendableAmount(
                maxAmount = options.maxAmount,
                balance = source.fiatBalance,
                fee = destination.fee.amount,
            ),
            sources = options.sources,
            destinations = options.destinations,
            source = source,
            destination = destination,
            displayValue = displayValue,
            amount = amount,
            validation = BankWithdrawAmountValidator.validate(
                amount = amount,
                minAmount = options.minAmount,
                maxAmount = options.maxAmount,
                balance = source.fiatBalance,
                fee = destination.fee.amount,
            ),
        )
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
    val maxAmount: BigDecimal,
    val sendableAmount: BigDecimal,
    val sources: ImmutableList<BankWithdrawSource>,
    val destinations: ImmutableList<OfframpDestination>,
    val source: BankWithdrawSource,
    val destination: OfframpDestination,
    val displayValue: String,
    val amount: BigDecimal?,
    val validation: BankWithdrawAmountValidator.Result,
) {

    val isContinueEnabled: Boolean
        get() = validation == BankWithdrawAmountValidator.Result.Valid

    fun toResult(): BankWithdrawAmountResult? {
        val amount = amount ?: return null
        return if (isContinueEnabled) BankWithdrawAmountResult(amount, source, destination) else null
    }
}