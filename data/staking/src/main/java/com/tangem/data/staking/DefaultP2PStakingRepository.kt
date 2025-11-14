package com.tangem.data.staking

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.data.staking.converters.p2p.*
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.p2p.P2PApi
import com.tangem.datasource.api.p2p.models.request.P2PBroadcastTransactionRequestBody
import com.tangem.datasource.api.p2p.models.request.P2PDepositRequestBody
import com.tangem.datasource.api.p2p.models.request.P2PUnstakeRequestBody
import com.tangem.datasource.api.p2p.models.request.P2PWithdrawRequestBody
import com.tangem.domain.staking.model.p2p.*
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PStakingRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * P2P staking repository implementation
 */
internal class DefaultP2PStakingRepository(
    private val p2pApi: P2PApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : P2PStakingRepository {

    private val vaultConverter = P2PVaultConverter
    private val accountInfoConverter = P2PAccountInfoConverter
    private val rewardConverter = P2PRewardConverter
    private val broadcastResultConverter = P2PBroadcastResultConverter
    private val errorConverter = P2PErrorConverter

    override suspend fun getVaults(network: P2PNetwork): Either<StakingError, List<P2PVault>> = either {
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
        network: P2PNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PUnsignedTransaction> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PDepositRequestBody(
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
                    P2PUnsignedTransactionConverter.convert(result.unsignedTransaction)
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun createUnstakeTransaction(
        network: P2PNetwork,
        stakerPublicKey: String,
        stakeTransactionHash: String,
    ): Either<StakingError, P2PUnsignedTransaction> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PUnstakeRequestBody(
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
                    P2PUnsignedTransaction(
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
        network: P2PNetwork,
        stakerAddress: String,
    ): Either<StakingError, P2PUnsignedTransaction> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PWithdrawRequestBody(stakerAddress = stakerAddress)
            val response = p2pApi.createWithdrawTransaction(network.value, requestBody)
            when (response) {
                is ApiResponse.Success -> {
                    val data = response.data
                    ensure(data.error == null) {
                        errorConverter.convertFromErrorDetails(requireNotNull(data.error))
                    }
                    val result = requireNotNull(data.result) { "Result is null in successful response" }
                    P2PUnsignedTransactionConverter.convert(result.unsignedTransaction)
                }
                is ApiResponse.Error -> raise(StakingError.UnknownError(response.cause))
            }
        }
    }

    override suspend fun broadcastTransaction(
        network: P2PNetwork,
        signedTransaction: String,
    ): Either<StakingError, P2PBroadcastResult> = either {
        withContext(dispatchers.io) {
            val requestBody = P2PBroadcastTransactionRequestBody(signedTransaction = signedTransaction)
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
        network: P2PNetwork,
        delegatorAddress: String,
        vaultAddress: String,
    ): Either<StakingError, P2PAccountInfo> = either {
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
        network: P2PNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        period: Int?,
    ): Either<StakingError, List<P2PReward>> = either {
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