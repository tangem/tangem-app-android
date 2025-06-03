package com.tangem.data.staking.multi

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.staking.fetcher.YieldBalanceFetcherImplementor
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.model.StakingID
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Implementor of fetcher for refreshing multiple yield balances
 *
 * @property yieldsBalancesStore yields balances store
 * @property stakingIdFactory    factory for creating [StakingID]
 * @property stakeKitApi         StakeKit API
 * @property dispatchers         dispatchers
 */
internal class MultiYieldBalanceFetcherImplementor(
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val stakingIdFactory: StakingIdFactory,
    private val stakeKitApi: StakeKitApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldBalanceFetcherImplementor<YieldBalanceFetcherParams.Multi> {

    override suspend fun createStakingIds(params: YieldBalanceFetcherParams.Multi): Set<StakingID> {
        return params.currencyIdWithNetworkMap.flatMapTo(hashSetOf()) { (currencyId, network) ->
            stakingIdFactory.create(userWalletId = params.userWalletId, currencyId = currencyId, network = network)
        }
    }

    override suspend fun fetch(params: YieldBalanceFetcherParams.Multi, stakingIds: Set<StakingID>) {
        safeApiCall(
            call = {
                val requests = stakingIds.map(YieldBalanceRequestBodyFactory::create)

                val yieldBalances = withContext(dispatchers.io) {
                    stakeKitApi.getMultipleYieldBalances(requests).bind()
                }

                yieldsBalancesStore.storeActual(userWalletId = params.userWalletId, values = yieldBalances)

                if (!allResponsesReceived(requests, yieldBalances)) {
                    val values = stakingIds.filter { stakingId ->
                        yieldBalances.none {
                            stakingId.integrationId == it.integrationId &&
                                stakingId.address == it.addresses.address
                        }
                    }

                    yieldsBalancesStore.storeError(userWalletId = params.userWalletId, stakingIds = values.toSet())
                }
            },
            onError = {
                Timber.e(it, "Unable to fetch yield balances $params")

                yieldsBalancesStore.storeError(userWalletId = params.userWalletId, stakingIds = stakingIds)

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