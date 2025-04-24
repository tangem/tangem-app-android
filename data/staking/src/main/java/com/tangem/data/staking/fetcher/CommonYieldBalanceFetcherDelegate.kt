package com.tangem.data.staking.fetcher

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import timber.log.Timber

internal fun <Params : YieldBalanceFetcherParams> commonFetcher(
    implementor: YieldBalanceFetcherImplementor<Params>,
    stakingYieldsStore: StakingYieldsStore,
    yieldsBalancesStore: YieldsBalancesStore,
    dispatchers: CoroutineDispatcherProvider,
): FlowFetcher<Params> {
    return CommonYieldBalanceFetcher(
        implementor = implementor,
        stakingYieldsStore = stakingYieldsStore,
        yieldsBalancesStore = yieldsBalancesStore,
        dispatchers = dispatchers,
    )
}

/**
 * Common implementation of YieldBalanceFetcher
 *
 * @param implementor            fetcher implementor
 * @property stakingYieldsStore  staking yields store
 * @property yieldsBalancesStore yields balances store
 * @property dispatchers         dispatchers
 */
private class CommonYieldBalanceFetcher<Params : YieldBalanceFetcherParams>(
    private val implementor: YieldBalanceFetcherImplementor<Params>,
    private val stakingYieldsStore: StakingYieldsStore,
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : FlowFetcher<Params> {

    override suspend fun invoke(params: Params): Either<Throwable, Unit> {
        val stakingIds = getStakingIds(params).getOrElse {
            return it.left()
        }

        return Either.catchOn(dispatchers.default) {
            val requests = prefetch(userWalletId = params.userWalletId, stakingIds = stakingIds)

            implementor.fetch(params = params, stakingIds = stakingIds, requests)
        }
            .onLeft { yieldsBalancesStore.storeError(userWalletId = params.userWalletId, stakingIds = stakingIds) }
    }

    private suspend fun getStakingIds(params: Params): Either<Throwable, Set<StakingID>> = either {
        val stakingIds = catch(
            block = { implementor.createStakingIds(params = params) },
            catch = { raise(it) },
        )

        ensure(stakingIds.isNotEmpty()) {
            val exception = IllegalStateException("Unable to create staking ids for $params: list is empty")
            Timber.e(exception)

            raise(exception)
        }

        stakingIds
    }

    private suspend fun prefetch(
        userWalletId: UserWalletId,
        stakingIds: Set<StakingID>,
    ): List<YieldBalanceRequestBody> {
        yieldsBalancesStore.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        val yieldDTOs = stakingYieldsStore.getSyncWithTimeout()

        if (yieldDTOs.isNullOrEmpty()) {
            val exception = IllegalStateException("No enabled yields for $userWalletId")
            Timber.e(exception)
            throw exception
        }

        val yieldIds = yieldDTOs.mapNotNullTo(destination = hashSetOf(), transform = YieldDTO::id)

        val requests = stakingIds
            .filter { stakingId -> yieldIds.any { it == stakingId.integrationId } }
            .map(YieldBalanceRequestBodyFactory::create)

        if (requests.isEmpty()) {
            val exception = IllegalStateException(
                """
                    No available yields to fetch yield balances:
                     – userWalletId: $userWalletId
                     – stakingIds: ${stakingIds.joinToString()}
                """.trimIndent(),
            )
            Timber.d(exception)
            throw exception
        }

        return requests
    }
}