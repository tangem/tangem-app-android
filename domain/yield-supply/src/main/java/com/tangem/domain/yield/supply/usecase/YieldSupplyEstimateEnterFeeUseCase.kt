package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
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

class YieldSupplyEstimateEnterFeeUseCase(
    private val feeRepository: FeeRepository,
    private val feeErrorResolver: FeeErrorResolver,
    private val blockAidGasEstimate: BlockAidGasEstimate,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, List<TransactionData.Uncompiled>> = Either.catch {
        if (transactionDataList.isSingleItem()) {
            transactionDataList.map { transaction ->
                transaction.copy(
                    fee = feeRepository.calculateFee(
                        userWallet = userWallet,
                        cryptoCurrency = cryptoCurrency,
                        transactionData = transaction,
                    ).normal,
                )
            }
        } else {
            val estimatedFees = estimateFeeWithBlockAid(
                userWallet = userWallet,
                cryptoCurrency = cryptoCurrency,
                transactionDataList = transactionDataList,
            )

            if (estimatedFees.isNullOrEmpty()) {
                // Fallback in case block aid returns error
                estimateFeeWithStatic(
                    userWallet = userWallet,
                    cryptoCurrency = cryptoCurrency,
                    transactionDataList = transactionDataList,
                )
            } else {
                estimatedFees
            }
        }
    }.mapLeft(feeErrorResolver::resolve)

    private suspend fun estimateFeeWithBlockAid(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): List<TransactionData.Uncompiled>? {
        val estimatedFees = blockAidGasEstimate.getGasEstimation(
            cryptoCurrency = cryptoCurrency,
            transactionDataList = transactionDataList,
        ).onLeft { TangemLogger.e("Error", it) }.getOrNull() ?: return null

        val estimatedGasList = estimatedFees.estimatedGasList
        // The estimates are matched to the transactions by position, so the list must cover every transaction: a
        // shorter list would make `zip` silently drop the tail of the batch (the `enter` call last), and the user
        // would sign deploy + approve without ever entering the protocol. An implausibly large estimate (it becomes
        // the signed gasLimit, ×1.4) is not trusted either — fall back to the static estimation in both cases.
        if (estimatedGasList.size != transactionDataList.size) {
            TangemLogger.e(
                "BlockAid returned ${estimatedGasList.size} gas estimates for ${transactionDataList.size} " +
                    "transactions; falling back to static estimation",
            )
            return null
        }
        if (estimatedGasList.any { it > MAX_GAS_LIMIT }) {
            TangemLogger.e("BlockAid gas estimate out of range: $estimatedGasList; falling back to static estimation")
            return null
        }

        val fee = feeRepository.getEthereumFeeWithoutGas(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrency,
        )

        return transactionDataList.zip(estimatedGasList) { transaction, estimatedGas ->
            transaction.copy(
                fee = fee.fixFee(cryptoCurrency, estimatedGas)
                    .increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
            )
        }
    }

    private suspend fun estimateFeeWithStatic(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): List<TransactionData.Uncompiled> {
        val withCalculatedFees = transactionDataList.filter {
            (it.extras as? EthereumTransactionExtras)?.callData !is EthereumYieldSupplyEnterCallData
        }.map { transaction ->
            transaction.copy(
                fee = feeRepository.calculateFee(
                    userWallet = userWallet,
                    cryptoCurrency = cryptoCurrency,
                    transactionData = transaction,
                ).normal,
            )
        }

        val calculatedFee = withCalculatedFees.firstOrNull()?.fee ?: error("Must be any calculated fee")

        val withEstimatedFees = transactionDataList.filter {
            (it.extras as? EthereumTransactionExtras)?.callData is EthereumYieldSupplyEnterCallData
        }.map { transaction ->
            transaction.copy(fee = calculatedFee.fixFee(cryptoCurrency, ETHEREUM_CONSTANT_GAS_LIMIT))
        }

        // First return calculated fees then estimated
        return withCalculatedFees + withEstimatedFees
    }

    private companion object {
        // Using constant gas limit to avoid fee calculation errors when contract address is not deployed yet
        val ETHEREUM_CONSTANT_GAS_LIMIT = 500_000.toBigInteger()

        /** Upper bound for a single EVM transaction's gas estimate (about a third of a block). */
        val MAX_GAS_LIMIT = 10_000_000.toBigInteger()
    }
}