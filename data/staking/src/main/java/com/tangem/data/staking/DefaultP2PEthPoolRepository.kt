package com.tangem.data.staking

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.data.staking.converters.ethpool.*
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolBroadcastRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolTransactionRequest
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolTransactionResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.P2PVaultLimitsStore
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.staking.model.P2PEthPoolIntegration
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * P2PEthPool staking repository implementation
 */
internal class DefaultP2PEthPoolRepository(
    private val p2pEthPoolApi: P2PEthPoolApi,
    private val p2pEthPoolVaultsStore: P2PEthPoolVaultsStore,
    private val p2pVaultLimitsStore: P2PVaultLimitsStore,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val stakingFeatureToggles: StakingFeatureToggles,
) : P2PEthPoolRepository {

    private val vaultConverter = P2PEthPoolVaultConverter
    private val accountConverter = P2PEthPoolStakingAccountConverter
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
        val vaults = if (stakingFeatureToggles.isIntegrationEnabled(StakingIntegrationID.P2PEthPool)) {
            getVaults(network).getOrElse { error ->
                TangemLogger.e("Error fetching P2PEthPool vaults: $error")
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
                result.vaults
                    .map { vaultConverter.convert(it) }
                    .filter { it.vaultAddress.lowercase() !in P2PEthPoolStakingConfig.TEST_VAULT_ADDRESSES }
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

    override fun getVaultsFlow(): Flow<List<P2PEthPoolVault>> {
        return p2pEthPoolVaultsStore.get()
    }

    override fun getStakingAvailability(): Flow<StakingAvailability> {
        return combine(
            getVaultsFlow().distinctUntilChanged(),
            getVaultLimitsFlow().distinctUntilChanged(),
        ) { vaults, limits ->
            when {
                vaults.isEmpty() -> StakingAvailability.TemporaryUnavailable
                limits == null -> StakingAvailability.TemporaryUnavailable
                else -> {
                    val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)
                    if (integration.areAllTargetsFull) {
                        StakingAvailability.Full(StakingOption.P2PEthPool(vaults))
                    } else {
                        StakingAvailability.Available(StakingOption.P2PEthPool(vaults))
                    }
                }
            }
        }.distinctUntilChanged()
    }

    override suspend fun getStakingAvailabilitySync(): StakingAvailability {
        val vaults = getVaultsSync()
        if (vaults.isEmpty()) return StakingAvailability.TemporaryUnavailable
        val limits = getVaultLimitsSyncOrNull() ?: return StakingAvailability.TemporaryUnavailable
        val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)
        return if (integration.areAllTargetsFull) {
            StakingAvailability.Full(StakingOption.P2PEthPool(vaults))
        } else {
            StakingAvailability.Available(StakingOption.P2PEthPool(vaults))
        }
    }

    override suspend fun getVaultsSync(): List<P2PEthPoolVault> {
        return p2pEthPoolVaultsStore.getSync()
    }

    override suspend fun fetchVaultLimits() {
        runSuspendCatching {
            val response = withContext(dispatchers.io) {
                tangemTechApi.getCoinsSettings().getOrThrow()
            }
            val vaults = response.staking?.vaults.orEmpty()
            val limits = vaults
                .mapNotNull { vault ->
                    val limit = vault.limit ?: return@mapNotNull null
                    vault.vaultAddress.lowercase() to VaultLimitInfo(
                        limit = limit,
                        coefficient = vault.coefficient,
                    )
                }
                .toMap()
            p2pVaultLimitsStore.store(limits)
        }.onFailure { e ->
            TangemLogger.e("Error fetching P2P vault limits: ${e.message}", e)
        }
    }

    override fun getVaultLimitsFlow(): Flow<Map<String, VaultLimitInfo>?> {
        return p2pVaultLimitsStore.get()
    }

    override suspend fun getVaultLimitsSyncOrNull(): Map<String, VaultLimitInfo>? {
        return p2pVaultLimitsStore.getSyncOrNull()
    }
}