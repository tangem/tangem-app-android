package com.tangem.data.staking.multi

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toOption
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [MultiYieldBalanceFetcher]
 *
 * @property userWalletsStore    user wallets store
 * @property stakingYieldsStore  staking yields store
 * @property yieldsBalancesStore yields balances store
 * @property stakingIdFactory    factory for creating StakingID
 * @property stakeKitApi         stake kit API
 * @property dispatchers         dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiYieldBalanceFetcher @Inject constructor(
    private val userWalletsStore: UserWalletsStore,
    private val stakingYieldsStore: StakingYieldsStore,
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val stakingIdFactory: StakingIdFactory,
    private val stakeKitApi: StakeKitApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiYieldBalanceFetcher {

    override suspend fun invoke(params: MultiYieldBalanceFetcher.Params): Either<Throwable, Unit> {
        checkIsSupportedByWalletOrElse(userWalletId = params.userWalletId) {
            return it.left()
        }

        val stakingIds = getStakingIds(params).getOrElse {
            return it.left()
        }

        return Either.catchOn(dispatchers.default) {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = stakingIds)

            val availableStakingIds = getAvailableStakingIds(
                userWalletId = params.userWalletId,
                stakingIds = stakingIds,
            )

            fetch(userWalletId = params.userWalletId, stakingIds = availableStakingIds)
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

    private suspend fun getStakingIds(params: MultiYieldBalanceFetcher.Params) = either {
        val stakingIds = catch(
            block = {
                params.currencyIdWithNetworkMap.flatMapTo(hashSetOf()) { (currencyId, network) ->
                    stakingIdFactory.create(
                        userWalletId = params.userWalletId,
                        currencyId = currencyId,
                        network = network,
                    )
                }
            },
            catch = ::raise,
        )

        ensure(stakingIds.isNotEmpty()) {
            val exception = IllegalStateException("Unable to create staking ids for $params: list is empty")
            Timber.e(exception)

            raise(exception)
        }

        stakingIds
    }

    private suspend fun getAvailableStakingIds(userWalletId: UserWalletId, stakingIds: Set<StakingID>): Set<StakingID> {
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

    private suspend fun fetch(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        safeApiCall(
            call = {
                val requests = stakingIds.map(YieldBalanceRequestBodyFactory::create)

                val yieldBalances = coroutineScope {
                    requests
                        // TODO: in the future, consider optimizing this part
                        .chunked(size = 15) // StakeKitApi limitation: no more than 15 requests at the same time
                        .map {
                            async(dispatchers.io) { stakeKitApi.getMultipleYieldBalances(it).bind() }
                        }
                        .awaitAll()
                        .flatten()
                        .toSet()
                }

                yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = yieldBalances)

                if (!allResponsesReceived(requests, yieldBalances)) {
                    val values = stakingIds.filter { stakingId ->
                        yieldBalances.none {
                            stakingId.integrationId == it.integrationId &&
                                stakingId.address == it.addresses.address
                        }
                    }

                    yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = values.toSet())
                }
            },
            onError = {
                Timber.e(it, "Unable to fetch yield balances $userWalletId")

                yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = stakingIds)

                throw it
            },
        )
    }

    private fun allResponsesReceived(
        requests: List<YieldBalanceRequestBody>,
        yieldBalances: Set<YieldBalanceWrapperDTO>,
    ): Boolean {
        return requests.all { request ->
            yieldBalances.any {
                request.integrationId == it.integrationId &&
                    request.addresses.address == it.addresses.address
            }
        }
    }
}