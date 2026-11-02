package com.tangem.domain.staking.contribution

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.utils.logging.TangemLogger

/**
 * Refreshes staking balances of the given currencies — the fetching counterpart of [StakingContributionProvider].
 *
 * Logs errors for currencies where staking ID cannot be obtained.
 */
class StakingContributionFetcher(
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
) : BalanceContributionFetcher {

    override suspend fun fetch(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        // A joint account is a Safe contract: it has no staking address of its own, and asking the wallet manager
        // for one would try to build a manager for a network that has no key
        val stakableCurrencies = currencies.filterNot { it.isJoint }

        val stakingIds = stakableCurrencies.mapNotNullTo(hashSetOf()) { currency ->
            val stakingId = stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = currency)

            if (stakingId.isLeft { it is StakingIdFactory.Error.UnableToGetAddress }) {
                TangemLogger.e(
                    messageString = "Unable to get stakingID for user wallet $userWalletId and currency ${currency.id}",
                    shouldSanitize = false,
                )
            }

            stakingId.getOrNull()
        }

        if (stakingIds.isEmpty()) {
            TangemLogger.i("No staking IDs found for user wallet $userWalletId")
            return Unit.right()
        }

        return multiStakingBalanceFetcher(
            params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
        )
    }
}