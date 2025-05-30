package com.tangem.data.staking.fetcher

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toOption
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import timber.log.Timber

internal fun <Params : YieldBalanceFetcherParams> commonFetcher(
    implementor: YieldBalanceFetcherImplementor<Params>,
    userWalletsStore: UserWalletsStore,
    stakingYieldsStore: StakingYieldsStore,
    yieldsBalancesStore: YieldsBalancesStore,
    dispatchers: CoroutineDispatcherProvider,
): FlowFetcher<Params> {
    return CommonYieldBalanceFetcher(
        implementor = implementor,
        userWalletsStore = userWalletsStore,
        stakingYieldsStore = stakingYieldsStore,
        yieldsBalancesStore = yieldsBalancesStore,
        dispatchers = dispatchers,
    )
}

/**
 * Common implementation of YieldBalanceFetcher
 *
 * @property implementor         fetcher implementor
 * @property userWalletsStore    user wallets store
 * @property stakingYieldsStore  staking yields store
 * @property yieldsBalancesStore yields balances store
 * @property dispatchers         dispatchers
 */
private class CommonYieldBalanceFetcher<Params : YieldBalanceFetcherParams>(
    private val implementor: YieldBalanceFetcherImplementor<Params>,
    private val userWalletsStore: UserWalletsStore,
    private val stakingYieldsStore: StakingYieldsStore,
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : FlowFetcher<Params> {

    override suspend fun invoke(params: Params): Either<Throwable, Unit> {
        checkIsSupportedByWalletOrElse(userWalletId = params.userWalletId) {
            return it.left()
        }

        val stakingIds = getStakingIds(params).getOrElse {
            return it.left()
        }

        return Either.catchOn(dispatchers.default) {
            val availableStakingIds = prefetch(
                userWalletId = params.userWalletId,
                stakingIds = stakingIds,
            )

            implementor.fetch(params = params, stakingIds = availableStakingIds)
        }
            .onLeft {
                Timber.e(it, "Unable to fetch yield balances $params")

                yieldsBalancesStore.storeError(userWalletId = params.userWalletId, stakingIds = stakingIds)
            }
    }

    private inline fun checkIsSupportedByWalletOrElse(userWalletId: UserWalletId, ifNotSupported: (Throwable) -> Unit) {
        val maybeUserWallet = userWalletsStore.getSyncOrNull(key = userWalletId).toOption()

        val isSupportedByWallet = maybeUserWallet.isSome(UserWallet::isMultiCurrency)

        if (!isSupportedByWallet) {
            val exception = IllegalStateException("Wallet $userWalletId is not supported: $maybeUserWallet")
            Timber.e(exception)

            ifNotSupported(exception)
        }
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

    private suspend fun prefetch(userWalletId: UserWalletId, stakingIds: Set<StakingID>): Set<StakingID> {
        yieldsBalancesStore.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        val yieldIds = getYieldsIds(userWalletId = userWalletId)

        // [true] -> available
        // [false] -> unavailable
        val groupedStakingIds = stakingIds.groupBy { stakingId ->
            yieldIds.any { it == stakingId.integrationId }
        }

        val availableStakingIds = groupedStakingIds[true].orEmpty()
        val unavailableStakingIds = groupedStakingIds[false].orEmpty()

        if (unavailableStakingIds.isNotEmpty()) {
            yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = unavailableStakingIds.toSet())
        }

        return availableStakingIds.toSet().ifEmpty {
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
    }

    private suspend fun getYieldsIds(userWalletId: UserWalletId): Set<String> {
        val yieldsIds = stakingYieldsStore.getSyncWithTimeout().orEmpty()
            .mapNotNullTo(destination = hashSetOf(), transform = YieldDTO::id)

        if (yieldsIds.isEmpty()) {
            val exception = IllegalStateException("No enabled yields for $userWalletId")
            Timber.e(exception)

            throw exception
        }

        return yieldsIds
    }
}