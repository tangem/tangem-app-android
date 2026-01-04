package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
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
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.error.mapToFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal
import java.math.RoundingMode

class GetFeeForGaslessUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    private val getFeeUseCase: GetFeeUseCase,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            catch(
                block = {
                    val nativeCurrency = currenciesRepository.getNetworkCoin(
                        userWalletId = userWallet.walletId,
                        networkId = network.id,
                        derivationPath = network.derivationPath,
                    )

                    if (!gaslessTransactionRepository.isNetworkSupported(network)) {
                        getFeeUseCase.invoke(userWallet, network, transactionData).fold(
                            ifLeft = { raise(it) },
                            ifRight = { fee ->
                                return@either TransactionFeeExtended(
                                    transactionFee = fee,
                                    feeTokenId = nativeCurrency.id,
                                )
                            },
                        )
                    }

                    val walletManager = prepareWalletManager(userWallet, network)

                    val initialFee = calculateInitialFee(
                        userWallet = userWallet,
                        network = network,
                        walletManager = walletManager,
                        transactionData = transactionData,
                    )

                    selectFeePaymentStrategy(
                        userWallet = userWallet,
                        walletManager = walletManager,
                        nativeCurrency = nativeCurrency,
                        network = network,
                        initialFee = initialFee,
                    )
                },
                catch = {
                    raise(GetFeeError.DataError(it))
                },
            )
        }
    }

    @Suppress("NullableToStringCall")
    private suspend fun Raise<GetFeeError>.prepareWalletManager(
        userWallet: UserWallet,
        network: Network,
    ): EthereumWalletManager {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWallet.walletId,
            network = network,
        )
        val ethereumWalletManager = walletManager as? EthereumWalletManager
            ?: raiseIllegalStateError("WalletManager type ${walletManager?.javaClass?.name} not supported")
        return ethereumWalletManager
    }

    private suspend fun Raise<GetFeeError>.calculateInitialFee(
        userWallet: UserWallet,
        network: Network,
        walletManager: EthereumWalletManager,
        transactionData: TransactionData,
    ): TransactionFee {
        val transactionSender = if (userWallet is UserWallet.Cold &&
            demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
        ) {
            demoTransactionSender(userWallet, network)
        } else {
            walletManager
        }

        val maybeFee = when (val result = transactionSender.getFee(transactionData = transactionData)) {
            is Result.Success -> result.data
            is Result.Failure -> raise(result.mapToFeeError())
        }
        return maybeFee
    }

    private suspend fun Raise<GetFeeError>.selectFeePaymentStrategy(
        userWallet: UserWallet,
        walletManager: EthereumWalletManager,
        nativeCurrency: CryptoCurrency,
        network: Network,
        initialFee: TransactionFee,
    ): TransactionFeeExtended {
        val feeValue = initialFee.normal.amount.value ?: raise(GetFeeError.UnknownError)

        val userCurrenciesStatuses = getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
            userWallet.walletId,
        ).getOrNull() ?: raiseIllegalStateError("currencies list is null for userWalletId=${userWallet.walletId}")

        val networkCurrenciesStatuses = userCurrenciesStatuses.filter {
            it.currency.network.id == network.id
        }

        val nativeCurrencyStatus = networkCurrenciesStatuses.find {
            it.currency.id == nativeCurrency.id
        } ?: raiseIllegalStateError("native currency not found for network ${network.id}")

        val nativeBalance = nativeCurrencyStatus.value.amount ?: BigDecimal.ZERO
        return if (nativeBalance >= feeValue) {
            TransactionFeeExtended(transactionFee = initialFee, feeTokenId = nativeCurrencyStatus.currency.id)
        } else {
            findTokensToPayFee(
                walletManager = walletManager,
                initialTxFee = initialFee,
                nativeCurrencyStatus = nativeCurrencyStatus,
                networkCurrenciesStatuses = networkCurrenciesStatuses,
            )
        }
    }

    @Suppress("NullableToStringCall")
    private suspend fun Raise<GetFeeError>.findTokensToPayFee(
        walletManager: EthereumWalletManager,
        initialTxFee: TransactionFee,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        networkCurrenciesStatuses: List<CryptoCurrencyStatus>,
    ): TransactionFeeExtended {
        val initialFee = initialTxFee.normal as? Fee.Ethereum
            ?: raiseIllegalStateError(
                error = "only Fee.Ethereum supported, but was ${initialTxFee.normal::class.qualifiedName}",
            )

        val supportedGaslessTokens = gaslessTransactionRepository.getSupportedTokens()
        val supportedGaslessTokensStatusesSortedByBalanceDesc = networkCurrenciesStatuses
            .filterNot { it.value.amount == BigDecimal.ZERO || it.currency !is CryptoCurrency.Token }
            .sortedByDescending { it.value.amount }
            .filter { it.currency in supportedGaslessTokens }

        /**
         * Selects token with highest balance to maximize chances of successful fee payment.
         * Returns null if no suitable tokens found.
         */
        val tokenForPayFeeStatus = supportedGaslessTokensStatusesSortedByBalanceDesc.firstOrNull()
            ?: raise(GaslessError.NoSupportedTokensFound)

        return calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenForPayFeeStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
        )
    }

    private suspend fun Raise<GetFeeError>.calculateTokenFee(
        walletManager: EthereumWalletManager,
        tokenForPayFeeStatus: CryptoCurrencyStatus,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        initialFee: Fee.Ethereum,
    ): TransactionFeeExtended {
        val tokenForPayFee = tokenForPayFeeStatus.currency as? CryptoCurrency.Token
            ?: raiseIllegalStateError("only tokens are supported")

        val transferFeeToContractAmount = createTokenAmount(tokenForPayFee, BigDecimal(FEE_TRANSFER_AMOUNT))
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
            is Result.Failure -> raise(GetFeeError.DataError(feeTransferGasLimitResult.error))
            is Result.Success -> feeTransferGasLimitResult.data
        }

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
        val coinPriceInToken = nativeFiatRate.divide(
            tokenFiatRate,
            maxOf(nativeCurrencyStatus.currency.decimals, tokenForPayFee.decimals),
            RoundingMode.DOWN,
        )

        val feeInTokenCurrency = coinPriceInToken.multiply(feeInNativeCurrency.toBigDecimal())

        val tokenBalance = tokenForPayFeeStatus.value.amount ?: BigDecimal.ZERO
        if (tokenBalance < feeInTokenCurrency) {
            raise(GaslessError.NotEnoughFunds)
        }

        val amount = createTokenAmount(tokenForPayFee, feeInTokenCurrency)

        val fee = Fee.Ethereum.TokenCurrency(
            amount = amount,
            gasLimit = maxTokenFeeGas,
            coinPriceInToken = coinPriceInToken,
            feeTransferGasLimit = feeTransferGasLimit,
            baseGas = baseGas,
        )
        return TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = fee),
            feeTokenId = tokenForPayFee.id,
        )
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

    private fun Raise<GetFeeError>.raiseIllegalStateError(error: String): Nothing {
        raise(GetFeeError.DataError(IllegalStateException(error)))
    }

    private companion object {
        /** Amount in token units for fee transfer */
        const val FEE_TRANSFER_AMOUNT = 10000
        /** Gas price safety multiplier for fee calculation */
        const val GAS_PRICE_MULTIPLIER = 2
    }
}