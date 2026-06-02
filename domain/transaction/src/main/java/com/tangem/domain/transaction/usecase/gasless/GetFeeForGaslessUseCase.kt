package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
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
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

@Suppress("LongParameterList")
class GetFeeForGaslessUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getFeeUseCase: GetFeeUseCase,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
    private val isYieldWithdrawEnabled: Boolean,
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
                    val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                        ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")

                    val nativeCurrencyStatus = accountStatusList.getCoinStatus(network).getOrElse {
                        raiseIllegalStateError("No native currency found: ${network.id}")
                    }

                    if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) {
                        getFeeUseCase.invoke(userWallet, network, transactionData).fold(
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

                    val initialFee = tokenFeeCalculator.calculateInitialFee(
                        userWallet = userWallet,
                        network = network,
                        walletManager = walletManager,
                        transactionData = transactionData,
                    ).bind()

                    selectFeePaymentStrategy(
                        userWallet = userWallet,
                        accountStatusList = accountStatusList,
                        walletManager = walletManager,
                        nativeCurrencyStatus = nativeCurrencyStatus,
                        network = network,
                        initialFee = initialFee,
                        transactionData = transactionData,
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
        transactionData: TransactionData,
    ): TransactionFeeExtended {
        val feeValue = initialFee.normal.amount.value ?: raise(GetFeeError.UnknownError)

        val networkCurrenciesStatuses = accountStatusList
            .flattenCurrencies()
            .filter { it.currency.network.id == network.id }

        val nativeBalance = nativeCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val nativeCoinSelectedResult =
            TransactionFeeExtended(transactionFee = initialFee, feeTokenId = nativeCurrencyStatus.currency.id)
        return if (nativeBalance >= feeValue) {
            nativeCoinSelectedResult
        } else {
            findTokensToPayFee(
                userWallet = userWallet,
                walletManager = walletManager,
                initialTxFee = initialFee,
                nativeCurrencyStatus = nativeCurrencyStatus,
                networkCurrenciesStatuses = networkCurrenciesStatuses,
                transactionData = transactionData,
            ).getOrElse { error ->
                when (error) {
                    GaslessError.NotEnoughFunds -> nativeCoinSelectedResult
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
        transactionData: TransactionData,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val initialFee = initialTxFee.normal as? Fee.Ethereum
            ?: raiseIllegalStateError(
                error = "only Fee.Ethereum supported, but was ${initialTxFee.normal::class.qualifiedName}",
            )

        val supportedGaslessTokens = gaslessTransactionRepository.getSupportedTokens(
            network = nativeCurrencyStatus.currency.network,
        ).mapNotNull { currency ->
            (currency as? CryptoCurrency.Token)?.contractAddress
        }.toSet()

        /**
         * Yield-aware candidate selection:
         * a token is eligible if it is a supported gasless token AND
         * (plain balance > 0 OR has an active yield position).
         * Sorted by (plain + effectiveProtocolBalance) descending to maximise chances of covering the fee.
         */
        val candidates = networkCurrenciesStatuses
            .filter { it.currency is CryptoCurrency.Token }
            .filter { (it.currency as CryptoCurrency.Token).contractAddress.lowercase() in supportedGaslessTokens }
            .filter { status ->
                val plain = status.value.amount ?: BigDecimal.ZERO
                plain > BigDecimal.ZERO || isYieldWithdrawEnabled && status.value.yieldSupplyStatus?.isActive == true
            }
            .sortedByDescending { status ->
                val plain = status.value.amount ?: BigDecimal.ZERO
                val yieldBal = status.value.yieldSupplyStatus?.effectiveProtocolBalance ?: BigDecimal.ZERO
                plain + yieldBal
            }

        val tokenForPayFeeStatus = candidates.firstOrNull() ?: raise(GaslessError.NoSupportedTokensFound)

        val isYieldActive = isYieldWithdrawEnabled && tokenForPayFeeStatus.value.yieldSupplyStatus?.isActive == true
        val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenForPayFeeStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
            isYieldActive = isYieldActive,
        ).bind()

        val feeInTokenCurrency = tokenFeeExtended.transactionFee.normal as? Fee.Ethereum.TokenCurrency
            ?: raiseIllegalStateError("gasless token fee must be Fee.Ethereum.TokenCurrency")
        val feeTokenContract = (tokenForPayFeeStatus.currency as CryptoCurrency.Token).contractAddress
        val sendAmountInFeeToken = computeSendAmountInFeeToken(transactionData, feeTokenContract)

        val plan = resolveGaslessFeePlanUseCase(
            userWallet = userWallet,
            tokenStatus = tokenForPayFeeStatus,
            tokenFee = feeInTokenCurrency,
            isYieldActive = isYieldActive,
            sendAmountInFeeToken = sendAmountInFeeToken,
        ).bind()

        tokenFeeExtended.copy(gaslessFeePlan = plan)
    }
}

/**
 * Computes how much of the fee token is also being spent in the main transaction body.
 *
 * Gasless token-fee transactions MUST supply uncompiled data (the resolver needs the raw amount to
 * account for it in the required-balance check). A compiled tx or a null sent amount on the
 * matching-token path are both programmer errors, so they raise loudly instead of silently
 * under-accounting as ZERO.
 *
 * @param transactionData the raw transaction data passed into [GetFeeForGaslessUseCase].
 * @param feeTokenContract the contract address of the token selected to pay the gasless fee.
 * @return the sent amount when [feeTokenContract] matches the sent-token contract,
 *         or [BigDecimal.ZERO] when a different token is being sent.
 */
internal fun Raise<GetFeeError>.computeSendAmountInFeeToken(
    transactionData: TransactionData,
    feeTokenContract: String,
): BigDecimal {
    // Gasless token-fee requires uncompiled tx data (mirrors CreateAndSendGaslessTransactionUseCase).
    val uncompiled = transactionData as? TransactionData.Uncompiled
        ?: raiseIllegalStateError("gasless token fee requires uncompiled transaction data")
    val sentTokenContract = when (val type = uncompiled.amount.type) {
        is AmountType.Token -> type.token.contractAddress
        is AmountType.TokenYieldSupply -> type.token.contractAddress
        else -> null
    }
    return if (sentTokenContract != null && sentTokenContract.equals(feeTokenContract, ignoreCase = true)) {
        uncompiled.amount.value
            ?: raiseIllegalStateError("sent amount is null while paying the gasless fee in the sent token")
    } else {
        BigDecimal.ZERO
    }
}