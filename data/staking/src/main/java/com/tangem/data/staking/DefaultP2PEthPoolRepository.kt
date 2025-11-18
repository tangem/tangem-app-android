package com.tangem.data.staking

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.data.staking.converters.ethpool.*
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolBroadcastRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolDepositRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolUnstakeRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolWithdrawRequest
import com.tangem.domain.staking.model.ethpool.*
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * P2P staking repository implementation
 */
internal class DefaultP2PEthPoolRepository(
    private val p2pApi: P2PEthPoolApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : P2PEthPoolRepository {

    private val vaultConverter = P2PEthPoolVaultConverter
    private val accountInfoConverter = P2PEthPoolAccountConverter
    private val rewardConverter = P2PEthPoolRewardConverter
    private val broadcastResultConverter = P2PEthPoolBroadcastResultConverter
    private val errorConverter = P2PEthPoolErrorConverter

    override suspend fun getVaults(network: P2PEthPoolNetwork): Either<StakingError, List<P2PEthPoolVault>> = either {
        withContext(dispatchers.io) {
            val response = p2pApi.getVaults(network.value)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    result.vaults.map { vaultConverter.convert(it) }
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun createDepositTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PEthPoolDepositRequest(
                delegatorAddress = delegatorAddress,
                vaultAddress = vaultAddress,
                amount = amount.toDoubleOrNull() ?: raise(StakingError.InvalidAmount("Invalid amount format: $amount")),
            )
            val response = p2pApi.createDepositTransaction(network.value, requestBody)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    P2PEthPoolUnsignedTxConverter.convert(result.unsignedTransaction)
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun createUnstakeTransaction(
        network: P2PEthPoolNetwork,
        stakerPublicKey: String,
        stakeTransactionHash: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PEthPoolUnstakeRequest(
                stakerPublicKey = stakerPublicKey,
                stakeTransactionHash = stakeTransactionHash,
            )
            val response = p2pApi.createUnstakeTransaction(network.value, requestBody)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    // Note: API returns only hex string for unstake, not full transaction structure
                    P2PEthPoolUnsignedTx(
                        serializeTx = result.unstakeTransactionHex,
                        to = "", // Will be parsed from hex by wallet
                        data = result.unstakeTransactionHex,
                        value = java.math.BigDecimal.ZERO,
                        nonce = 0,
                        chainId = network.chainId,
                        gasLimit = java.math.BigDecimal.ZERO,
                        maxFeePerGas = java.math.BigDecimal.ZERO,
                        maxPriorityFeePerGas = java.math.BigDecimal.ZERO,
                    )
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun createWithdrawTransaction(
        network: P2PEthPoolNetwork,
        stakerAddress: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PEthPoolWithdrawRequest(stakerAddress = stakerAddress)
            val response = p2pApi.createWithdrawTransaction(network.value, requestBody)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    P2PEthPoolUnsignedTxConverter.convert(result.unsignedTransaction)
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun broadcastTransaction(
        network: P2PEthPoolNetwork,
        signedTransaction: String,
    ): Either<StakingError, P2PEthPoolBroadcastResult> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PEthPoolBroadcastRequest(signedTransaction = signedTransaction)
            val response = p2pApi.broadcastTransaction(network.value, requestBody)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    broadcastResultConverter.convert(result)
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun getAccountInfo(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
    ): Either<StakingError, P2PEthPoolAccount> = either {
        withContext(dispatchers.io) {
            val response = p2pApi.getAccountInfo(network.value, delegatorAddress, vaultAddress)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    accountInfoConverter.convert(result)
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
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
            val response = p2pApi.getRewards(network.value, delegatorAddress, vaultAddress, period)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    result.rewards.map { rewardConverter.convert(it) }
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }
}