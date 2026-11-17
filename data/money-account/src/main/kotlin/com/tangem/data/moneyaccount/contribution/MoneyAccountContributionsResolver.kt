package com.tangem.data.moneyaccount.contribution

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.currency.balance.resolversFlow
import com.tangem.domain.models.currency.balance.withSpendableContributions
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.activeYieldSupplySource
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

/** Resolves the Money account contributions from their own stores — the reading half, used by the status producer. */
internal class MoneyAccountContributionsResolver @Inject constructor(
    @MoneyAccountContributions
    private val contributionProviders: List<@JvmSuppressWildcards BalanceContributionProvider>,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val policy: MoneyAccountContributionPolicy,
) {

    /**
     * [value] with the contributions of its balance resolved, and their spendable part folded into the amount.
     * Re-emits whenever a contribution or the account network status changes.
     */
    fun withContributions(userWalletId: UserWalletId, value: MoneyAccountStatusValue): Flow<MoneyAccountStatusValue> {
        val account = value as? MoneyAccountStatusValue.Account
        val balance = account?.balance
        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId)

        if (!policy.isEnabled || balance == null || userWallet == null) return flowOf(value)

        val currency = account.cryptoCurrency

        return combine(
            contributionProviders.resolversFlow(userWallet = userWallet),
            networkStatusFlow(userWalletId = userWalletId, network = currency.network),
        ) { resolvers, networkStatus ->
            val contributions = resolvers.mapNotNull { resolver ->
                resolver.resolve(currency = currency, networkStatus = networkStatus)
            }

            // A contribution's delta is relative to the network amount it was resolved against, and the network status
            // is stored before the account during a refresh (or alone, when the refresh then fails)
            val networkAmount = networkStatus.loadedAmountOf(currency)

            val enriched = account.copySealed(
                balance = balance.copy(
                    cryptoAmount = (networkAmount ?: balance.cryptoAmount).withSpendableContributions(contributions),
                    contributions = contributions,
                ),
            )

            // The same freshness a portfolio token's spendable amount has: its network status and its active yield
            val source = listOfNotNull(
                account.source,
                networkStatus?.value?.source?.takeIf { networkAmount != null },
                enriched.cryptoCurrencyStatus?.value?.activeYieldSupplySource,
            ).getResultStatusSource()

            enriched.copySealed(source = source, error = account.error)
        }
    }

    private fun NetworkStatus?.loadedAmountOf(currency: CryptoCurrency): BigDecimal? {
        val verified = this?.value as? NetworkStatus.Verified ?: return null
        return (verified.amounts[currency.id] as? NetworkStatus.Amount.Loaded)?.value
    }

    private fun networkStatusFlow(userWalletId: UserWalletId, network: Network): Flow<NetworkStatus?> {
        return multiNetworkStatusSupplier(MultiNetworkStatusProducer.Params(userWalletId = userWalletId))
            .map { statuses -> statuses.firstOrNull { status -> status.network.id == network.id } }
            .distinctUntilChanged()
    }
}