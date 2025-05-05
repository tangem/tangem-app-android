package com.tangem.data.staking.single

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.staking.fetcher.YieldBalanceFetcherImplementor
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.model.StakingID
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Implementor of fetcher for refreshing single yield balance
 *
 * @property yieldsBalancesStore yields balances store
 * @property stakingIdFactory    factory for creating [StakingID]
 * @property stakeKitApi         StakeKit API
 * @property dispatchers         dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class SingleYieldBalanceFetcherImplementor(
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val stakingIdFactory: StakingIdFactory,
    private val stakeKitApi: StakeKitApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldBalanceFetcherImplementor<YieldBalanceFetcherParams.Single> {

    override suspend fun createStakingIds(params: YieldBalanceFetcherParams.Single): Set<StakingID> {
        val dataStakingId = stakingIdFactory.createForDefault(
            userWalletId = params.userWalletId,
            currencyId = params.currencyId,
            network = params.network,
        ) ?: return emptySet()

        return setOf(
            StakingID(integrationId = dataStakingId.integrationId, address = dataStakingId.address),
        )
    }

    override suspend fun fetch(
        params: YieldBalanceFetcherParams.Single,
        stakingIds: Set<StakingID>,
        requests: List<YieldBalanceRequestBody>,
    ) {
        fetchInternal(
            params = params,
            stakingId = stakingIds.first(),
            request = requests.first(),
        )
    }

    private suspend fun fetchInternal(
        params: YieldBalanceFetcherParams.Single,
        stakingId: StakingID,
        request: YieldBalanceRequestBody,
    ) {
        safeApiCall(
            call = {
                val result = withContext(dispatchers.io) {
                    stakeKitApi.getSingleYieldBalance(
                        integrationId = stakingId.integrationId,
                        body = request,
                    ).bind()
                }

                yieldsBalancesStore.storeActual(
                    userWalletId = params.userWalletId,
                    values = setOf(
                        YieldBalanceWrapperDTO(
                            balances = result,
                            integrationId = request.integrationId,
                            addresses = request.addresses,
                        ),
                    ),
                )
            },
            onError = {
                Timber.e(it, "Unable to fetch yield balances $params")

                yieldsBalancesStore.storeError(userWalletId = params.userWalletId, stakingIds = setOf(stakingId))

                throw it
            },
        )
    }
}