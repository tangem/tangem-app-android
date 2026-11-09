package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency

/** Turns the off-ramp's rails and the source account's tokens into the wizard's [BankWithdrawOptions]. */
internal object BankWithdrawOptionsFactory {

    /** The off-ramp options carry no bounds, so the withdrawal range agreed with the backend lives here. */
    private val MIN_AMOUNT = BigDecimal("1.00")
    private val MAX_AMOUNT = BigDecimal("10000.00")

    private val ROUTE_RAILS = setOf(BankRail.ACH, BankRail.WIRE)

    /**
     * Null when a bank withdrawal cannot start: the off-ramp offers no fixed-fee ACH or wire rail, the account holds
     * none of the tokens it accepts, or neither the account nor the rail names a currency the platform knows.
     */
    fun create(
        destinations: List<OfframpDestination>,
        sources: List<BankWithdrawSource>,
        balanceCurrencyCode: String?,
    ): BankWithdrawOptions? {
        val routeDestinations = destinations.filter(::isFixedFeeRouteRail)
        if (routeDestinations.isEmpty() || sources.none(BankWithdrawSource::isAvailable)) return null
        val currency = currencyOrNull(balanceCurrencyCode)
            ?: currencyOrNull(routeDestinations.first().fee.currency)
            ?: return null
        return BankWithdrawOptions(
            sources = sources.sortedByAvailabilityAndSymbol().toImmutableList(),
            destinations = routeDestinations.toImmutableList(),
            minAmount = MIN_AMOUNT,
            maxAmount = MAX_AMOUNT,
            currency = currency,
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

    private fun isFixedFeeRouteRail(destination: OfframpDestination): Boolean =
        destination.rail in ROUTE_RAILS && destination.fee.calculationType == AccountFee.CalculationType.FIXED

    private fun currencyOrNull(code: String?): Currency? =
        code?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
}