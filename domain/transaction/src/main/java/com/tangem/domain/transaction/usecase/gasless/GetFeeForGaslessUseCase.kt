package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

@Suppress("LongParameterList")
class GetFeeForGaslessUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
    )

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

                    if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) {
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

                    val initialFee = tokenFeeCalculator.calculateInitialFee(
                        userWallet = userWallet,
                        network = network,
                        walletManager = walletManager,
                        transactionData = transactionData,
                    ).bind()

                    selectFeePaymentStrategy(
                        userWallet = userWallet,
                        walletManager = walletManager,
                        nativeCurrency = nativeCurrency,
                        network = network,
                        initialFee = initialFee,
                    )
                },
                catch = {
                    raise(GaslessError.DataError(it))
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

        val supportedGaslessTokens = gaslessTransactionRepository.getSupportedTokens(
            network = nativeCurrencyStatus.currency.network,
        ).mapNotNull { currency ->
            (currency as? CryptoCurrency.Token)?.contractAddress
        }.toSet()

        val supportedGaslessTokensStatusesSortedByBalanceDesc = networkCurrenciesStatuses
            .filterNot { it.value.amount == BigDecimal.ZERO || it.currency !is CryptoCurrency.Token }
            .sortedByDescending { it.value.amount }
            .filter { status ->
                val token = status.currency as? CryptoCurrency.Token ?: return@filter false
                token.contractAddress.lowercase() in supportedGaslessTokens
            }

        /**
         * Selects token with highest balance to maximize chances of successful fee payment.
         * Returns null if no suitable tokens found.
         */
        val tokenForPayFeeStatus = supportedGaslessTokensStatusesSortedByBalanceDesc.firstOrNull()
            ?: raise(GaslessError.NoSupportedTokensFound)

        return tokenFeeCalculator.calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenForPayFeeStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
        ).bind()
    }
}