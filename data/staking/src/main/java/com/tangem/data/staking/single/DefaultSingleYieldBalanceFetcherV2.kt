package com.tangem.data.staking.single

import arrow.core.Either
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import javax.inject.Inject

/**
 * Default implementation of [MultiYieldBalanceFetcher]
 *
 * @property multiYieldBalanceFetcher multi yield balance fetcher
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleYieldBalanceFetcherV2 @Inject constructor(
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
) {

    suspend fun invoke(params: SingleYieldBalanceFetcher.Params): Either<Throwable, Unit> {
        return multiYieldBalanceFetcher(
            params = YieldBalanceFetcherParams.Multi(
                userWalletId = params.userWalletId,
                currencyIdWithNetworkMap = mapOf(
                    params.currencyId to params.network,
                ),
            ),
        )
    }
}