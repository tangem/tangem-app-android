package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.orZero

/** Payment-account tokens in backend order with off-ramp-capable ones first; the first capable token is the default. */
internal object BankWithdrawSourceResolver {

    fun resolve(networks: List<PaymentNetworkStatus>, sources: List<OfframpSource>): List<BankWithdrawSource> {
        val accepted = sources
            .flatMap { source -> source.tokens.map { token -> source.chainId to token.contractAddress.lowercase() } }
            .toSet()
        return networks
            .filterIsInstance<PaymentNetworkStatus.Available>()
            .flatMap { network ->
                network.cryptoCurrencyStatuses.mapNotNull { status ->
                    val token = status.currency as? CryptoCurrency.Token ?: return@mapNotNull null
                    val fiatBalance = status.value.fiatAmount
                    val isAccepted = network.chainId to token.contractAddress.lowercase() in accepted
                    BankWithdrawSource(
                        network = network.network,
                        chainId = network.chainId,
                        token = token,
                        fiatBalance = fiatBalance,
                        isAvailable = isAccepted && fiatBalance.orZero().isPositive(),
                    )
                }
            }
            .sortedByDescending { it.isAvailable }
    }

    fun default(sources: List<BankWithdrawSource>): BankWithdrawSource? =
        sources.firstOrNull { it.isAvailable } ?: sources.firstOrNull()
}