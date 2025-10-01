package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.utils.extensions.isSingleItem

class YieldSupplyEstimateEnterFeeUseCase(
    private val feeRepository: FeeRepository,
    private val feeErrorResolver: FeeErrorResolver,
) {
    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, List<TransactionData.Uncompiled>> = Either.catch {
        val withCalculatedFee = transactionDataList.filter {
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

        val withEstimatedCalculatedFee = transactionDataList.filter {
            (it.extras as? EthereumTransactionExtras)?.callData is EthereumYieldSupplyEnterCallData
        }.map { transaction ->
            if (transactionDataList.isSingleItem()) {
                transaction.copy(
                    fee = feeRepository.calculateFee(
                        userWallet = userWallet,
                        cryptoCurrency = cryptoCurrency,
                        transactionData = transaction,
                    ).normal,
                )
            } else {
                transaction.copy(fee = withCalculatedFee.firstOrNull()?.fee?.fixFee(cryptoCurrency))
            }
        }

        // Transactions order must be preserved
        withCalculatedFee + withEstimatedCalculatedFee
    }.mapLeft(feeErrorResolver::resolve)

    private fun Fee.fixFee(cryptoCurrency: CryptoCurrency) = when (this) {
        is Fee.Ethereum.Legacy -> copy(
            gasLimit = ETHEREUM_CONSTANT_GAS_LIMIT,
            amount = amount.copy(
                value = gasPrice.multiply(ETHEREUM_CONSTANT_GAS_LIMIT)
                    .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
            ),
        )
        is Fee.Ethereum.EIP1559 -> copy(
            gasLimit = ETHEREUM_CONSTANT_GAS_LIMIT,
            amount = amount.copy(
                value = maxFeePerGas.multiply(ETHEREUM_CONSTANT_GAS_LIMIT)
                    .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
            ),
        )
        else -> this
    }

    private companion object {
        // Using constant gas limit to avoid fee calculation errors when contract address is not deployed yet
        val ETHEREUM_CONSTANT_GAS_LIMIT = 350_000.toBigInteger()
    }
}