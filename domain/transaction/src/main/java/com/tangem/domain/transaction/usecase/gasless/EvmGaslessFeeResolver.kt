package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

/** Native coin first, a supported token only when its balance falls short. */
@Suppress("LongParameterList")
class EvmGaslessFeeResolver(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val gaslessYieldRepository: GaslessYieldRepository,
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
) {

    private val tokenFeeCalculator = TokenFeeCalculator(
        walletManagersFacade = walletManagersFacade,
        gaslessTransactionRepository = gaslessTransactionRepository,
        demoConfig = demoConfig,
        gaslessYieldRepository = gaslessYieldRepository,
    )

    /** @param sentAmount required for a yield-supply send, whose [TransactionData.Uncompiled.amount] is zeroed. */
    suspend fun resolveWithAutoFeeToken(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        accountStatusList: AccountStatusList,
        sentAmount: BigDecimal? = null,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
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
            sentAmount = sentAmount,
        )
    }

    /**
     * No balance-based fallback: the token is the user's choice, so falling short is an error rather than
     * a silent switch to the native coin.
     *
     * @param sentAmount required for a yield-supply send, whose [TransactionData.Uncompiled.amount] is zeroed.
     */
    @Suppress("LongParameterList")
    suspend fun resolveWithSelectedFeeToken(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        tokenCurrencyStatus: CryptoCurrencyStatus,
        sentAmount: BigDecimal? = null,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val walletManager = prepareWalletManager(userWallet, network)

        val initialTxFee = tokenFeeCalculator.calculateInitialFee(
            userWallet = userWallet,
            network = network,
            walletManager = walletManager,
            transactionData = transactionData,
        ).bind()

        val initialFee = initialTxFee.normal as? Fee.Ethereum
            ?: raiseIllegalStateError(
                error = "only Fee.Ethereum supported, but was ${initialTxFee.normal::class.qualifiedName}",
            )

        val isYieldActive = tokenCurrencyStatus.value.yieldSupplyStatus?.isActive == true

        val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenCurrencyStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
            isYieldActive = isYieldActive,
            userWallet = userWallet,
        ).bind()

        // Only the yield path needs a plan: without an active position the fee token is spent straight
        // from the wallet balance, and there is nothing to withdraw first.
        if (isYieldActive) {
            attachGaslessFeePlan(
                resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
                userWallet = userWallet,
                tokenStatus = tokenCurrencyStatus,
                tokenFeeExtended = tokenFeeExtended,
                transactionData = transactionData,
                isYieldActive = true,
                sentAmount = sentAmount,
            ).copy(nativeFee = initialTxFee)
        } else {
            tokenFeeExtended.copy(nativeFee = initialTxFee)
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
        sentAmount: BigDecimal?,
    ): TransactionFeeExtended {
        val feeValue = initialFee.normal.amount.value ?: raise(GetFeeError.UnknownError)

        val networkCurrenciesStatuses = accountStatusList
            .flattenPersonalCurrencies()
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
                transactionData = transactionData,
                sentAmount = sentAmount,
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
        transactionData: TransactionData,
        sentAmount: BigDecimal?,
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

        // For a yield token value.amount is already effectiveBalance (liquid EOA + effectiveProtocolBalance),
        // so adding effectiveProtocolBalance again would double-count the module portion.
        val candidates = networkCurrenciesStatuses
            .asSequence()
            .filter { it.currency is CryptoCurrency.Token }
            .filter { (it.currency as CryptoCurrency.Token).contractAddress.lowercase() in supportedGaslessTokens }
            .filter { status ->
                val total = status.value.amount ?: BigDecimal.ZERO
                total > BigDecimal.ZERO || status.value.yieldSupplyStatus?.isActive == true
            }
            .sortedByDescending { status -> status.value.amount ?: BigDecimal.ZERO }

        val tokenForPayFeeStatus = candidates.firstOrNull() ?: raise(GaslessError.NoSupportedTokensFound)

        val isYieldActive = tokenForPayFeeStatus.value.yieldSupplyStatus?.isActive == true
        val tokenFeeExtended = tokenFeeCalculator.calculateTokenFee(
            walletManager = walletManager,
            tokenForPayFeeStatus = tokenForPayFeeStatus,
            nativeCurrencyStatus = nativeCurrencyStatus,
            initialFee = initialFee,
            isYieldActive = isYieldActive,
            userWallet = userWallet,
        ).bind()

        attachGaslessFeePlan(
            resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
            userWallet = userWallet,
            tokenStatus = tokenForPayFeeStatus,
            tokenFeeExtended = tokenFeeExtended,
            transactionData = transactionData,
            isYieldActive = isYieldActive,
            sentAmount = sentAmount,
        )
    }
}