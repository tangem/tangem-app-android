package com.tangem.data.staking.multi

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [MultiStakingBalanceFetcher]
 *
 * Supports both StakeKit and P2PEthPool staking providers.
 *
 * @property userWalletsStore        user wallets store
 * @property stakingYieldsStore      staking yields store
 * @property stakeKitBalancesStore    staking balances store (StakeKit)
 * @property p2PEthPoolBalancesStore        P2PEthPool balances store
 * @property stakeKitApi             stake kit API
 * @property p2pEthPoolApi           P2PEthPool API
 * @property p2pEthPoolVaultsStore   P2PEthPool vaults store
 * @property dispatchers             dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultMultiStakingBalanceFetcher @Inject constructor(
    private val userWalletsStore: UserWalletsStore,
    private val stakingYieldsStore: StakingYieldsStore,
    private val stakeKitBalancesStore: StakeKitBalancesStore,
    private val p2PEthPoolBalancesStore: P2PEthPoolBalancesStore,
    private val stakeKitApi: StakeKitApi,
    private val p2pEthPoolApi: P2PEthPoolApi,
    private val p2pEthPoolVaultsStore: P2PEthPoolVaultsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiStakingBalanceFetcher {

    override suspend fun invoke(params: MultiStakingBalanceFetcher.Params): Either<Throwable, Unit> {
        Timber.i("Start fetching staking balances for params:\n$params")

        val stakingIds = params.stakingIds.ifEmpty {
            Timber.i("Nothing to fetch, empty stakingIds for ${params.userWalletId}")
            return Unit.right()
        }

        checkIsSupportedByWalletOrElse(userWalletId = params.userWalletId) {
            return it.left()
        }

        val (stakeKitIds, p2pEthPoolIds) = stakingIds.partition { stakingId ->
            val stakingIntegrationID = StakingIntegrationID.entries.find {
                it.value == stakingId.integrationId
            }
            stakingIntegrationID is StakingIntegrationID.StakeKit
        }

        Timber.i(
            """
                Staking IDs to fetch:
                 - StakeKit: ${stakeKitIds.joinToString()}
                 - P2PEthPool: ${p2pEthPoolIds.joinToString()}
            """.trimIndent(),
        )

        return Either.catchOn(dispatchers.default) {
            coroutineScope {
                if (stakeKitIds.isNotEmpty()) {
                    launch { fetchStakeKitBalances(params.userWalletId, stakeKitIds.toSet()) }
                }

                if (p2pEthPoolIds.isNotEmpty()) {
                    launch { fetchP2PBalances(params.userWalletId, p2pEthPoolIds.toSet()) }
                }
            }
        }
            .onLeft { throwable ->
                Timber.e(throwable, "Unable to fetch staking balances $params")

                if (stakeKitIds.isNotEmpty()) {
                    stakeKitBalancesStore.storeError(
                        userWalletId = params.userWalletId,
                        stakingIds = stakeKitIds.toSet(),
                    )
                }
                if (p2pEthPoolIds.isNotEmpty()) {
                    p2PEthPoolBalancesStore.storeError(
                        userWalletId = params.userWalletId,
                        stakingIds = p2pEthPoolIds.toSet(),
                    )
                }
            }
    }

    private suspend fun fetchStakeKitBalances(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        stakeKitBalancesStore.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        val availableStakingIds = getAvailableStakingIds(
            userWalletId = userWalletId,
            stakingIds = stakingIds,
        )

        fetchFromStakeKit(userWalletId = userWalletId, stakingIds = availableStakingIds)
    }

    private suspend fun fetchP2PBalances(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        p2PEthPoolBalancesStore.refresh(userWalletId = userWalletId, stakingIds = stakingIds)

        val vaults = runSuspendCatching { p2pEthPoolVaultsStore.getSync() }.getOrNull().orEmpty()
        if (vaults.isEmpty()) {
            Timber.w("No P2PEthPool vaults available for $userWalletId, storing empty balances")
            p2PEthPoolBalancesStore.storeEmpty(userWalletId = userWalletId, stakingIds = stakingIds)
            return
        }

        fetchFromP2P(userWalletId = userWalletId, stakingIds = stakingIds, vaults = vaults)
    }

    private suspend fun fetchFromP2P(
        userWalletId: UserWalletId,
        stakingIds: Set<StakingID>,
        vaults: List<P2PEthPoolVault>,
    ) {
        safeApiCall(
            call = {
                val addresses = stakingIds.map { it.address }.toSet()
                val responses = fetchP2PAccountResponses(vaults = vaults, addresses = addresses)

                Timber.i("Successfully fetched ${responses.size} P2PEthPool balances for $userWalletId")

                if (responses.isNotEmpty()) {
                    p2PEthPoolBalancesStore.storeActual(userWalletId = userWalletId, values = responses)

                    val missingStakingIds = stakingIds.filter { stakingId ->
                        responses.none { response ->
                            response.delegatorAddress.equals(stakingId.address, ignoreCase = true)
                        }
                    }

                    if (missingStakingIds.isNotEmpty()) {
                        Timber.i("Missing responses for ${missingStakingIds.size} staking IDs: $missingStakingIds")
                        p2PEthPoolBalancesStore.storeError(
                            userWalletId = userWalletId,
                            stakingIds = missingStakingIds.toSet(),
                        )
                    }
                } else {
                    Timber.i("No P2PEthPool responses received for $userWalletId")
                    p2PEthPoolBalancesStore.storeError(userWalletId = userWalletId, stakingIds = stakingIds)
                }
            },
            onError = { throwable ->
                Timber.e(throwable, "Unable to fetch P2PEthPool balances $userWalletId")

                p2PEthPoolBalancesStore.storeError(userWalletId = userWalletId, stakingIds = stakingIds)

                throw throwable
            },
        )
    }

    private suspend fun fetchP2PAccountResponses(
        vaults: List<P2PEthPoolVault>,
        addresses: Set<String>,
    ): Set<P2PEthPoolAccountResponse> {
        val responses = mutableSetOf<P2PEthPoolAccountResponse>()

        for (vault in vaults) {
            for (address in addresses) {
                runSuspendCatching {
                    val response = p2pEthPoolApi.getAccountInfo(
                        network = P2PEthPoolStakingConfig.activeNetwork.value,
                        delegatorAddress = address,
                        vaultAddress = vault.vaultAddress,
                    )

                    when (response) {
                        is ApiResponse.Success -> {
                            val data = response.data
                            if (data.error != null) {
                                Timber.w(
                                    "P2PEthPool API returned error for vault ${vault.vaultAddress}, " +
                                        "address $address: ${data.error ?: "error"}",
                                )
                            } else {
                                val result = requireNotNull(data.result) {
                                    "Result is null in successful response"
                                }
                                responses.add(result)
                            }
                        }
                        is ApiResponse.Error -> {
                            Timber.w(
                                response.cause,
                                "Failed to fetch P2PEthPool balance for vault ${vault.vaultAddress}, " +
                                    "address $address",
                            )
                        }
                    }
                }.onFailure { error ->
                    Timber.w(
                        error,
                        "Failed to fetch P2PEthPool balance for vault ${vault.vaultAddress}, address $address",
                    )
                }
            }
        }

        return responses
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

    private suspend fun getAvailableStakingIds(userWalletId: UserWalletId, stakingIds: Set<StakingID>): Set<StakingID> {
        val yieldIds = getYieldsIds(userWalletId = userWalletId)

        // [true] -> available
        // [false] -> unavailable
        val groupedStakingIds = stakingIds.groupBy { stakingId ->
            yieldIds.any { it == stakingId.integrationId }
        }

        val availableStakingIds = groupedStakingIds[true].orEmpty()
        val unavailableStakingIds = groupedStakingIds[false].orEmpty()

        Timber.i(
            """
                Available staking IDs: ${availableStakingIds.joinToString()}
                Unavailable staking IDs: ${unavailableStakingIds.joinToString()}
            """.trimIndent(),
        )

        if (unavailableStakingIds.isNotEmpty()) {
            stakeKitBalancesStore.storeError(userWalletId = userWalletId, stakingIds = unavailableStakingIds.toSet())
        }

        return availableStakingIds.toSet().ifEmpty {
            val exception = IllegalStateException(
                """
                    No available yields to fetch yield balances:
                     – userWalletId: $userWalletId
                     – stakingIds: ${stakingIds.joinToString()}
                """.trimIndent(),
            )
            Timber.i(exception)
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

    private suspend fun fetchFromStakeKit(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        safeApiCall(
            call = {
                val requests = stakingIds.map(YieldBalanceRequestBodyFactory::create)

                val yieldBalances = coroutineScope {
                    requests
                        // TODO: in the future, consider optimizing this part
                        .chunked(size = 15) // StakeKitApi limitation: no more than 15 requests at the same time
                        .map {
                            async(dispatchers.io) {
                                stakeKitApi.getMultipleYieldBalances(it).bind()
                            }
                        }
                        .awaitAll()
                        .flatten()
                        .toSet()
                }

                Timber.i(
                    "Successfully fetched staking balances for $userWalletId:\n${yieldBalances.joinToString("\n")}",
                )
                stakeKitBalancesStore.storeActual(userWalletId = userWalletId, values = yieldBalances)

                if (!allResponsesReceived(requests, yieldBalances)) {
                    val values = stakingIds.filter { stakingId ->
                        yieldBalances.none { balanceWrapper ->
                            stakingId.integrationId == balanceWrapper.integrationId &&
                                stakingId.address == balanceWrapper.addresses.address
                        }
                    }

                    stakeKitBalancesStore.storeError(userWalletId = userWalletId, stakingIds = values.toSet())
                }
            },
            onError = { throwable ->
                Timber.e(throwable, "Unable to fetch staking balances $userWalletId")

                stakeKitBalancesStore.storeError(userWalletId = userWalletId, stakingIds = stakingIds)

                throw throwable
            },
        )
    }

    private fun allResponsesReceived(
        requests: List<YieldBalanceRequestBody>,
        yieldBalances: Set<YieldBalanceWrapperDTO>,
    ): Boolean {
        return requests.all { request ->
            yieldBalances.any { balance ->
                request.integrationId == balance.integrationId &&
                    request.addresses.address == balance.addresses.address
            }
        }
    }
}