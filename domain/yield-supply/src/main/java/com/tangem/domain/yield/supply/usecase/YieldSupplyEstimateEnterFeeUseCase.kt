package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterWithNativeCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.fixFee
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.utils.extensions.isSingleItem
import com.tangem.utils.logging.TangemLogger
import java.math.BigInteger

class YieldSupplyEstimateEnterFeeUseCase(
    private val feeRepository: FeeRepository,
    private val feeErrorResolver: FeeErrorResolver,
    private val blockAidGasEstimate: BlockAidGasEstimate,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
        policy: SimulationPolicy = SimulationPolicy(),
    ): Either<GetFeeError, List<TransactionData.Uncompiled>> = either {
        if (transactionDataList.isSingleItem()) {
            return@either catchFee {
                transactionDataList.map { transaction ->
                    transaction.copy(
                        fee = feeRepository.calculateFee(
                            userWallet = userWallet,
                            cryptoCurrency = cryptoCurrency,
                            transactionData = transaction,
                        ).normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                    )
                }
            }
        }

        when (val simulation = simulate(cryptoCurrency, transactionDataList)) {
            is GasEstimationResult.Estimated -> catchFee {
                priceSimulatedGas(
                    userWallet = userWallet,
                    cryptoCurrency = cryptoCurrency,
                    transactionDataList = transactionDataList,
                    gasLimits = simulation.gasLimits,
                )
            }
            GasEstimationResult.Reverted -> {
                ensure(!policy.isRevertFatal) { GetFeeError.SimulationError.Reverted }
                catchFee { estimateFeeWithStatic(userWallet, cryptoCurrency, transactionDataList) }
            }
            GasEstimationResult.Unavailable -> {
                ensure(policy.isStaticFallbackAllowed) { GetFeeError.SimulationError.Unavailable }
                catchFee { estimateFeeWithStatic(userWallet, cryptoCurrency, transactionDataList) }
            }
        }
    }

    private suspend fun <T> Raise<GetFeeError>.catchFee(block: suspend () -> T): T =
        Either.catch { block() }.mapLeft(feeErrorResolver::resolve).bind()

    private suspend fun simulate(
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): GasEstimationResult {
        val result = blockAidGasEstimate.getGasEstimation(
            cryptoCurrency = cryptoCurrency,
            transactionDataList = transactionDataList,
        ).onLeft { TangemLogger.e("Error", it) }.getOrNull() ?: return GasEstimationResult.Unavailable

        if (result == GasEstimationResult.Reverted) TangemLogger.e("Gas simulation reverted")
        return result
    }

    private suspend fun priceSimulatedGas(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
        gasLimits: List<BigInteger>,
    ): List<TransactionData.Uncompiled> {
        val fee = feeRepository.getEthereumFeeWithoutGas(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrency,
        )

        return transactionDataList.zip(gasLimits) { transaction, estimatedGas ->
            transaction.copy(
                fee = fee.fixFee(cryptoCurrency, estimatedGas)
                    .increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
            )
        }
    }

    private fun TransactionData.Uncompiled.isEnter(): Boolean {
        val callData = (extras as? EthereumTransactionExtras)?.callData?.unwrapYieldModuleUpgrade()
        return callData is EthereumYieldSupplyEnterCallData || callData is EthereumYieldSupplyEnterWithNativeCallData
    }

    private suspend fun estimateFeeWithStatic(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): List<TransactionData.Uncompiled> {
        val withCalculatedFees = transactionDataList.filterNot { it.isEnter() }.map { transaction ->
            transaction.copy(
                fee = feeRepository.calculateFee(
                    userWallet = userWallet,
                    cryptoCurrency = cryptoCurrency,
                    transactionData = transaction,
                ).normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
            )
        }

        val calculatedFee = withCalculatedFees.firstOrNull()?.fee ?: error("Must be any calculated fee")

        val withEstimatedFees = transactionDataList.filter { it.isEnter() }.map { transaction ->
            transaction.copy(
                fee = calculatedFee.fixFee(cryptoCurrency, ETHEREUM_CONSTANT_GAS_LIMIT)
                    .increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
            )
        }

        // First return calculated fees then estimated
        return withCalculatedFees + withEstimatedFees
    }

    /** What an estimate of several transactions does when their simulation does not price them */
    data class SimulationPolicy(
        val isRevertFatal: Boolean = false,
        val isStaticFallbackAllowed: Boolean = true,
    )

    private companion object {
        // Using constant gas limit to avoid fee calculation errors when contract address is not deployed yet
        val ETHEREUM_CONSTANT_GAS_LIMIT = 500_000.toBigInteger()
    }
}