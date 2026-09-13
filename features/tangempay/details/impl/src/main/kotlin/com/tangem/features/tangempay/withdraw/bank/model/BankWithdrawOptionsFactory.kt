package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.utils.extensions.isPositive
import kotlinx.collections.immutable.toImmutableList
import java.util.Currency

/** Turns the backend's off-ramp options and the payment account's networks into the wizard's [BankWithdrawOptions]. */
internal object BankWithdrawOptionsFactory {

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
        val sources = resolveSources(networks = networks, sources = options.sources).ifEmpty { return null }
        val currency = currencyOrNull(balanceCurrencyCode)
            ?: currencyOrNull(destinations.first().fee.currency)
            ?: return null
        return BankWithdrawOptions(
            sources = sources.toImmutableList(),
            destinations = destinations.toImmutableList(),
            limits = options.amountLimits,
            currency = currency,
        )
    }

    /** Payment-account tokens in backend order, keeping only what the off-ramp accepts and the user can actually send. */
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
                    val fiatBalance = status.value.fiatAmount ?: return@mapNotNull null
                    val isAccepted = network.chainId to token.contractAddress.lowercase() in accepted
                    if (!isAccepted || !fiatBalance.isPositive()) return@mapNotNull null
                    BankWithdrawSource(
                        network = network.network,
                        chainId = network.chainId,
                        token = token,
                        fiatBalance = fiatBalance,
                    )
                }
            }
    }

    private fun currencyOrNull(code: String?): Currency? =
        code?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
}