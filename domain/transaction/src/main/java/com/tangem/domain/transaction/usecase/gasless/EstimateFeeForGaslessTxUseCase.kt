package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

@Suppress("LongParameterList")
class EstimateFeeForGaslessTxUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
    )

    suspend operator fun invoke(
        userWallet: UserWallet,
        amount: BigDecimal,
        sendingTokenCurrencyStatus: CryptoCurrencyStatus,
    ): Either<GetFeeError, TransactionFeeExtended> {
        return either {
            catch(
                block = {
                    val network = sendingTokenCurrencyStatus.currency.network

                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                        ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")

                    val nativeCurrencyStatus = accountStatusList.getCoinStatus(network).getOrElse {
                        raiseIllegalStateError("No native currency found: ${network.id}")
                    }

                    if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) {
                        estimateFeeUseCase.invoke(
                            userWallet = userWallet,
                            amount = amount,
                            cryptoCurrencyStatus = sendingTokenCurrencyStatus,
                        ).fold(
                            ifLeft = { raise(it) },
                            ifRight = { fee ->
                                return@either TransactionFeeExtended(
                                    transactionFee = fee,
                                    feeTokenId = nativeCurrencyStatus.currency.id,
                                )
                            },
                        )
                    }

                    val walletManager = prepareWalletManager(userWallet, network)

                    val initialFee = tokenFeeCalculator.estimateInitialFee(
                        userWallet = userWallet,
                        amount = amount,
                        txTokenCurrencyStatus = sendingTokenCurrencyStatus,
                    ).bind()

                    selectFeePaymentStrategy(
                        accountStatusList = accountStatusList,
                        walletManager = walletManager,
                        nativeCurrencyStatus = nativeCurrencyStatus,
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
        accountStatusList: AccountStatusList,
        walletManager: EthereumWalletManager,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        network: Network,
        initialFee: TransactionFee,
    ): TransactionFeeExtended {
        val feeValue = initialFee.normal.amount.value ?: raise(GetFeeError.UnknownError)

        val networkCurrenciesStatuses = accountStatusList
            .flattenCurrencies()
            .filter { it.currency.network.id == network.id }

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