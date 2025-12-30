package com.tangem.data.staking.single

import arrow.core.Either
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import javax.inject.Inject

/**
 * Default implementation of [SingleStakingBalanceFetcher]
 *
 * @property multiStakingBalanceFetcher multi staking balance fetcher
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleStakingBalanceFetcher @Inject constructor(
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
) : SingleStakingBalanceFetcher {

    override suspend fun invoke(params: SingleStakingBalanceFetcher.Params): Either<Throwable, Unit> {
        return multiStakingBalanceFetcher(
            params = MultiStakingBalanceFetcher.Params(
                userWalletId = params.userWalletId,
                stakingIds = setOf(params.stakingId),
            ),
        )
    }
}