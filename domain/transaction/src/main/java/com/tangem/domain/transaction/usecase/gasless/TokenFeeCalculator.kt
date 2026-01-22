package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal class TokenFeeCalculator(
    private val walletManagersFacade: WalletManagersFacade,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val demoConfig: DemoConfig,
) {

    suspend fun calculateInitialFee(
        userWallet: UserWallet,
        network: Network,
        walletManager: EthereumWalletManager,
        transactionData: TransactionData,
    ): Either<GetFeeError, TransactionFee> {
        return either {
            val transactionSender = if (userWallet is UserWallet.Cold &&
                demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
            ) {
                demoTransactionSender(userWallet, network)
            } else {
                walletManager
            }

            val maybeFee = when (val result = transactionSender.getFee(transactionData = transactionData)) {
                is Result.Success -> result.data
                is Result.Failure -> raise(GaslessError.DataError(result.error))
            }
            maybeFee
        }
    }

    suspend fun estimateInitialFee(
        userWallet: UserWallet,
        amount: BigDecimal,
        txTokenCurrencyStatus: CryptoCurrencyStatus,
    ): Either<GetFeeError, TransactionFee> {
        return either {
            val network = txTokenCurrencyStatus.currency.network
            val amountData = amount.convertToSdkAmount(txTokenCurrencyStatus)
            val result = if (userWallet is UserWallet.Cold &&
                demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
            ) {
                demoTransactionSender(userWallet, network).estimateFee(
                    amount = amountData,
                    destination = "",
                )
            } else {
                walletManagersFacade.estimateFee(
                    amount = amountData,
                    userWalletId = userWallet.walletId,
                    network = network,
                )
            }

            when (result) {
                is Result.Success -> result.data
                is Result.Failure -> raise(GaslessError.DataError(result.error))
                null -> raise(GetFeeError.UnknownError)
            }
        }
    }

    suspend fun calculateTokenFee(
        walletManager: EthereumWalletManager,
        tokenForPayFeeStatus: CryptoCurrencyStatus,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        initialFee: Fee.Ethereum,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            // fast finish to skip calculations if no funds in token
            if (tokenForPayFeeStatus.value.amount?.isZero() == true) {
                raise(GaslessError.NotEnoughFunds)
            }

            val tokenForPayFee = tokenForPayFeeStatus.currency as? CryptoCurrency.Token
                ?: raiseIllegalStateError("only tokens are supported")

            val transferFeeToContractAmount = createTokenAmount(
                token = tokenForPayFee,
                value = BigDecimal(FEE_TRANSFER_AMOUNT),
            )
            val feeDestination = gaslessTransactionRepository.getTokenFeeReceiverAddress()
            val feeTransferGasLimitResult = walletManager.getGasLimit(
                amount = transferFeeToContractAmount,
                destination = feeDestination,
                callData = TransferERC20TokenCallData(
                    destination = feeDestination,
                    amount = transferFeeToContractAmount,
                ),
            )

            val feeTransferGasLimit = when (feeTransferGasLimitResult) {
                is Result.Failure -> raise(GaslessError.DataError(feeTransferGasLimitResult.error))
                is Result.Success -> feeTransferGasLimitResult.data
            }.increaseByPercent(PERCENT_TO_INCREASE_TRANSFER_GASLIMIT)

            val baseGas = gaslessTransactionRepository.getBaseGasForTransaction()

            val maxTokenFeeGas = initialFee.gasLimit + feeTransferGasLimit + baseGas

            val maxFeePerGas = when (initialFee) {
                is Fee.Ethereum.EIP1559 -> initialFee.maxFeePerGas
                is Fee.Ethereum.Legacy -> initialFee.gasPrice
                is Fee.Ethereum.TokenCurrency -> raiseIllegalStateError("initialFee could only be native")
            }

            val feeInNativeCurrency = maxTokenFeeGas
                .multiply(maxFeePerGas)
                .multiply(GAS_PRICE_MULTIPLIER.toBigInteger())

            val nativeFiatRate = nativeCurrencyStatus.value.fiatRate ?: raiseIllegalStateError("fiatRate is null")
            val tokenFiatRate = tokenForPayFeeStatus.value.fiatRate ?: raiseIllegalStateError("fiatRate is null")

            val coinPriceInTokenInBigDecimal = nativeFiatRate.divide(
                tokenFiatRate,
                maxOf(nativeCurrencyStatus.currency.decimals, tokenForPayFee.decimals),
                RoundingMode.UP,
            )

            val coinPriceInTokenBigInt = coinPriceInTokenInBigDecimal
                .movePointRight(tokenForPayFee.decimals)
                .increaseByPercent(PERCENT_TO_INCREASE_TOKEN_PRICE)
                .toBigInteger()

            val feeInTokenCurrency = coinPriceInTokenInBigDecimal.multiply(
                feeInNativeCurrency
                    .toBigDecimal()
                    .movePointLeft(nativeCurrencyStatus.currency.decimals),
            )

            val tokenBalance = tokenForPayFeeStatus.value.amount ?: BigDecimal.ZERO
            if (tokenBalance < feeInTokenCurrency) {
                raise(GaslessError.NotEnoughFunds)
            }

            val amount = createTokenAmount(tokenForPayFee, feeInTokenCurrency)

            val fee = Fee.Ethereum.TokenCurrency(
                amount = amount,
                gasLimit = maxTokenFeeGas,
                coinPriceInToken = coinPriceInTokenBigInt,
                feeTransferGasLimit = feeTransferGasLimit,
                baseGas = baseGas,
            )
            TransactionFeeExtended(
                transactionFee = TransactionFee.Single(normal = fee),
                feeTokenId = tokenForPayFee.id,
            )
        }
    }

    private fun createTokenAmount(token: CryptoCurrency.Token, value: BigDecimal): Amount = Amount(
        token = Token(
            symbol = token.symbol,
            contractAddress = token.contractAddress,
            decimals = token.decimals,
        ),
        value = value,
    )

    private suspend fun Raise<GetFeeError>.demoTransactionSender(
        userWallet: UserWallet,
        network: Network,
    ): DemoTransactionSender {
        return DemoTransactionSender(
            walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
                ?: raiseIllegalStateError("WalletManager is null"),
        )
    }

    private companion object {
        /** Amount in token units for fee transfer */
        const val FEE_TRANSFER_AMOUNT = 0.01 // calculate using decimals
        /** Gas price safety multiplier for fee calculation */
        const val GAS_PRICE_MULTIPLIER = 2
        const val PERCENT_TO_INCREASE_TOKEN_PRICE = 1
        const val PERCENT_TO_INCREASE_TRANSFER_GASLIMIT = 10

        /**
         * Increases BigDecimal value by specified percentage.
         *
         * @param percent percentage to increase by (e.g., 1 for 1%, 10 for 10%)
         * @return value increased by specified percentage (value * (1 + percent/100))
         *
         * Example:
         * ```
         * BigDecimal("100").increaseByPercent(1)  // 101
         * BigDecimal("100").increaseByPercent(10) // 110
         * BigDecimal("100").increaseByPercent(50) // 150
         * ```
         */
        private fun BigDecimal.increaseByPercent(percent: Int): BigDecimal {
            require(percent >= 0) { "Percent must be non-negative" }
            val multiplier = BigDecimal.ONE.add(BigDecimal(percent).divide(BigDecimal("100")))
            return this.multiply(multiplier)
        }

        private fun BigInteger.increaseByPercent(percent: Int): BigInteger {
            require(percent >= 0) { "Percent must be non-negative" }
            val multiplier = BigDecimal.ONE.add(BigDecimal(percent).divide(BigDecimal("100")))
            return BigDecimal(this).multiply(multiplier).toBigInteger()
        }
    }
}