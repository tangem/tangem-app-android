package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError

class YieldSupplyEstimateEnterFeeUseCase(
    private val feeRepository: FeeRepository,
    private val feeErrorResolver: FeeErrorResolver,
) {
    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, List<TransactionData.Uncompiled>> = Either.catch {
        transactionDataList.mapIndexed { index, transaction ->
            val fee = feeRepository.calculateFee(
                userWallet = userWallet,
                cryptoCurrency = cryptoCurrency,
                transactionData = transaction,
            ).normal

            if (index == transactionDataList.lastIndex) { // todo yield supply replace with check for contract
                transaction.copy(fee = fee.fixFee(cryptoCurrency))
            } else {
                transaction.copy(fee = fee)
            }
        }
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