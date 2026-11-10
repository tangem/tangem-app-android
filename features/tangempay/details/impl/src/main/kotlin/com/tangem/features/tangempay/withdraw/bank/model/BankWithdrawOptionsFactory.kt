package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency

/** Turns the off-ramp's rails and the source account's tokens into the wizard's [BankWithdrawOptions]. */
internal object BankWithdrawOptionsFactory {

    private val DEFAULT_MIN_AMOUNT = BigDecimal("1.00")
    private val DEFAULT_MAX_AMOUNT = BigDecimal("10000.00")

    /**
     * Null when a bank withdrawal cannot start: the off-ramp offers no rail the account can take — a fixed-fee ACH or
     * wire rail, or SPEI where [isSpeiOffered] — the account holds none of the tokens it accepts, or neither the
     * account nor the rail names a currency the platform knows.
     */
    fun create(
        destinations: List<OfframpDestination>,
        sources: List<BankWithdrawSource>,
        balanceCurrencyCode: String?,
        isLimitedPerRail: Boolean,
        isSpeiOffered: Boolean,
    ): BankWithdrawOptions? {
        val offeredDestinations = destinations.filter { destination -> destination.isTakenBy(isSpeiOffered) }
        if (offeredDestinations.isEmpty() || sources.none(BankWithdrawSource::isAvailable)) return null
        val currency = currencyOrNull(balanceCurrencyCode)
            ?: currencyOrNull(offeredDestinations.first().fee.currency)
            ?: return null
        return BankWithdrawOptions(
            sources = sources.sortedByAvailabilityAndSymbol().toImmutableList(),
            destinations = offeredDestinations.toImmutableList(),
            currency = currency,
            defaultAmountLimits = AmountLimits(
                min = DEFAULT_MIN_AMOUNT,
                max = DEFAULT_MAX_AMOUNT,
                currency = currency.currencyCode,
            ),
            isLimitedPerRail = isLimitedPerRail,
        )
    }

    /**
     * Available tokens come before the rest, and each part is grouped by symbol: USDC first, then every other symbol
     * in the order it first appears among the sources, and inside a group that order.
     */
    private fun List<BankWithdrawSource>.sortedByAvailabilityAndSymbol(): List<BankWithdrawSource> {
        val symbolOrder = mutableMapOf<String, Int>()
        forEach { source -> symbolOrder.getOrPut(source.token.symbol) { symbolOrder.size } }
        return sortedWith(
            compareBy(
                { source -> !source.isAvailable },
                { source -> !source.token.symbol.equals(TangemPayCurrencyFactory.TOKEN_NAME, ignoreCase = true) },
                { source -> symbolOrder.getValue(source.token.symbol) },
            ),
        )
    }

    private fun OfframpDestination.isTakenBy(isSpeiOffered: Boolean): Boolean = when (rail) {
        BankRail.ACH, BankRail.WIRE -> fee.calculationType == FeeCalculationType.FIXED
        BankRail.SPEI -> isSpeiOffered
    }

    private fun currencyOrNull(code: String?): Currency? =
        code?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
}