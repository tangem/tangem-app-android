package com.tangem.domain.account.status.utils

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.BalanceFetchingOperations
import com.tangem.domain.tokens.FetchErrorFormatter
import com.tangem.domain.tokens.FetchingSource
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.utils.logging.TangemLogger

/**
 * Refreshes the registered balance contributions of every portfolio currency on [Network] after its network status
 * was refreshed on its own, e.g. after a send.
 *
 * A send can change a contribution, not only the network balance: a yield-active token withdraws the shortfall from
 * its Yield Module.
 */
class NetworkContributionsRefresher(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val balanceFetchingOperations: BalanceFetchingOperations,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network) {
        // Without the toggle CONTRIBUTIONS is the legacy staking fetch, which this refresh never triggered
        if (!tokensFeatureToggles.isPluggableBalanceSourcesEnabled) return

        val currencies = singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            ?.flattenCurrencies()
            ?.map { it.currency }
            ?.filter { it.network.id == network.id }

        if (currencies.isNullOrEmpty()) return

        val errors = balanceFetchingOperations.fetchAll(
            userWalletId = userWalletId,
            currencies = currencies,
            sources = setOf(FetchingSource.CONTRIBUTIONS),
        )

        if (errors.isNotEmpty()) {
            TangemLogger.e(FetchErrorFormatter.format(userWalletId, errors))
        }
    }
}