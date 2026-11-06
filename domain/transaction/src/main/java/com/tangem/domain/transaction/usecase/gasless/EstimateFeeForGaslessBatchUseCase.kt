package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
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
import com.tangem.domain.transaction.models.YieldModuleSameTokenRule
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

/** Picks the fee for a batch of already-estimated calls: the native coin when it covers them, else a token. */
@Suppress("LongParameterList")
class EstimateFeeForGaslessBatchUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    demoConfig: DemoConfig,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    gaslessYieldRepository: GaslessYieldRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
        gaslessYieldRepository = gaslessYieldRepository,
    )

    /**
     * Right(null) — network does not support gasless; caller keeps its native fee.
     * Right(fee) with fee.isGasless == false — native coin covers the sum of call fees.
     * Right(fee) with fee.isGasless == true — token-paid; fee.callGasLimits.size == calls.size.
     *
     * @param isOperationTokenExcluded leaves the operation token out of the fee-token candidates, for callers that
     *   cannot apply the [sameTokenRule] adjustment the operation token's fee requires.
     * @param feeCurrencyStatus currency the user picked to pay the fee; null picks it automatically. A picked token
     *   that cannot pay is an error rather than a silent switch to another currency.
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        operationTokenStatus: CryptoCurrencyStatus,
        calls: List<TransactionData.Uncompiled>,
        sameTokenRule: YieldModuleSameTokenRule,
        isOperationTokenExcluded: Boolean = false,
        feeCurrencyStatus: CryptoCurrencyStatus? = null,
    ): Either<GetFeeError, TransactionFeeExtended?> = either {
        catch(
            block = {
                val network = operationTokenStatus.currency.network
                if (!currencyChecksRepository.isNetworkSupportedForGaslessTx(network)) return@either null

                val callFees = calls.map { it.fee as? Fee.Ethereum ?: raise(GetFeeError.UnknownError) }
                val summedCallFee = sumCallFees(callFees)
                val nativeFee = TransactionFee.Single(normal = summedCallFee)

                val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWallet.walletId)
                    ?: raiseIllegalStateError("AccountStatusList is null for ${userWallet.walletId}")
                val feeCurrencies = resolveNetworkFeeCurrencies(
                    nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
                    userWalletId = userWallet.walletId,
                    network = network,
                    portfolioCoinStatus = accountStatusList.getCoinStatus(network).getOrNull(),
                )
                val nativeCurrencyStatus = feeCurrencies.nativeCurrencyStatus
                val nativeResult = TransactionFeeExtended(
                    transactionFee = nativeFee,
                    feeTokenId = nativeCurrencyStatus.currency.id,
                    nativeFee = nativeFee,
                )

                if (feeCurrencyStatus?.currency is CryptoCurrency.Coin) return@either nativeResult
                val pickedToken = feeCurrencyStatus?.let { accountStatusList.freshStatusOf(it) }

                val nativeBalance = nativeCurrencyStatus.value.amount ?: BigDecimal.ZERO
                val nativeFeeValue = nativeFee.normal.amount.value ?: raise(GetFeeError.UnknownError)
                if (pickedToken == null && nativeBalance >= nativeFeeValue) return@either nativeResult

                val networkCurrenciesStatuses = pickedToken?.let(::listOf)
                    ?: networkFeeTokenCandidates(feeCurrencies.nonPortfolioFeeToken, accountStatusList, network)

                findTokenToPayFee(
                    userWallet = userWallet,
                    walletManager = prepareWalletManager(userWallet, network),
                    initialFee = summedCallFee,
                    nativeCurrencyStatus = nativeCurrencyStatus,
                    networkCurrenciesStatuses = networkCurrenciesStatuses,
                    operationTokenStatus = operationTokenStatus,
                    sameTokenRule = sameTokenRule,
                    isOperationTokenExcluded = isOperationTokenExcluded(
                        pickedToken = pickedToken,
                        operationTokenStatus = operationTokenStatus,
                        isExcludedByCaller = isOperationTokenExcluded,
                    ),
                ).map { tokenFee ->
                    tokenFee.copy(
                        // The calls carry the gas limits they are sent with natively, Yield Mode margin included:
                        // padding them again doubles the margin in the signed limits and in the fee.
                        callGasLimits = callFees.map { it.gasLimit },
                        nativeFee = nativeFee,
                    )
                }.getOrElse { error ->
                    if (pickedToken != null) raise(error)
                    when (error) {
                        GaslessError.NotEnoughFunds,
                        GaslessError.YieldBalanceUnavailable,
                        GaslessError.NoSupportedTokensFound,
                        -> nativeResult
                        else -> raise(error)
                    }
                }
            },
            catch = { raise(GaslessError.DataError(it)) },
        )
    }

    private fun AccountStatusList.freshStatusOf(status: CryptoCurrencyStatus): CryptoCurrencyStatus {
        return flattenPersonalCurrencies().firstOrNull { it.currency.id == status.currency.id } ?: status
    }

    /** A picked token is the only candidate, so the operation token stays only when it is the one picked. */
    private fun isOperationTokenExcluded(
        pickedToken: CryptoCurrencyStatus?,
        operationTokenStatus: CryptoCurrencyStatus,
        isExcludedByCaller: Boolean,
    ): Boolean {
        if (pickedToken == null) return isExcludedByCaller
        return pickedToken.currency.id != operationTokenStatus.currency.id
    }

    private fun networkFeeTokenCandidates(
        nonPortfolioFeeToken: CryptoCurrencyStatus?,
        accountStatusList: AccountStatusList,
        network: Network,
    ): List<CryptoCurrencyStatus> {
        return nonPortfolioFeeToken?.let(::listOf)
            ?: accountStatusList.flattenPersonalCurrencies().filter { it.currency.network.id == network.id }
    }

    /**
     * The calls may be estimated one by one and so carry different gas prices: the token fee is priced from the
     * summed gas at the highest of them, so that it caps every call, while the amount stays the sum of call amounts.
     */
    private fun Raise<GetFeeError>.sumCallFees(callFees: List<Fee.Ethereum>): Fee.Ethereum {
        val first = callFees.firstOrNull() ?: raise(GetFeeError.UnknownError)
        if (callFees.any { it::class != first::class }) raise(GetFeeError.UnknownError)

        val gasLimit = callFees.sumOf { it.gasLimit }
        val amount = first.amount.copy(value = callFees.sumOf { it.amount.value ?: raise(GetFeeError.UnknownError) })
        return when (first) {
            is Fee.Ethereum.EIP1559 -> {
                val eip1559Fees = callFees.filterIsInstance<Fee.Ethereum.EIP1559>()
                first.copy(
                    amount = amount,
                    gasLimit = gasLimit,
                    maxFeePerGas = eip1559Fees.maxOf { it.maxFeePerGas },
                    priorityFee = eip1559Fees.maxOf { it.priorityFee },
                )
            }
            is Fee.Ethereum.Legacy -> first.copy(
                amount = amount,
                gasLimit = gasLimit,
                gasPrice = callFees.filterIsInstance<Fee.Ethereum.Legacy>().maxOf { it.gasPrice },
            )
            is Fee.Ethereum.TokenCurrency -> raise(GetFeeError.UnknownError)
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
        return walletManager as? EthereumWalletManager
            ?: raiseIllegalStateError("WalletManager type ${walletManager?.javaClass?.name} not supported")
    }

    @Suppress("LongParameterList")
    private suspend fun findTokenToPayFee(
        userWallet: UserWallet,
        walletManager: EthereumWalletManager,
        initialFee: Fee.Ethereum,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        networkCurrenciesStatuses: List<CryptoCurrencyStatus>,
        operationTokenStatus: CryptoCurrencyStatus,
        sameTokenRule: YieldModuleSameTokenRule,
        isOperationTokenExcluded: Boolean,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val operationToken = operationTokenStatus.currency as? CryptoCurrency.Token
            ?: raiseIllegalStateError("gasless batch operation currency must be a token")

        val supportedGaslessTokens = gaslessTransactionRepository.getSupportedTokens(
            network = nativeCurrencyStatus.currency.network,
        ).mapNotNull { currency ->
            (currency as? CryptoCurrency.Token)?.contractAddress?.lowercase()
        }.toSet()

        val otherCandidates = networkCurrenciesStatuses.filterNot { it.isTokenOf(operationToken) }
        val candidates = if (isOperationTokenExcluded) otherCandidates else otherCandidates + operationTokenStatus
        val rankedCandidates = candidates
            .asSequence()
            .filter { it.currency is CryptoCurrency.Token }
            .filter { (it.currency as CryptoCurrency.Token).contractAddress.lowercase() in supportedGaslessTokens }
            .filter { status ->
                val total = status.value.amount ?: BigDecimal.ZERO
                total > BigDecimal.ZERO || status.value.yieldSupplyStatus?.isActive == true
            }
            .sortedByDescending { status -> status.value.amount ?: BigDecimal.ZERO }
            .toList()
        val tokenForPayFeeStatus = rankedCandidates.firstOrNull() ?: raise(GaslessError.NoSupportedTokensFound)

        if (!tokenForPayFeeStatus.isTokenOf(operationToken)) {
            return@either otherTokenFee(
                userWallet = userWallet,
                walletManager = walletManager,
                initialFee = initialFee,
                nativeCurrencyStatus = nativeCurrencyStatus,
                tokenForPayFeeStatus = tokenForPayFeeStatus,
            )
        }

        val operationTokenFee = either {
            operationTokenFee(
                userWallet = userWallet,
                walletManager = walletManager,
                initialFee = initialFee,
                nativeCurrencyStatus = nativeCurrencyStatus,
                operationTokenStatus = operationTokenStatus,
                sameTokenRule = sameTokenRule,
            )
        }
        operationTokenFee.getOrElse { error ->
            // The operation token ranks by its total, which may sit in a module and not pay; native is already
            // short here, so a smaller token that can pay beats falling back to it.
            val nextStatus = rankedCandidates.firstOrNull { !it.isTokenOf(operationToken) }
            if (error != GaslessError.NotEnoughFunds || nextStatus == null) raise(error)

            otherTokenFee(
                userWallet = userWallet,
                walletManager = walletManager,
                initialFee = initialFee,
                nativeCurrencyStatus = nativeCurrencyStatus,
                tokenForPayFeeStatus = nextStatus,
            )
        }
    }

    @Suppress("LongParameterList")
    private suspend fun Raise<GetFeeError>.operationTokenFee(
        userWallet: UserWallet,
        walletManager: EthereumWalletManager,
        initialFee: Fee.Ethereum,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        operationTokenStatus: CryptoCurrencyStatus,
        sameTokenRule: YieldModuleSameTokenRule,
    ): TransactionFeeExtended {
        val operationToken = operationTokenStatus.currency as? CryptoCurrency.Token
            ?: raiseIllegalStateError("gasless batch operation currency must be a token")

        return when (sameTokenRule) {
            YieldModuleSameTokenRule.Enter -> {
                val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
                    walletManager = walletManager,
                    tokenForPayFeeStatus = operationTokenStatus,
                    nativeCurrencyStatus = nativeCurrencyStatus,
                    initialFee = initialFee,
                    userWallet = userWallet,
                    isCallGasLimitFinal = true,
                ).bind()
                attachGaslessFeePlan(
                    resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
                    userWallet = userWallet,
                    tokenStatus = operationTokenStatus,
                    tokenFeeExtended = tokenFeeExtended,
                    // At least one minimal unit must stay liquid to be entered after the fee.
                    sendAmountInFeeToken = BigDecimal.ONE.movePointLeft(operationToken.decimals),
                    isYieldActive = false,
                )
            }
            YieldModuleSameTokenRule.Exit -> {
                val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
                    walletManager = walletManager,
                    tokenForPayFeeStatus = operationTokenStatus,
                    nativeCurrencyStatus = nativeCurrencyStatus,
                    initialFee = initialFee,
                    userWallet = userWallet,
                    // The fee is taken after exit has moved the module balance to the EOA, so the liquid balance
                    // at estimation time (often zero, reverting the fee-transfer probe) does not limit it.
                    isLiquidBalanceCheckSkipped = true,
                    isCallGasLimitFinal = true,
                ).bind()
                val tokenFee = tokenFeeExtended.transactionFee.normal as? Fee.Ethereum.TokenCurrency
                    ?: raiseIllegalStateError("gasless token fee must be Fee.Ethereum.TokenCurrency")
                val feeAmount = tokenFee.amount.value ?: raiseIllegalStateError("token fee amount is null")
                val totalBalance = operationTokenStatus.value.amount ?: BigDecimal.ZERO
                if (totalBalance < feeAmount) raise(GaslessError.NotEnoughFunds)

                val plan = GaslessFeePlan.TokenPay(feeToken = operationToken, fee = tokenFee)
                tokenFeeExtended.copy(gaslessFeePlan = plan)
            }
        }
    }

    private suspend fun Raise<GetFeeError>.otherTokenFee(
        userWallet: UserWallet,
        walletManager: EthereumWalletManager,
        initialFee: Fee.Ethereum,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        tokenForPayFeeStatus: CryptoCurrencyStatus,
    ): TransactionFeeExtended {
        val isYieldActive = tokenForPayFeeStatus.value.yieldSupplyStatus?.isActive == true
        val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenForPayFeeStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
            isYieldActive = isYieldActive,
            userWallet = userWallet,
            isCallGasLimitFinal = true,
        ).bind()

        if (!isYieldActive) return tokenFeeExtended

        return attachGaslessFeePlan(
            resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
            userWallet = userWallet,
            tokenStatus = tokenForPayFeeStatus,
            tokenFeeExtended = tokenFeeExtended,
            sendAmountInFeeToken = BigDecimal.ZERO,
            isYieldActive = true,
        )
    }

    private fun CryptoCurrencyStatus.isTokenOf(token: CryptoCurrency.Token): Boolean {
        val contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: return false
        return contractAddress.equals(token.contractAddress, ignoreCase = true)
    }
}