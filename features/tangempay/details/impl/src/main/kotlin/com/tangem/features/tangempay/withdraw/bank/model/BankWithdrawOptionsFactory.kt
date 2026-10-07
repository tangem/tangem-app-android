package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.utils.extensions.isPositive
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency

/** Turns the backend's off-ramp options and the payment account's networks into the wizard's [BankWithdrawOptions]. */
internal object BankWithdrawOptionsFactory {

    /** The off-ramp options carry no bounds, so the withdrawal range agreed with the backend lives here. */
    private val MIN_AMOUNT = BigDecimal("1.00")
    private val MAX_AMOUNT = BigDecimal("10000.00")

    /**
     * Null when a bank withdrawal cannot start: the off-ramp offers no rail, the account holds none of the tokens it
     * accepts, or neither the account nor the rail names a currency the platform knows.
     */
    fun create(
        options: BankOfframpOptions,
        networks: List<PaymentNetworkStatus>,
        balanceCurrencyCode: String?,
    ): BankWithdrawOptions? {
        val destinations = options.destinations.ifEmpty { return null }
        val sources = resolveSources(networks = networks, sources = options.sources)
            .takeIf { resolved -> resolved.any(BankWithdrawSource::isAvailable) }
            ?: return null
        val currency = currencyOrNull(balanceCurrencyCode)
            ?: currencyOrNull(destinations.first().fee.currency)
            ?: return null
        return BankWithdrawOptions(
            sources = sources.toImmutableList(),
            destinations = destinations.toImmutableList(),
            minAmount = MIN_AMOUNT,
            maxAmount = MAX_AMOUNT,
            currency = currency,
        )
    }

    /**
     * Payment-account tokens holding a positive balance, available when the off-ramp accepts them, grouped by symbol:
     * USDC first, then every other symbol in the order it first appears among the account's networks, and inside a
     * group that order.
     */
    private fun resolveSources(
        networks: List<PaymentNetworkStatus>,
        sources: List<OfframpSource>,
    ): List<BankWithdrawSource> {
        val accepted = sources
            .flatMap { source -> source.tokens.map { token -> source.chainId to token.contractAddress.lowercase() } }
            .toSet()
        return networks
            .filterIsInstance<PaymentNetworkStatus.Available>()
            .flatMap { network ->
                network.cryptoCurrencyStatuses.mapNotNull { status ->
                    val token = status.currency as? CryptoCurrency.Token ?: return@mapNotNull null
                    val fiatBalance = status.value.fiatAmount?.takeIf { it.isPositive() } ?: return@mapNotNull null
                    BankWithdrawSource(
                        network = network.network,
                        chainId = network.chainId,
                        token = token,
                        fiatBalance = fiatBalance,
                        isAvailable = network.chainId to token.contractAddress.lowercase() in accepted,
                    )
                }
            }
            .sortedBySymbol()
    }

    private fun List<BankWithdrawSource>.sortedBySymbol(): List<BankWithdrawSource> {
        val symbolOrder = mutableMapOf<String, Int>()
        forEach { source -> symbolOrder.getOrPut(source.token.symbol) { symbolOrder.size } }
        return sortedWith(
            compareBy(
                { source -> !source.token.symbol.equals(TangemPayCurrencyFactory.TOKEN_NAME, ignoreCase = true) },
                { source -> symbolOrder.getValue(source.token.symbol) },
            ),
        )
    }

    private fun currencyOrNull(code: String?): Currency? =
        code?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
}