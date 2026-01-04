package com.tangem.data.staking

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.data.staking.converters.ethpool.P2PEthPoolBroadcastResultConverter
import com.tangem.data.staking.converters.ethpool.P2PEthPoolErrorConverter
import com.tangem.data.staking.converters.ethpool.P2PEthPoolRewardConverter
import com.tangem.data.staking.converters.ethpool.P2PEthPoolStakingAccountConverter
import com.tangem.data.staking.converters.ethpool.P2PEthPoolUnsignedTxConverter
import com.tangem.data.staking.converters.ethpool.P2PEthPoolVaultConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolBroadcastRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolTransactionRequest
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolTransactionResponse
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.ethpool.P2PEthPoolReward
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * P2PEthPool staking repository implementation
 */
internal class DefaultP2PEthPoolRepository(
    private val p2pEthPoolApi: P2PEthPoolApi,
    private val p2pEthPoolVaultsStore: P2PEthPoolVaultsStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val stakingFeatureToggles: StakingFeatureToggles,
) : P2PEthPoolRepository {

    private val vaultConverter = P2PEthPoolVaultConverter
    private val accountConverter = P2PEthPoolStakingAccountConverter
    private val rewardConverter = P2PEthPoolRewardConverter
    private val broadcastResultConverter = P2PEthPoolBroadcastResultConverter
    private val errorConverter = P2PEthPoolErrorConverter

    /**
     * Handles P2PEthPool API response with error checking and result extraction.
     * Reduces duplication across all API call methods.
     */
    private inline fun <T, R> Raise<StakingError>.handleApiResponse(
        response: ApiResponse<P2PEthPoolResponse<T>>,
        transform: (T) -> R,
    ): R = when (response) {
        is ApiResponse.Success -> {
            val data = response.data
            ensure(data.error == null) {
                errorConverter.convertFromErrorDetails(requireNotNull(data.error))
            }
            val result = requireNotNull(data.result) { "Result is null in successful response" }
            transform(result)
        }
        is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
    }

    override suspend fun fetchVaults(network: P2PEthPoolNetwork) {
        val vaults = if (stakingFeatureToggles.isEthStakingEnabled) {
            getVaults(network).getOrElse { error ->
                Timber.e("Error fetching P2PEthPool vaults: $error")
                emptyList()
            }
        } else {
            emptyList()
        }

        p2pEthPoolVaultsStore.store(vaults)
    }

    override suspend fun getVaults(network: P2PEthPoolNetwork): Either<StakingError, List<P2PEthPoolVault>> = either {
        withContext(dispatchers.io) {
            handleApiResponse(p2pEthPoolApi.getVaults(network.value)) { result ->
                result.vaults.map { vaultConverter.convert(it) }
            }
        }
    }

    override suspend fun createDepositTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = createStakingTransaction(
        network = network,
        delegatorAddress = delegatorAddress,
        vaultAddress = vaultAddress,
        amount = amount,
        apiCall = p2pEthPoolApi::createDepositTransaction,
    )

    override suspend fun createUnstakeTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = createStakingTransaction(
        network = network,
        delegatorAddress = delegatorAddress,
        vaultAddress = vaultAddress,
        amount = amount,
        apiCall = p2pEthPoolApi::createUnstakeTransaction,
    )

    override suspend fun createWithdrawTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = createStakingTransaction(
        network = network,
        delegatorAddress = delegatorAddress,
        vaultAddress = vaultAddress,
        amount = amount,
        apiCall = p2pEthPoolApi::createWithdrawTransaction,
    )

    private suspend fun createStakingTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
        apiCall:
        suspend (
            String,
            P2PEthPoolTransactionRequest,
        ) -> ApiResponse<P2PEthPoolResponse<P2PEthPoolTransactionResponse>>,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PEthPoolTransactionRequest(
                delegatorAddress = delegatorAddress,
                vaultAddress = vaultAddress,
                amount = amount.toBigDecimalOrNull()
                    ?: raise(StakingError.InvalidAmount("Invalid amount format: $amount")),
            )
            handleApiResponse(apiCall(network.value, requestBody)) { result ->
                P2PEthPoolUnsignedTxConverter.convert(result.unsignedTransaction)
            }
        }
    }

    override suspend fun broadcastTransaction(
        network: P2PEthPoolNetwork,
        signedTransaction: String,
    ): Either<StakingError, P2PEthPoolBroadcastResult> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PEthPoolBroadcastRequest(signedTransaction = signedTransaction)
            handleApiResponse(p2pEthPoolApi.broadcastTransaction(network.value, requestBody)) { result ->
                broadcastResultConverter.convert(result)
            }
        }
    }

    override suspend fun getAccountInfo(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
    ): Either<StakingError, P2PEthPoolStakingAccount> = either {
        withContext(dispatchers.io) {
            handleApiResponse(
                p2pEthPoolApi.getAccountInfo(network.value, delegatorAddress, vaultAddress),
            ) { result ->
                accountConverter.convert(result)
            }
        }
    }

    override suspend fun getRewards(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        period: Int?,
    ): Either<StakingError, List<P2PEthPoolReward>> = either {
        withContext(dispatchers.io) {
            handleApiResponse(
                p2pEthPoolApi.getRewards(
                    network = network.value,
                    delegatorAddress = delegatorAddress,
                    vaultAddress = vaultAddress,
                    period = period,
                ),
            ) { result ->
                result.rewards.map { rewardConverter.convert(it) }
            }
        }
    }

    override fun getVaultsFlow(): Flow<List<P2PEthPoolVault>> {
        return p2pEthPoolVaultsStore.get()
    }

    override fun getStakingAvailability(): Flow<StakingAvailability> {
        return getVaultsFlow()
            .distinctUntilChanged()
            .map { vaults ->
                if (vaults.isEmpty()) {
                    return@map StakingAvailability.TemporaryUnavailable
                } else {
                    StakingAvailability.Available(StakingOption.P2PEthPool(vaults))
                }
            }
    }

    override suspend fun getStakingAvailabilitySync(): StakingAvailability {
        val vaults = getVaultsSync()
        return if (vaults.isEmpty()) {
            StakingAvailability.TemporaryUnavailable
        } else {
            StakingAvailability.Available(StakingOption.P2PEthPool(vaults))
        }
    }

    override suspend fun getVaultsSync(): List<P2PEthPoolVault> {
        return p2pEthPoolVaultsStore.getSync()
    }
}