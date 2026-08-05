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
import com.tangem.domain.transaction.GaslessYieldRepository
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
    private val gaslessYieldRepository: GaslessYieldRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
    private val isYieldWithdrawEnabled: Boolean,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
        gaslessYieldRepository = gaslessYieldRepository,
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
                                    nativeFee = fee,
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
                        userWallet = userWallet,
                        accountStatusList = accountStatusList,
                        walletManager = walletManager,
                        nativeCurrencyStatus = nativeCurrencyStatus,
                        network = network,
                        initialFee = initialFee,
                        sendingTokenCurrencyStatus = sendingTokenCurrencyStatus,
                        amount = amount,
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

    @Suppress("LongParameterList")
    private suspend fun Raise<GetFeeError>.selectFeePaymentStrategy(
        userWallet: UserWallet,
        accountStatusList: AccountStatusList,
        walletManager: EthereumWalletManager,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        network: Network,
        initialFee: TransactionFee,
        sendingTokenCurrencyStatus: CryptoCurrencyStatus,
        amount: BigDecimal,
    ): TransactionFeeExtended {
        val feeValue = initialFee.normal.amount.value ?: raise(GetFeeError.UnknownError)

        val networkCurrenciesStatuses = accountStatusList
            .flattenCurrencies()
            .filter { it.currency.network.id == network.id }

        val nativeBalance = nativeCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val nativeCoinSelectedResult = TransactionFeeExtended(
            transactionFee = initialFee,
            feeTokenId = nativeCurrencyStatus.currency.id,
            nativeFee = initialFee,
        )
        return if (nativeBalance >= feeValue) {
            nativeCoinSelectedResult
        } else {
            findTokensToPayFee(
                userWallet = userWallet,
                walletManager = walletManager,
                initialTxFee = initialFee,
                nativeCurrencyStatus = nativeCurrencyStatus,
                networkCurrenciesStatuses = networkCurrenciesStatuses,
                sendingTokenCurrencyStatus = sendingTokenCurrencyStatus,
                amount = amount,
            ).map { it.copy(nativeFee = initialFee) }.getOrElse { error ->
                when (error) {
                    GaslessError.NotEnoughFunds,
                    GaslessError.YieldBalanceUnavailable,
                    -> nativeCoinSelectedResult
                    else -> raise(error)
                }
            }
        }
    }

    @Suppress("NullableToStringCall", "LongParameterList")
    private suspend fun findTokensToPayFee(
        userWallet: UserWallet,
        walletManager: EthereumWalletManager,
        initialTxFee: TransactionFee,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        networkCurrenciesStatuses: List<CryptoCurrencyStatus>,
        sendingTokenCurrencyStatus: CryptoCurrencyStatus,
        amount: BigDecimal,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val initialFee = initialTxFee.normal as? Fee.Ethereum
            ?: raiseIllegalStateError(
                error = "only Fee.Ethereum supported, but was ${initialTxFee.normal::class.qualifiedName}",
            )

        val supportedGaslessTokens = gaslessTransactionRepository.getSupportedTokens(
            network = nativeCurrencyStatus.currency.network,
        ).mapNotNull { currency ->
            (currency as? CryptoCurrency.Token)?.contractAddress?.lowercase()
        }.toSet()

        val tokenForPayFeeStatus = networkCurrenciesStatuses
            .asSequence()
            .filter { it.currency is CryptoCurrency.Token }
            .filter { (it.currency as CryptoCurrency.Token).contractAddress.lowercase() in supportedGaslessTokens }
            .filter { status ->
                val total = status.value.amount ?: BigDecimal.ZERO
                total > BigDecimal.ZERO || isYieldWithdrawEnabled && status.value.yieldSupplyStatus?.isActive == true
            }.maxByOrNull { status -> status.value.amount ?: BigDecimal.ZERO }
            ?: raise(GaslessError.NoSupportedTokensFound)

        val isYieldActive = isYieldWithdrawEnabled && tokenForPayFeeStatus.value.yieldSupplyStatus?.isActive == true
        val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenForPayFeeStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
            isYieldActive = isYieldActive,
            userWallet = userWallet,
        ).bind()

        if (!isYieldActive) return@either tokenFeeExtended

        val feeTokenContract = (tokenForPayFeeStatus.currency as? CryptoCurrency.Token)?.contractAddress
            ?: raiseIllegalStateError("gasless fee currency must be a token")

        attachGaslessFeePlan(
            resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
            userWallet = userWallet,
            tokenStatus = tokenForPayFeeStatus,
            tokenFeeExtended = tokenFeeExtended,
            sendAmountInFeeToken = computeSendAmountInFeeToken(
                sendingCurrency = sendingTokenCurrencyStatus.currency,
                feeTokenContract = feeTokenContract,
                amount = amount,
            ),
            isYieldActive = true,
        )
    }
}