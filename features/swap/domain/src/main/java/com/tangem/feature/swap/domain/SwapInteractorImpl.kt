package com.tangem.feature.swap.domain

import android.util.Base64
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.models.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.domain.swap.usecase.GetSwapPairUseCase
import com.tangem.domain.tokens.GetAssetRequirementsUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.fee.CexSwapFeeCalculator
import com.tangem.feature.swap.domain.fee.DexSwapFeeCalculator
import com.tangem.feature.swap.domain.fee.SwapFeeFactory
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.lib.crypto.BlockchainUtils.SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Suppress("LargeClass", "LongParameterList")
internal class SwapInteractorImpl @Inject constructor(
    private val repository: SwapRepository,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val quotesRepository: QuotesRepository,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val appCurrencyRepository: AppCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase,
    private val estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getEthSpecificFeeUseCase: GetEthSpecificFeeUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val getAssetRequirementsUseCase: GetAssetRequirementsUseCase,
    private val amountFormatter: AmountFormatter,
    private val rampStateManager: RampStateManager,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val getAllowanceInfoUseCase: GetAllowanceInfoUseCase,
    private val getSwapPairUseCase: GetSwapPairUseCase,
    private val dexSwapFeeCalculator: DexSwapFeeCalculator,
    private val cexSwapFeeCalculator: CexSwapFeeCalculator,
) : SwapInteractor {

    private val getSelectedAppCurrencyUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }

    private val hundredPercent = BigInteger("100")

    override suspend fun getPair(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        filterProviderTypes: List<ExchangeProviderType>,
    ): Either<ExpressError, List<SwapPairLeast>> {
        return getSwapPairUseCase(
            primarySwapCurrencyStatus = fromSwapCurrencyStatus,
            secondarySwapCurrencyStatus = toSwapCurrencyStatus,
            filterProviderTypes = filterProviderTypes.map { type ->
                // Temporary solution until domain layer is migrated
                when (type) {
                    ExchangeProviderType.DEX -> ExpressProviderType.DEX
                    ExchangeProviderType.CEX -> ExpressProviderType.CEX
                    ExchangeProviderType.DEX_BRIDGE -> ExpressProviderType.DEX_BRIDGE
                }
            },
            swapTxType = SwapTxType.Swap,
        ).map { pairs ->
            pairs.map { pair ->
                SwapPairLeast(
                    from = LeastTokenInfo(
                        contractAddress = pair.from.currency.getContractAddress(),
                        network = pair.from.currency.network.rawId,
                    ),
                    to = LeastTokenInfo(
                        contractAddress = pair.to.currency.getContractAddress(),
                        network = pair.to.currency.network.rawId,
                    ),
                    providers = pair.providers.map { provider ->
                        provider.toSwapProvider()
                    },
                )
            }
        }
    }

    override fun findProvidersForPair(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
    ): List<SwapProvider> {
        return pairs.firstOrNull { pair ->
            pair.from.network == fromSwapCurrencyStatus.currency.network.rawId &&
                pair.from.contractAddress == fromSwapCurrencyStatus.currency.getContractAddress() &&
                pair.to.network == toSwapCurrencyStatus.currency.network.rawId &&
                pair.to.contractAddress == toSwapCurrencyStatus.currency.getContractAddress()
        }?.providers.orEmpty()
    }

    override suspend fun findProvidersForPairWithCheck(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
    ): List<SwapProvider> {
        val requirements = getAssetRequirementsUseCase.invoke(
            fromSwapCurrencyStatus.userWalletId,
            fromSwapCurrencyStatus.currency,
        ).getOrNull()

        if (!rampStateManager.checkAssetRequirements(requirements)) {
            return emptyList()
        }

        return findProvidersForPair(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            pairs = pairs,
        )
    }

    override suspend fun findBestQuote(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        reduceBalanceBy: BigDecimal,
    ): Map<SwapProvider, SwapState> {
        TangemLogger.i(
            """
               Find the best quote
               |- fromSwapCurrencyStatus:
               |---- walletId: ${fromSwapCurrencyStatus.userWalletId}
               |---- accountId: ${fromSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${fromSwapCurrencyStatus.currency.id}
               |- toSwapCurrencyStatus: $toSwapCurrencyStatus
               |---- walletId: ${toSwapCurrencyStatus.userWalletId}
               |---- accountId: ${toSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${toSwapCurrencyStatus.currency.id}
               |- providers: $providers
               |- amountToSwap: $amountToSwap
            """.trimIndent(),
            shouldSanitize = false,
        )

        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.signum() == 0) {
            return providers.associateWith { createEmptyAmountState() }
        }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
        val isBalanceWithoutFeeEnough = isBalanceEnough(fromSwapCurrencyStatus, amount, null)
        return supervisorScope {
            providers.map { provider ->
                async {
                    when (provider.type) {
                        ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                            if (isSolana(fromSwapCurrencyStatus.currency.network.rawId)) {
                                manageDexSolana(
                                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                                    provider = provider,
                                    amount = amount,
                                    isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                                    expressOperationType = ExpressOperationType.SWAP,
                                )
                            } else {
                                manageDex(
                                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                                    provider = provider,
                                    amount = amount,
                                    isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                                    expressOperationType = ExpressOperationType.SWAP,
                                )
                            }
                        }
                        ExchangeProviderType.CEX -> {
                            manageCex(
                                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                toSwapCurrencyStatus = toSwapCurrencyStatus,
                                provider = provider,
                                amount = amount,
                                reduceBalanceBy = reduceBalanceBy,
                                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                            )
                        }
                    }
                }
            }.awaitAll().toMap()
        }
    }

    @Suppress("LongMethod")
    private suspend fun manageDex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        isBalanceWithoutFeeEnough: Boolean,
        expressOperationType: ExpressOperationType,
    ): Pair<SwapProvider, SwapState> {
        if (fromSwapCurrencyStatus.status.value.yieldSupplyStatus?.isActive == true) {
            return provider to produceDexSwapDataError(
                error = ExpressDataError.DexActiveSupplyError,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                amount = amount,
            )
        }

        val maybeQuotes = repository.findBestQuote(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        val fromTokenAddress = getTokenAddress(fromSwapCurrencyStatus.currency)
        val isAllowedToSpend = maybeQuotes.fold(
            ifRight = { quotes ->
                quotes.allowanceContract?.let { allowanceContract ->
                    getAllowanceInfoUseCase(
                        userWalletId = fromSwapCurrencyStatus.userWalletId,
                        cryptoCurrency = fromSwapCurrencyStatus.currency,
                        spenderAddress = allowanceContract,
                        requiredAmount = amount.value,
                    ).getOrNull() is AllowanceInfo.Enough
                } != false
            },
            ifLeft = { false },
        )

        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            cryptoCurrencyBalanceFetcher(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                currency = fromSwapCurrencyStatus.currency,
            )
        }
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
            provider to loadDexSwapDataNoFee(
                provider = provider,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                amount = amount,
                expressOperationType = expressOperationType,
            )
        } else {
            provider to getQuotesState(
                provider = provider,
                quoteDataModel = maybeQuotes,
                amount = amount,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                includeFeeInAmount = IncludeFeeInAmount.Excluded, // exclude for dex
            )
        }
    }

    private suspend fun manageDexSolana(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        isBalanceWithoutFeeEnough: Boolean,
        expressOperationType: ExpressOperationType,
    ): Pair<SwapProvider, SwapState> {
        val maybeQuotes = repository.findBestQuote(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        return if (isBalanceWithoutFeeEnough && maybeQuotes.isRight()) {
            provider to loadDexSwapDataNoFee(
                provider = provider,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                amount = amount,
                expressOperationType = expressOperationType,
            )
        } else {
            provider to getQuotesState(
                provider = provider,
                quoteDataModel = maybeQuotes,
                amount = amount,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                isAllowedToSpend = true,
                isBalanceWithoutFeeEnough = false,
                includeFeeInAmount = IncludeFeeInAmount.Excluded, // exclude for dex
            )
        }
    }

    private suspend fun manageCex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        isBalanceWithoutFeeEnough: Boolean,
    ): Pair<SwapProvider, SwapState> {
        return provider to loadCexQuoteData(
            amount = amount,
            reduceBalanceBy = reduceBalanceBy,
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            isAllowedToSpend = true,
            isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
            provider = provider,
        )
    }

    private suspend fun manageWarnings(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        fee: BigDecimal,
        includeFeeInAmount: IncludeFeeInAmount,
    ): CryptoCurrencyCheck {
        val balanceAfterTransaction = getCoinBalanceAfterTransaction(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            includeFeeInAmount = includeFeeInAmount,
            fee = fee,
        )
        val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
            includeFeeInAmount.amountSubtractFee
        } else {
            amount
        }
        val feePaidCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
        ).getOrNull()
        val currencyCheck = getCurrencyCheckUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            currencyStatus = fromSwapCurrencyStatus.status,
            feeCurrencyStatus = feePaidCurrencyStatus,
            amount = amountToRequest.value,
            fee = fee,
            feeCurrencyBalanceAfterTransaction = balanceAfterTransaction,
        )

        return currencyCheck
    }

    private suspend fun getCoinBalanceAfterTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: BigDecimal,
    ): BigDecimal? {
        return when (fromSwapCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                val statusValue = fromSwapCurrencyStatus.status.value as? CryptoCurrencyStatus.Loaded
                when (includeFeeInAmount) {
                    is IncludeFeeInAmount.Included -> {
                        statusValue?.let { it.amount - includeFeeInAmount.amountSubtractFee.value - fee }
                    }
                    is IncludeFeeInAmount.Excluded -> {
                        statusValue?.let { it.amount - amount.value - fee }
                    }
                    else -> null
                }
            }
            is CryptoCurrency.Token -> {
                val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)
                when (feePaidCurrency) {
                    FeePaidCurrency.Coin -> {
                        val nativeBalance = walletManagersFacade.getNativeTokenBalance(
                            userWalletId = fromSwapCurrencyStatus.userWalletId,
                            networkId = fromSwapCurrencyStatus.currency.network.rawId,
                            derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
                        )

                        nativeBalance - fee
                    }
                    else -> null // it doesnt matter for this fun
                }
            }
        }
    }

    private suspend fun manageTransactionValidationWarnings(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        feeValue: BigDecimal,
    ): Throwable? {
        val currency = fromSwapCurrencyStatus.currency
        val blockchain = currency.network.toBlockchain()
        // Stellar validation removed because swap uses destination = "0" and throws an error
        if (blockchain == Blockchain.Stellar) {
            return null
        }

        val fee = Fee.Common(
            amount = Amount(
                value = feeValue,
                blockchain = blockchain,
            ),
        )

        val result = validateTransactionUseCase(
            amount = amount.value.convertToSdkAmount(fromSwapCurrencyStatus.status),
            fee = fee,
            memo = null,
            destination = getTokenAddress(fromSwapCurrencyStatus.currency),
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = currency.network,
        ).leftOrNull()

        return result
    }

    @Suppress("NullableToStringCall")
    override suspend fun onSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        amountToSwap: String,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: TxFee?,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
    ): SwapTransactionState {
        TangemLogger.i(
            """
               Swap
               |- swapProvider: $swapProvider
               |- swapData: $swapData
               |- fromSwapCurrencyStatus: 
               |---- walletId: ${fromSwapCurrencyStatus.userWalletId}
               |---- accountId: ${fromSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${fromSwapCurrencyStatus.currency.id}
               |- toSwapCurrencyStatus: $toSwapCurrencyStatus
               |---- walletId: ${toSwapCurrencyStatus.userWalletId}
               |---- accountId: ${toSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${toSwapCurrencyStatus.currency.id}
               |- amountToSwap: $amountToSwap
               |- includeFeeInAmount: $includeFeeInAmount
               |- fee: $fee
            """.trimIndent(),
            shouldSanitize = false,
        )

        val userWallet = fromSwapCurrencyStatus.userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.DemoMode
        }

        return when (swapProvider.type) {
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amountToSwap)
                val amount = SwapAmount(requireNotNull(amountDecimal), fromSwapCurrencyStatus.currency.decimals)
                val amountToSwapWithFee = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                    includeFeeInAmount.amountSubtractFee
                } else {
                    amount
                }
                onSwapCex(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amountToSwapWithFee,
                    txFee = fee,
                    swapProvider = swapProvider,
                    expressOperationType = expressOperationType,
                    isTangemPayWithdrawal = isTangemPayWithdrawal,
                )
            }
            ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                val networkId = fromSwapCurrencyStatus.currency.network.rawId
                if (isSolana(networkId)) {
                    onSwapSolanaDex(
                        provider = swapProvider,
                        swapData = requireNotNull(swapData),
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        amountToSwap = amountToSwap,
                    )
                } else {
                    if (fee == null) return SwapTransactionState.Error.UnknownError
                    onSwapDex(
                        provider = swapProvider,
                        swapData = requireNotNull(swapData),
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        txFee = fee,
                        amountToSwap = amountToSwap,
                    )
                }
            }
        }
    }

    @Suppress("NullableToStringCall")
    override suspend fun onSwapWithUnifiedFee(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        amountToSwap: String,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: SwapFee?,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
    ): SwapTransactionState {
        TangemLogger.i(
            """
               Swap (unified fee)
               |- swapProvider: $swapProvider
               |- swapData: $swapData
               |- fromSwapCurrencyStatus:
               |---- walletId: ${fromSwapCurrencyStatus.userWalletId}
               |---- accountId: ${fromSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${fromSwapCurrencyStatus.currency.id}
               |- toSwapCurrencyStatus: $toSwapCurrencyStatus
               |---- walletId: ${toSwapCurrencyStatus.userWalletId}
               |---- accountId: ${toSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${toSwapCurrencyStatus.currency.id}
               |- amountToSwap: $amountToSwap
               |- includeFeeInAmount: $includeFeeInAmount
               |- fee: $fee
            """.trimIndent(),
            shouldSanitize = false,
        )

        val userWallet = fromSwapCurrencyStatus.userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.DemoMode
        }

        return when (swapProvider.type) {
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amountToSwap)
                val amount = SwapAmount(requireNotNull(amountDecimal), fromSwapCurrencyStatus.currency.decimals)
                val amountToSwapWithFee = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                    includeFeeInAmount.amountSubtractFee
                } else {
                    amount
                }
                onSwapCexUnified(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amountToSwapWithFee,
                    swapFee = fee,
                    swapProvider = swapProvider,
                    expressOperationType = expressOperationType,
                    isTangemPayWithdrawal = isTangemPayWithdrawal,
                )
            }
            ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                val networkId = fromSwapCurrencyStatus.currency.network.rawId
                if (isSolana(networkId)) {
                    onSwapSolanaDex(
                        provider = swapProvider,
                        swapData = requireNotNull(swapData),
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        amountToSwap = amountToSwap,
                    )
                } else {
                    if (fee == null) return SwapTransactionState.Error.UnknownError
                    onSwapDexUnified(
                        provider = swapProvider,
                        swapData = requireNotNull(swapData),
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        swapFee = fee,
                        amountToSwap = amountToSwap,
                    )
                }
            }
        }
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 4. DEX swap dispatch using [SwapFee]. Mirrors [onSwapDex] exactly,
     * substituting `SwapFee.fee` where the legacy code used `TxFee.fee`. Solana DEX continues
     * to use [onSwapSolanaDex] which doesn't consume a fee.
     */
    private suspend fun onSwapDexUnified(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amountToSwap: String,
        swapFee: SwapFee,
    ): SwapTransactionState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val txValue = requireNotNull(swapData.transaction.txValue) { "txValue is null" }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
        val dexTransaction = swapData.transaction as ExpressTransactionModel.DEX
        val dataToSign = dexTransaction.txData
        val amountToSend = createNativeAmountForDex(txValue, fromSwapCurrencyStatus.currency.network)
        val txData = createTransactionUseCase(
            amount = amountToSend,
            fee = swapFee.fee,
            memo = null,
            destination = swapData.transaction.txTo,
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = toSwapCurrencyStatus.currency.network,
            txExtras = createDexTxExtras(
                dataToSign,
                fromSwapCurrencyStatus.currency.network,
                swapFee.fee.getGasLimit(),
            ),
        ).getOrElse { error ->
            TangemLogger.e("Failed to create swap dex tx data", error)
            return SwapTransactionState.Error.UnknownError
        }

        return handleSwapResult(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            provider = provider,
            swapData = swapData,
            amount = amount,
            txData = txData,
            payInAddress = getPayoutAddress(txData),
        )
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 4. CEX swap dispatch using [SwapFee]. Mirrors [onSwapCex] exactly.
     *
     * Branch selection (matches legacy):
     *  - Gasless token path: `swapFee.transactionFeeResult is LoadedExtended && selectedFeeToken.currency is Token`
     *    → `createAndSendGaslessTransactionUseCase`.
     *  - Otherwise → `sendTransactionUseCase` with `swapFee.fee`.
     */
    @Suppress("LongMethod", "CanBeNonNullable")
    private suspend fun onSwapCexUnified(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapFee: SwapFee?,
        swapProvider: SwapProvider,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
    ): SwapTransactionState {
        val fromNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val fromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty()
        val toNetworkAddress = toSwapCurrencyStatus.status.value.networkAddress
        val toAddress = toNetworkAddress?.defaultAddress?.value.orEmpty()
        val exchangeData = repository.getExchangeData(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            fromAddress = fromAddress,
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = swapProvider.providerId,
            rateType = RateType.FLOAT,
            expressOperationType = expressOperationType,
            toAddress = toAddress,
            refundAddress = fromNetworkAddress?.defaultAddress?.value,
            refundExtraId = null, // currently always null,
        ).getOrElse { error -> return SwapTransactionState.Error.ExpressError(error) }

        val exchangeDataCex =
            exchangeData.transaction as? ExpressTransactionModel.CEX ?: return SwapTransactionState.Error.UnknownError

        if (isTangemPayWithdrawal) {
            return SwapTransactionState.TangemPayWithdrawalData(
                cryptoAmount = amount.value,
                cryptoCurrencyId = requireNotNull(fromSwapCurrencyStatus.currency.id.rawCurrencyId),
                cexAddress = exchangeDataCex.txTo,
                fromAmount = amountFormatter.formatSwapAmountToUI(
                    amount,
                    fromSwapCurrencyStatus.currency.symbol,
                ),
                fromAmountValue = amount.value,
                toAmount = amountFormatter.formatSwapAmountToUI(
                    exchangeData.toTokenAmount,
                    toSwapCurrencyStatus.currency.symbol,
                ),
                toAmountValue = exchangeData.toTokenAmount.value,
                storeData = SwapTransactionState.TangemPayWithdrawalData.StoreTransactionData(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    txExternalUrl = exchangeDataCex.externalTxUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                    averageDuration = null,
                ),
                exchangeData = TangemPayWithdrawExchangeState(
                    txId = exchangeDataCex.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty(),
                    payInAddress = exchangeData.transaction.txTo,
                    payInExtraId = exchangeDataCex.txExtraId,
                ),
            )
        }

        val userWallet = fromSwapCurrencyStatus.userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.Error.UnknownError
        }
        val fee = requireNotNull(swapFee)
        val txData = createTransferTransactionUseCase(
            amount = amount.value.convertToSdkAmount(fromSwapCurrencyStatus.status),
            fee = fee.fee,
            memo = exchangeDataCex.txExtraId,
            destination = exchangeDataCex.txTo,
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = fromSwapCurrencyStatus.currency.network,
        ).getOrElse { error ->
            TangemLogger.e("Failed to create swap CEX tx data", error)
            return SwapTransactionState.Error.UnknownError
        }

        if (txData.extras == null && exchangeDataCex.txExtraId != null) {
            return SwapTransactionState.Error.UnknownError
        }

        val isGaslessToken = fee.selectedFeeToken.currency is CryptoCurrency.Token &&
            fee.transactionFeeResult is TransactionFeeResult.LoadedExtended
        val result = if (isGaslessToken) {
            createAndSendGaslessTransactionUseCase.invoke(
                transactionData = txData,
                userWallet = userWallet,
                fee = (fee.transactionFeeResult as TransactionFeeResult.LoadedExtended).fee,
            )
        } else {
            sendTransactionUseCase(
                txData = txData,
                userWallet = userWallet,
                network = fromSwapCurrencyStatus.currency.network,
            )
        }

        val cexNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val cexFromAddress = cexNetworkAddress?.defaultAddress?.value.orEmpty()
        return result.fold(
            ifLeft = { error -> SwapTransactionState.Error.TransactionError(error) },
            ifRight = { txHash ->
                repository.exchangeSent(
                    userWallet = userWallet,
                    txId = exchangeDataCex.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = cexFromAddress,
                    payInAddress = getPayoutAddress(txData),
                    txHash = txHash,
                    payInExtraId = exchangeDataCex.txExtraId,
                )
                val timestamp = System.currentTimeMillis()
                val txExternalUrl = exchangeDataCex.externalTxUrl
                storeSwapTransaction(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    timestamp = timestamp,
                    txExternalUrl = txExternalUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                )
                storeLastCryptoCurrencyId(toSwapCurrencyStatus)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        fromSwapCurrencyStatus.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        exchangeData.toTokenAmount,
                        toSwapCurrencyStatus.currency.symbol,
                    ),
                    toAmountValue = exchangeData.toTokenAmount.value,
                    txHash = txHash,
                    txExternalUrl = txExternalUrl,
                    timestamp = timestamp,
                )
            },
        )
    }

    private suspend fun onSwapDex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amountToSwap: String,
        txFee: TxFee,
    ): SwapTransactionState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val txValue = requireNotNull(swapData.transaction.txValue) { "txValue is null" }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
        val dexTransaction = swapData.transaction as ExpressTransactionModel.DEX
        val dataToSign = dexTransaction.txData
        val amountToSend = createNativeAmountForDex(txValue, fromSwapCurrencyStatus.currency.network)
        val txData = createTransactionUseCase(
            amount = amountToSend,
            fee = txFee.fee,
            memo = null,
            destination = swapData.transaction.txTo,
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = toSwapCurrencyStatus.currency.network,
            txExtras = createDexTxExtras(
                dataToSign,
                fromSwapCurrencyStatus.currency.network,
                txFee.fee.getGasLimit(),
            ),
        ).getOrElse { error ->
            TangemLogger.e("Failed to create swap dex tx data", error)
            return SwapTransactionState.Error.UnknownError
        }

        return handleSwapResult(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            provider = provider,
            swapData = swapData,
            amount = amount,
            txData = txData,
            payInAddress = getPayoutAddress(txData),
        )
    }

    private suspend fun onSwapSolanaDex(
        provider: SwapProvider,
        swapData: SwapDataModel,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amountToSwap: String,
    ): SwapTransactionState {
        val dexTransaction = swapData.transaction as? ExpressTransactionModel.DEX
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val txDataBase64 = requireNotNull(dexTransaction?.txData) { "txData is null" }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
        val compiledTransaction = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(Base64.decode(txDataBase64, Base64.NO_WRAP)),
        )
        return handleSwapResult(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            provider = provider,
            swapData = swapData,
            amount = amount,
            txData = compiledTransaction,
            payInAddress = swapData.transaction.txTo,
        )
    }

    private suspend fun handleSwapResult(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amount: SwapAmount,
        txData: TransactionData,
        payInAddress: String,
    ): SwapTransactionState {
        val result = sendTransactionUseCase(
            txData = txData,
            userWallet = fromSwapCurrencyStatus.userWallet,
            network = fromSwapCurrencyStatus.currency.network,
        )
        return result.fold(
            ifRight = { txHash ->
                val networkAddress = fromSwapCurrencyStatus.status.value.networkAddress
                val fromAddress = networkAddress?.defaultAddress?.value.orEmpty()
                repository.exchangeSent(
                    userWallet = fromSwapCurrencyStatus.userWallet,
                    txId = swapData.transaction.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = fromAddress,
                    payInAddress = payInAddress,
                    txHash = txHash,
                    payInExtraId = swapData.transaction.txExtraId,
                )
                val timestamp = System.currentTimeMillis()
                storeSwapTransaction(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = provider,
                    swapDataModel = swapData,
                    timestamp = timestamp,
                )
                storeLastCryptoCurrencyId(fromSwapCurrencyStatus)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        fromSwapCurrencyStatus.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        swapData.toTokenAmount,
                        toSwapCurrencyStatus.currency.symbol,
                    ),
                    toAmountValue = swapData.toTokenAmount.value,
                    txHash = txHash,
                    timestamp = timestamp,
                )
            },
            ifLeft = { SwapTransactionState.Error.TransactionError(it) },
        )
    }

    private fun createDexTxExtras(data: String, network: Network, gasLimit: Int?): TransactionExtras {
        return createTransactionExtrasUseCase(
            data = data,
            network = network,
            gasLimit = gasLimit?.toBigInteger(),
        ).getOrNull() ?: error("failed to create extras")
    }

    @Suppress("LongMethod", "CanBeNonNullable")
    private suspend fun onSwapCex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        txFee: TxFee?,
        swapProvider: SwapProvider,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
    ): SwapTransactionState {
        val fromNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val fromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty()
        val toNetworkAddress = toSwapCurrencyStatus.status.value.networkAddress
        val toAddress = toNetworkAddress?.defaultAddress?.value.orEmpty()
        val exchangeData = repository.getExchangeData(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            fromAddress = fromAddress,
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = swapProvider.providerId,
            rateType = RateType.FLOAT,
            expressOperationType = expressOperationType,
            toAddress = toAddress,
            refundAddress = fromNetworkAddress?.defaultAddress?.value,
            refundExtraId = null, // currently always null,
        ).getOrElse { error -> return SwapTransactionState.Error.ExpressError(error) }

        val exchangeDataCex =
            exchangeData.transaction as? ExpressTransactionModel.CEX ?: return SwapTransactionState.Error.UnknownError

        if (isTangemPayWithdrawal) {
            return SwapTransactionState.TangemPayWithdrawalData(
                cryptoAmount = amount.value,
                cryptoCurrencyId = requireNotNull(fromSwapCurrencyStatus.currency.id.rawCurrencyId),
                cexAddress = exchangeDataCex.txTo,
                fromAmount = amountFormatter.formatSwapAmountToUI(
                    amount,
                    fromSwapCurrencyStatus.currency.symbol,
                ),
                fromAmountValue = amount.value,
                toAmount = amountFormatter.formatSwapAmountToUI(
                    exchangeData.toTokenAmount,
                    toSwapCurrencyStatus.currency.symbol,
                ),
                toAmountValue = exchangeData.toTokenAmount.value,
                storeData = SwapTransactionState.TangemPayWithdrawalData.StoreTransactionData(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    txExternalUrl = exchangeDataCex.externalTxUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                    averageDuration = null,
                ),
                exchangeData = TangemPayWithdrawExchangeState(
                    txId = exchangeDataCex.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty(),
                    payInAddress = exchangeData.transaction.txTo,
                    payInExtraId = exchangeDataCex.txExtraId,
                ),
            )
        }

        val userWallet = fromSwapCurrencyStatus.userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.Error.UnknownError
        }
        val fee = requireNotNull(txFee)
        val txData = createTransferTransactionUseCase(
            amount = amount.value.convertToSdkAmount(fromSwapCurrencyStatus.status),
            fee = fee.fee,
            memo = exchangeDataCex.txExtraId,
            destination = exchangeDataCex.txTo,
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = fromSwapCurrencyStatus.currency.network,
        ).getOrElse { error ->
            TangemLogger.e("Failed to create swap CEX tx data", error)
            return SwapTransactionState.Error.UnknownError
        }

        if (txData.extras == null && exchangeDataCex.txExtraId != null) {
            return SwapTransactionState.Error.UnknownError
        }

        val result = when (fee) {
            is TxFee.FeeComponent -> {
                if (fee.selectedToken?.currency is CryptoCurrency.Token &&
                    fee.transactionFeeResult is TransactionFeeResult.LoadedExtended
                ) {
                    createAndSendGaslessTransactionUseCase.invoke(
                        transactionData = txData,
                        userWallet = userWallet,
                        fee = fee.transactionFeeResult.fee,
                    )
                } else {
                    sendTransactionUseCase(
                        txData = txData,
                        userWallet = userWallet,
                        network = fromSwapCurrencyStatus.currency.network,
                    )
                }
            }
            is TxFee.Legacy -> {
                sendTransactionUseCase(
                    txData = txData,
                    userWallet = userWallet,
                    network = fromSwapCurrencyStatus.currency.network,
                )
            }
        }

        val cexNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val cexFromAddress = cexNetworkAddress?.defaultAddress?.value.orEmpty()
        return result.fold(
            ifLeft = { error -> SwapTransactionState.Error.TransactionError(error) },
            ifRight = { txHash ->
                repository.exchangeSent(
                    userWallet = userWallet,
                    txId = exchangeDataCex.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = cexFromAddress,
                    payInAddress = getPayoutAddress(txData),
                    txHash = txHash,
                    payInExtraId = exchangeDataCex.txExtraId,
                )
                val timestamp = System.currentTimeMillis()
                val txExternalUrl = exchangeDataCex.externalTxUrl
                storeSwapTransaction(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    timestamp = timestamp,
                    txExternalUrl = txExternalUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                )
                storeLastCryptoCurrencyId(toSwapCurrencyStatus)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        fromSwapCurrencyStatus.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        exchangeData.toTokenAmount,
                        toSwapCurrencyStatus.currency.symbol,
                    ),
                    toAmountValue = exchangeData.toTokenAmount.value,
                    txHash = txHash,
                    txExternalUrl = txExternalUrl,
                    timestamp = timestamp,
                )
            },
        )
    }

    override suspend fun storeSwapTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapProvider: SwapProvider,
        swapDataModel: SwapDataModel,
        timestamp: Long,
        txExternalUrl: String?,
        txExternalId: String?,
        averageDuration: Int?,
    ) {
        swapTransactionRepository.storeTransaction(
            fromUserWalletId = fromSwapCurrencyStatus.userWalletId,
            toUserWalletId = toSwapCurrencyStatus.userWalletId,
            fromCryptoCurrency = fromSwapCurrencyStatus.currency,
            toCryptoCurrency = toSwapCurrencyStatus.currency,
            fromAccount = fromSwapCurrencyStatus.account,
            toAccount = toSwapCurrencyStatus.account,
            transaction = SavedSwapTransactionModel(
                txId = swapDataModel.transaction.txId,
                provider = swapProvider,
                timestamp = timestamp,
                fromCryptoAmount = amount.value,
                toCryptoAmount = swapDataModel.toTokenAmount.value,
                status = ExchangeStatusModel(
                    providerId = swapProvider.providerId,
                    status = ExchangeStatus.New,
                    txId = swapDataModel.transaction.txId,
                    txExternalUrl = txExternalUrl,
                    txExternalId = txExternalId,
                    averageDuration = averageDuration,
                ),
            ),
        )
    }

    @Suppress("LongParameterList")
    override suspend fun loadFeeForSwapTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: String,
        reduceBalanceBy: BigDecimal,
        provider: SwapProvider,
        selectedFeeToken: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        when (provider.type) {
            ExchangeProviderType.DEX,
            ExchangeProviderType.DEX_BRIDGE,
            -> raise(GetFeeError.GaslessError.NetworkIsNotSupported)
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amount)
                if (amountDecimal == null || amountDecimal.signum() == 0) {
                    raise(GetFeeError.UnknownError)
                }

                return if (selectedFeeToken != null) {
                    estimateFeeForTokenUseCase(
                        userWallet = fromSwapCurrencyStatus.userWallet,
                        feeTokenCurrencyStatus = selectedFeeToken,
                        sendingTokenCurrencyStatus = fromSwapCurrencyStatus.status,
                        amount = amountDecimal,
                    )
                } else {
                    estimateFeeForGaslessTxUseCase(
                        amount = amountDecimal,
                        userWallet = fromSwapCurrencyStatus.userWallet,
                        sendingTokenCurrencyStatus = fromSwapCurrencyStatus.status,
                    )
                }
            }
        }
    }

    override suspend fun loadFeeForSwapTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: String,
        reduceBalanceBy: BigDecimal,
        provider: SwapProvider,
    ): Either<GetFeeError, TransactionFee> = either {
        return when (provider.type) {
            ExchangeProviderType.DEX,
            ExchangeProviderType.DEX_BRIDGE,
            -> {
                val fromNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
                val dexFromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty()
                val toNetworkAddress = toSwapCurrencyStatus.status.value.networkAddress
                val dexToAddress = toNetworkAddress?.defaultAddress?.value.orEmpty()
                val amountBigDecimal = toBigDecimalOrNull(amount)
                if (amountBigDecimal == null || amountBigDecimal.signum() == 0) {
                    raise(GetFeeError.UnknownError)
                }
                val swapAmount = SwapAmount(amountBigDecimal, fromSwapCurrencyStatus.currency.decimals)

                repository.getExchangeData(
                    userWallet = fromSwapCurrencyStatus.userWallet,
                    fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
                    fromAddress = dexFromAddress,
                    toNetwork = toSwapCurrencyStatus.currency.network.rawId,
                    fromAmount = swapAmount.toStringWithRightOffset(),
                    fromDecimals = swapAmount.decimals,
                    toDecimals = toSwapCurrencyStatus.currency.decimals,
                    providerId = provider.providerId,
                    rateType = RateType.FLOAT,
                    toAddress = dexToAddress,
                    refundAddress = fromNetworkAddress?.defaultAddress?.value,
                    expressOperationType = ExpressOperationType.SWAP,
                ).map { swapData ->
                    val transaction = swapData.transaction as ExpressTransactionModel.DEX
                    loadFeeForDex(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        transaction = transaction,
                    ).getOrElse { raise(GetFeeError.UnknownError) }
                }.mapLeft {
                    GetFeeError.UnknownError
                }
            }
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amount)
                if (amountDecimal == null || amountDecimal.signum() == 0) {
                    raise(GetFeeError.UnknownError)
                }

                estimateFeeUseCase.invoke(
                    amount = amountDecimal,
                    userWallet = fromSwapCurrencyStatus.userWallet,
                    cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
                ).map {
                    it.patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_SEND)
                }
            }
        }
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 3 unified fee API. Delegates to [DexSwapFeeCalculator] /
     * [CexSwapFeeCalculator] and wraps the result in a [SwapFee].
     *
     * Behavior parity with the legacy `loadFeeForSwapTransaction` overloads is intentional —
     * the legacy methods stay in place through Phase 4. See `SwapInteractor.loadSwapFee` for
     * the full contract.
     */
    @Suppress("LongParameterList", "ReturnCount")
    override suspend fun loadSwapFee(
        provider: SwapProvider,
        fromStatus: SwapCurrencyStatus,
        toStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapData: SwapDataModel?,
        selectedFeeToken: CryptoCurrencyStatus?,
    ): Either<GetFeeError, SwapFee> = either {
        if (amount.value.signum() == 0) {
            raise(GetFeeError.UnknownError)
        }
        return when (provider.type) {
            ExchangeProviderType.DEX,
            ExchangeProviderType.DEX_BRIDGE,
            -> loadDexSwapFee(
                fromStatus = fromStatus,
                swapData = swapData,
                selectedFeeToken = selectedFeeToken,
            )
            ExchangeProviderType.CEX -> loadCexSwapFee(
                fromStatus = fromStatus,
                amount = amount,
                selectedFeeToken = selectedFeeToken,
            )
        }
    }

    /**
     * [REDACTED_TASK_KEY] — DEX branch of [loadSwapFee]. Pulls the cached `ExpressTransactionModel.DEX`
     * out of [swapData] and hands it to [DexSwapFeeCalculator]. Maps [ExpressDataError] →
     * `Left(GetFeeError.UnknownError)` to keep the unified surface a single error type, matching
     * what the legacy `loadFeeForSwapTransaction` overload 2 does for DEX failures (line 1027 of
     * the original code).
     */
    private suspend fun loadDexSwapFee(
        fromStatus: SwapCurrencyStatus,
        swapData: SwapDataModel?,
        selectedFeeToken: CryptoCurrencyStatus?,
    ): Either<GetFeeError, SwapFee> {
        val transaction = swapData?.transaction as? ExpressTransactionModel.DEX
            ?: return GetFeeError.UnknownError.left()

        return dexSwapFeeCalculator.calculate(
            fromSwapCurrencyStatus = fromStatus,
            transaction = transaction,
            selectedToken = selectedFeeToken,
        ).fold(
            ifLeft = { GetFeeError.UnknownError.left() },
            ifRight = { dexFeeResult ->
                val feeToken = selectedFeeToken
                    ?: resolveNativeFeeTokenStatus(fromStatus)
                    ?: return@fold GetFeeError.UnknownError.left()
                SwapFeeFactory.from(
                    transactionFeeResult = dexFeeResult.transactionFee,
                    selectedFeeToken = feeToken,
                    otherNativeFee = dexFeeResult.otherNativeFee,
                    feeBucket = FeeBucket.MARKET,
                ).right()
            },
        )
    }

    /**
     * [REDACTED_TASK_KEY] — CEX branch of [loadSwapFee]. Native-fallback behaviour is preserved: when
     * [selectedFeeToken] is null the gasless use case (invoked inside [CexSwapFeeCalculator])
     * decides native vs token. The resulting `SwapFee.selectedFeeToken` is the explicit choice
     * if provided, otherwise the native coin status of the from-token's network.
     */
    private suspend fun loadCexSwapFee(
        fromStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        selectedFeeToken: CryptoCurrencyStatus?,
    ): Either<GetFeeError, SwapFee> {
        return cexSwapFeeCalculator.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = amount.value,
            selectedFeeToken = selectedFeeToken,
        ).fold(
            ifLeft = { it.left() },
            ifRight = { cexFeeResult ->
                val feeToken = selectedFeeToken
                    ?: resolveNativeFeeTokenStatus(fromStatus)
                    ?: return@fold GetFeeError.UnknownError.left()
                SwapFeeFactory.from(
                    transactionFeeResult = cexFeeResult.transactionFee,
                    selectedFeeToken = feeToken,
                    otherNativeFee = BigDecimal.ZERO,
                    feeBucket = FeeBucket.MARKET,
                ).right()
            },
        )
    }

    /**
     * [REDACTED_TASK_KEY] — resolves the native-coin [CryptoCurrencyStatus] for the from-token's network.
     * Used as the default `selectedFeeToken` of [SwapFee] when the caller did not provide an
     * explicit choice. Mirrors how `SwapModel.updateFeePaidCryptoCurrencyFor` populates
     * `dataState.feePaidCryptoCurrency`.
     */
    private suspend fun resolveNativeFeeTokenStatus(fromStatus: SwapCurrencyStatus): CryptoCurrencyStatus? {
        return getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWalletId = fromStatus.userWalletId,
            cryptoCurrencyStatus = fromStatus.status,
        ).getOrNull()
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 4. Patches an existing [SwapState.QuotesLoadedState] with a freshly
     * resolved [SwapFee]. See [SwapInteractor.applySwapFee] for the full contract.
     *
     * Numeric fee used for downstream computation:
     *  - If `fee.selectedFeeToken.currency` is a token → `0` for the balance/include-fee math when
     *    the fee currency differs from the from-token (matches legacy `manageWarnings` semantics
     *    at line 422 of the pre-Phase-4 code).
     *  - Otherwise → `fee.fee.amount.value + fee.otherNativeFee` (the bridge-aware native fee).
     *
     * The same `feeToCheck` is fed into `getFeeState`, `isBalanceEnough` and `getIncludeFeeInAmount`
     * for consistency with the legacy `loadDexSwapData` path.
     */
    override suspend fun applySwapFee(state: SwapState.QuotesLoadedState, fee: SwapFee): SwapState.QuotesLoadedState {
        val fromSwapCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus
        val amount = state.fromTokenInfo.tokenAmount
        val isFeeInToken = fee.selectedFeeToken.currency is CryptoCurrency.Token
        val nativeFee = (fee.fee.amount.value ?: BigDecimal.ZERO) + fee.otherNativeFee

        // Mirrors legacy manageWarnings: token-fee paths skip the native deduction.
        val warningsFee = if (isFeeInToken && fromSwapCurrencyStatus.currency.id != fee.selectedFeeToken.currency.id) {
            BigDecimal.ZERO
        } else {
            nativeFee
        }

        val feeState = getFeeState(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            fee = nativeFee,
            spendAmount = amount,
            selectedFeeToken = fee.selectedFeeToken,
        )
        val isBalanceIncludeFeeEnough = isBalanceEnough(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            fee = nativeFee,
        )
        val includeFeeInAmount = getIncludeFeeInAmount(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            reduceBalanceBy = BigDecimal.ZERO,
            feeValue = nativeFee,
            selectedFeeToken = fee.selectedFeeToken,
        )
        val currencyCheck = manageWarnings(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            fee = warningsFee,
            includeFeeInAmount = includeFeeInAmount,
        )
        val validationResult = manageTransactionValidationWarnings(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            feeValue = nativeFee,
        )
        val minAdaValue = (fee.fee as? Fee.CardanoToken)?.minAdaValue

        return state.copy(
            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                isBalanceEnough = isBalanceIncludeFeeEnough,
                feeState = feeState,
                includeFeeInAmount = includeFeeInAmount,
            ),
            currencyCheck = currencyCheck,
            validationResult = validationResult,
            minAdaValue = minAdaValue,
        )
    }

    private suspend fun storeLastCryptoCurrencyId(swapCurrencyStatus: SwapCurrencyStatus) {
        swapTransactionRepository.storeLastSwappedCryptoCurrencyId(
            userWalletId = swapCurrencyStatus.userWalletId,
            cryptoCurrencyId = swapCurrencyStatus.currency.id,
        )
    }

    override fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount {
        return SwapAmount(token.value.amount ?: BigDecimal.ZERO, token.currency.decimals)
    }

    override suspend fun getNativeToken(swapCurrencyStatus: SwapCurrencyStatus): CryptoCurrency {
        val network = swapCurrencyStatus.currency.network
        return multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(swapCurrencyStatus.userWalletId),
        )
            ?.filterIsInstance<CryptoCurrency.Coin>()
            ?.firstOrNull { nativeCoin ->
                nativeCoin.network.id == network.id &&
                    nativeCoin.network.derivationPath == network.derivationPath
            }
            ?: currenciesRepository.createCoinCurrency(network)
    }

    private suspend fun createEmptyAmountState(): SwapState {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        return SwapState.EmptyAmountState(
            zeroAmountEquivalent = stringReference(
                BigDecimal.ZERO.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
            ),
        )
    }

    /**
     * Load quote data calls only if spend is not allowed for token contract address.
     *
     * [REDACTED_TASK_KEY] — Phase 4: fee is no longer computed during quote load. The fee selector
     * (`FeeSelectorBlockComponent`) is the only fee owner. Initial state is `feeState = NotEnough()`
     * and `includeFeeInAmount = Excluded`. `applySwapFee` patches these values once the fee
     * selector resolves.
     */
    @Suppress("LongParameterList")
    private suspend fun loadCexQuoteData(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        provider: SwapProvider,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
    ): SwapState {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency
        return coroutineScope {
            val includeFeeInAmount = getIncludeFeeInAmount(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                feeValue = BigDecimal.ZERO,
            )

            val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                includeFeeInAmount.amountSubtractFee
            } else {
                amount
            }

            val quotes = repository.findBestQuote(
                userWallet = fromSwapCurrencyStatus.userWallet,
                fromContractAddress = fromToken.getContractAddress(),
                fromNetwork = fromToken.network.rawId,
                toContractAddress = toToken.getContractAddress(),
                toNetwork = toToken.network.rawId,
                fromAmount = amountToRequest.toStringWithRightOffset(),
                fromDecimals = amount.decimals,
                toDecimals = toToken.decimals,
                providerId = provider.providerId,
                rateType = RateType.FLOAT,
            )

            getQuotesState(
                provider = provider,
                quoteDataModel = quotes,
                amount = amount,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                includeFeeInAmount = includeFeeInAmount,
            )
        }
    }

    // [REDACTED_TASK_KEY]: redesign in progress — do not extend. Unreachable from production after Phase 4.
    @Suppress("UnusedPrivateMember")
    private suspend fun updateTxFeeStateIfNeededForCEX(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        txFeeSealedState: TxFeeSealedState,
        amount: SwapAmount,
    ): TxFeeSealedState {
        return when (txFeeSealedState) {
            is TxFeeSealedState.Component -> txFeeSealedState
            is TxFeeSealedState.Legacy -> {
                if (txFeeSealedState.txFeeState is TxFeeState.Empty) {
                    val txFeeResult = estimateFeeUseCase(
                        amount = amount.value,
                        userWallet = fromSwapCurrencyStatus.userWallet,
                        cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
                    )
                    val txFee = getFeeForCex(txFeeResult, fromSwapCurrencyStatus)

                    TxFeeSealedState.Legacy(
                        txFeeState = txFee,
                        selectedFee = txFeeSealedState.selectedFee,
                    )
                } else {
                    txFeeSealedState
                }
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun getQuotesState(
        provider: SwapProvider,
        quoteDataModel: Either<ExpressDataError, QuoteModel>,
        amount: SwapAmount,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
        includeFeeInAmount: IncludeFeeInAmount,
    ): SwapState {
        return quoteDataModel.fold(
            ifRight = { quoteModel ->
                val swapState = updateBalances(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    fromTokenAmount = amount,
                    toTokenAmount = quoteModel.toTokenAmount,
                    swapData = null,
                    provider = provider,
                ).copy(
                    currencyCheck = manageWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        fee = BigDecimal.ZERO,
                        includeFeeInAmount = includeFeeInAmount,
                    ),
                    validationResult = manageTransactionValidationWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        feeValue = BigDecimal.ZERO,
                    ),
                    minAdaValue = null,
                )

                when (provider.type) {
                    ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                        val state = updatePermissionState(
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            quotesLoadedState = swapState,
                            isAllowedToSpend = isAllowedToSpend,
                            swapAmount = amount,
                            quoteModel = quoteModel,
                        )
                        if (state !is SwapState.QuotesLoadedState) return state
                        state.copy(
                            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                                isBalanceEnough = isBalanceWithoutFeeEnough,
                            ),
                        )
                    }
                    ExchangeProviderType.CEX -> {
                        swapState.copy(
                            permissionState = PermissionDataState.Empty,
                            preparedSwapConfigState = PreparedSwapConfigState(
                                feeState = SwapFeeState.NotEnough(),
                                isBalanceEnough = isBalanceWithoutFeeEnough,
                                hasOutgoingTransaction = hasOutgoingTransaction(fromSwapCurrencyStatus.status),
                                includeFeeInAmount = includeFeeInAmount,
                            ),
                        )
                    }
                }
            },
            ifLeft = { error ->
                createSwapErrorWith(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    amount = amount,
                    includeFeeInAmount = includeFeeInAmount,
                    expressDataError = error,
                )
            },
        )
    }

    private suspend fun createSwapErrorWith(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        includeFeeInAmount: IncludeFeeInAmount,
        expressDataError: ExpressDataError,
    ): SwapState.SwapError {
        val rates = getQuotes(fromSwapCurrencyStatus.currency.id)
        val fromTokenSwapInfo = TokenSwapInfo(
            swapCurrencyStatus = fromSwapCurrencyStatus,
            tokenAmount = amount,
            amountFiat = rates[fromSwapCurrencyStatus.currency.id]?.fiatRate?.multiply(amount.value) ?: BigDecimal.ZERO,
        )
        return SwapState.SwapError(fromTokenSwapInfo, expressDataError, includeFeeInAmount)
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 4. Computes `IncludeFeeInAmount` for a given numeric fee value paid in
     * [selectedFeeToken].
     *
     * Branches:
     *  - [selectedFeeToken] is the same currency as [fromSwapCurrencyStatus] (and not a coin) →
     *    same-currency-token path: balance check on the from-token's own balance.
     *  - Otherwise → native-fee branch via [getIncludeFeeInAmountForNative].
     *
     * Used both by [loadCexQuoteData] (with `feeValue = ZERO` at quote stage) and by
     * [applySwapFee] (with the actual fee once the selector resolves).
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "CastNullableToNonNullableType", "LongParameterList")
    private suspend fun getIncludeFeeInAmount(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
        selectedFeeToken: CryptoCurrencyStatus? = null,
    ): IncludeFeeInAmount {
        val isFeeInSameCurrencyToken = selectedFeeToken != null &&
            fromSwapCurrencyStatus.currency.id == selectedFeeToken.currency.id &&
            selectedFeeToken.currency is CryptoCurrency.Token

        return if (isFeeInSameCurrencyToken) {
            // we have a token selected for fee payment the same as sending token
            val reducedBalance = fromSwapCurrencyStatus.status.value.amount as BigDecimal - reduceBalanceBy
            when {
                amount.value > reducedBalance -> IncludeFeeInAmount.BalanceNotEnough
                amount.value + feeValue <= reducedBalance -> IncludeFeeInAmount.Excluded
                else -> {
                    if (feeValue < amount.value) {
                        IncludeFeeInAmount.Included(
                            amountSubtractFee = SwapAmount(
                                value = reducedBalance - feeValue,
                                decimals = fromSwapCurrencyStatus.currency.decimals,
                            ),
                        )
                    } else {
                        IncludeFeeInAmount.Excluded
                    }
                }
            }
        } else {
            getIncludeFeeInAmountForNative(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                feeValue = feeValue,
            )
        }
    }

    private suspend fun getIncludeFeeInAmountForNative(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
    ): IncludeFeeInAmount {
        return when (val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)) {
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > feeValue) {
                    IncludeFeeInAmount.Excluded
                } else {
                    IncludeFeeInAmount.BalanceNotEnough
                }
            }
            else -> getIncludeFeeAmountForCoinFee(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                feeValue = feeValue,
            )
        }
    }

    private suspend fun getIncludeFeeAmountForCoinFee(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
    ): IncludeFeeInAmount {
        val networkId = fromSwapCurrencyStatus.currency.network.rawId
        val tokenForFeeBalance = walletManagersFacade.getNativeTokenBalance(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            networkId = networkId,
            derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
        )
        val reducedBalance = tokenForFeeBalance - reduceBalanceBy
        val amountWithFee = amount.value + feeValue
        return when {
            fromSwapCurrencyStatus.currency is CryptoCurrency.Token -> {
                if (feeValue > reducedBalance || reducedBalance.signum() == 0) {
                    IncludeFeeInAmount.BalanceNotEnough
                } else {
                    IncludeFeeInAmount.Excluded
                }
            }
            amount.value > reducedBalance -> {
                IncludeFeeInAmount.BalanceNotEnough
            }
            amountWithFee <= reducedBalance -> {
                IncludeFeeInAmount.Excluded
            }
            else -> {
                if (feeValue < amount.value) {
                    val nativeCoinDecimals =
                        Blockchain.fromNetworkId(networkId)?.decimals() ?: error("Blockchain not found")
                    IncludeFeeInAmount.Included(
                        amountSubtractFee = SwapAmount(
                            reducedBalance - feeValue,
                            nativeCoinDecimals,
                        ),
                    )
                } else {
                    IncludeFeeInAmount.BalanceNotEnough
                }
            }
        }
    }

    private suspend fun getFormattedFiatFees(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        vararg fees: BigDecimal,
    ): List<String> {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        val feeCurrencyId: CryptoCurrency.ID = when (val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)) {
            is FeePaidCurrency.Token -> feePaidCurrency.tokenId
            else -> getNativeToken(fromSwapCurrencyStatus).id
        }
        val rates = getQuotes(feeCurrencyId)
        return rates[feeCurrencyId]?.let { rate ->
            fees.map { fee ->
                rate.fiatRate.multiply(fee).format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                }
            }
        }.orEmpty()
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 4. DEX-swap-data loader that **does not** compute a fee.
     *
     * The fee is owned exclusively by the fee selector (`FeeSelectorBlockComponent`). This method
     * fetches the swap data via [SwapRepository.getExchangeData], populates `swapDataModel`, and
     * returns an initial [SwapState.QuotesLoadedState] with:
     *  - `txFee = TxFeeState.Empty` (legacy field; removed in Phase 5).
     *  - `preparedSwapConfigState.feeState = SwapFeeState.NotEnough()` — transient until
     *    `applySwapFee` is called.
     *  - `preparedSwapConfigState.isBalanceEnough = true` only if balance covers the amount;
     *    the *fee*-inclusive check is deferred to `applySwapFee`.
     *  - `currencyCheck`, `validationResult`, `minAdaValue` populated with `fee = 0` (re-derived
     *    once the fee is known).
     *
     * Replaces the legacy `loadDexSwapData` at all DEX call sites in `findBestQuote`. The legacy
     * method stays in source through Phase 4 (already removed in this commit) and is unreachable
     * from production callers.
     */
    @Suppress("LongParameterList", "LongMethod")
    private suspend fun loadDexSwapDataNoFee(
        provider: SwapProvider,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        expressOperationType: ExpressOperationType,
    ): SwapState {
        val fromNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val dexFromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty()
        val toNetworkAddress = toSwapCurrencyStatus.status.value.networkAddress
        val dexToAddress = toNetworkAddress?.defaultAddress?.value.orEmpty()
        return repository.getExchangeData(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            fromAddress = dexFromAddress,
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
            toAddress = dexToAddress,
            refundAddress = fromNetworkAddress?.defaultAddress?.value,
            expressOperationType = expressOperationType,
        ).fold(
            ifRight = { swapData ->
                val includeFeeInAmount = IncludeFeeInAmount.Excluded // exclude for dex
                val isBalanceIncludeFeeEnough = isBalanceEnough(fromSwapCurrencyStatus, amount, BigDecimal.ZERO)
                val preparedSwapConfigState = PreparedSwapConfigState(
                    isBalanceEnough = isBalanceIncludeFeeEnough,
                    feeState = SwapFeeState.NotEnough(),
                    hasOutgoingTransaction = hasOutgoingTransaction(fromSwapCurrencyStatus.status),
                    includeFeeInAmount = includeFeeInAmount,
                )
                val swapState = updateBalances(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    fromTokenAmount = amount,
                    toTokenAmount = swapData.toTokenAmount,
                    swapData = swapData,
                    provider = provider,
                )
                swapState.copy(
                    permissionState = PermissionDataState.Empty,
                    currencyCheck = manageWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        fee = BigDecimal.ZERO,
                        includeFeeInAmount = includeFeeInAmount,
                    ),
                    validationResult = manageTransactionValidationWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        feeValue = BigDecimal.ZERO,
                    ),
                    preparedSwapConfigState = preparedSwapConfigState,
                )
            },
            ifLeft = { error ->
                produceDexSwapDataError(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    error = error,
                    amount = amount,
                )
            },
        )
    }

    // [REDACTED_TASK_KEY]: redesign in progress — do not extend
    private suspend fun loadFeeForDex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
    ): Either<ExpressDataError, TransactionFee> = either {
        if (isSolana(fromSwapCurrencyStatus.currency.network.rawId)) {
            val transactionBytes = Base64.decode(transaction.txData, Base64.NO_WRAP)

            val formattedHash = getFormattedHash(transactionBytes)

            if (formattedHash.size > SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES &&
                fromSwapCurrencyStatus.userWallet is UserWallet.Cold
            ) {
                raise(ExpressDataError.TooLargeSolanaTransactionError)
            }

            getFeeDataForSolanaDexSwap(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                transactionBytes = transactionBytes,
            )
        } else {
            getFeeDataForDexSwap(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                transaction = transaction,
            ).map { fee ->
                (fee as TransactionFeeResult.Loaded).fee.patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_DEX)
            }.bind()
        }
    }

    private suspend fun produceDexSwapDataError(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        error: ExpressDataError,
        amount: SwapAmount,
    ): SwapState.SwapError {
        val rates = getQuotes(fromSwapCurrencyStatus.currency.id)
        val fromTokenSwapInfo = TokenSwapInfo(
            swapCurrencyStatus = fromSwapCurrencyStatus,
            tokenAmount = amount,
            amountFiat = rates[fromSwapCurrencyStatus.currency.id]?.fiatRate?.multiply(amount.value) ?: BigDecimal.ZERO,
        )
        return SwapState.SwapError(
            fromTokenSwapInfo,
            error,
            IncludeFeeInAmount.Excluded,
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun getFeeDataForDexSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        selectedToken: CryptoCurrencyStatus? = null,
    ): Either<ExpressDataError, TransactionFeeResult> = either {
        val nativeBalance = walletManagersFacade.getNativeTokenBalance(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            networkId = fromSwapCurrencyStatus.currency.network.rawId,
            derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
        )

        // if native balance is zero - we can't calculate fee
        if (nativeBalance.signum() == 0) {
            raise(ExpressDataError.UnknownError)
        }

        try {
            val txAmountValue = transaction.txValue ?: error("unable to get txValue")
            val amountToSend = createNativeAmountForDex(txAmountValue, fromSwapCurrencyStatus.currency.network)

            // transaction.txValue is always native coin
            if (nativeBalance < amountToSend.value) {
                error("It's impossible to calculate fee for nativeBalance.value < amountToSend.value")
            }

            val extras = createTransactionExtrasUseCase(
                data = transaction.txData,
                network = fromSwapCurrencyStatus.currency.network,
            ).getOrNull() ?: error("unable to create extras")

            val transactionData = TransactionData.Uncompiled(
                amount = amountToSend,
                destinationAddress = transaction.txTo,
                fee = null,
                sourceAddress = transaction.txFrom,
                extras = extras,
            )
            if (selectedToken != null && selectedToken.currency is CryptoCurrency.Token) {
                getFeeForTokenUseCase(
                    transactionData = transactionData,
                    token = selectedToken.currency,
                    userWallet = fromSwapCurrencyStatus.userWallet,
                ).getOrNull()?.let { TransactionFeeResult.LoadedExtended(it) }
                    ?: error("unable to calculate fee for token")
            } else {
                getFeeUseCase(
                    transactionData = transactionData,
                    network = fromSwapCurrencyStatus.currency.network,
                    userWallet = fromSwapCurrencyStatus.userWallet,
                ).getOrNull()?.let { TransactionFeeResult.Loaded(it) } ?: error("unable to calculate fee")
            }
        } catch (_: IllegalStateException) {
            getEthSpecificFeeUseCase(
                userWallet = fromSwapCurrencyStatus.userWallet,
                cryptoCurrency = fromSwapCurrencyStatus.currency,
                gasLimit = transaction.gas,
            ).getOrNull()?.let { TransactionFeeResult.Loaded(it) }
                ?: error("can't get fee for getEthSpecificFeeUseCase")
        }
    }

    private suspend fun getFeeDataForSolanaDexSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transactionBytes: ByteArray,
    ): TransactionFee {
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(transactionBytes),
        )

        return getFeeUseCase(
            transactionData = transactionData,
            network = fromSwapCurrencyStatus.currency.network,
            userWallet = fromSwapCurrencyStatus.userWallet,
        ).getOrNull() ?: error("unable to calculate fee")
    }

    @Suppress("LongParameterList", "MaxChainedCallsOnSameLine")
    private suspend fun updateBalances(
        provider: SwapProvider,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: SwapAmount,
        toTokenAmount: SwapAmount,
        swapData: SwapDataModel?,
    ): SwapState.QuotesLoadedState {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency
        val nativeToken = getNativeToken(fromSwapCurrencyStatus)
        val rates = getQuotes(fromToken.id, toToken.id, nativeToken.id)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                swapCurrencyStatus = fromSwapCurrencyStatus,
                amountFiat = rates[fromToken.id]?.fiatRate?.multiply(fromTokenAmount.value) ?: BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                swapCurrencyStatus = toSwapCurrencyStatus,
                amountFiat = rates[toToken.id]?.fiatRate?.multiply(toTokenAmount.value) ?: BigDecimal.ZERO,
            ),
            priceImpact = calculatePriceImpact(
                fromTokenAmount = fromTokenAmount.value,
                fromQuoteStatus = rates[fromToken.id],
                toTokenAmount = toTokenAmount.value,
                toQuoteStatus = rates[toToken.id],
            ),
            swapDataModel = swapData,
            swapProvider = provider,
            txFee = TxFeeState.Empty, // legacy field; populated by applySwapFee in Phase 5 removal
            minAdaValue = null,
        )
    }

    // [REDACTED_TASK_KEY]: redesign in progress — do not extend
    private suspend fun getFeeForCex(
        txFeeResult: Either<GetFeeError, TransactionFee>?,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
    ): TxFeeState {
        return txFeeResult?.fold(
            ifLeft = { TxFeeState.Empty },
            ifRight = { txFee ->
                txFee.patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_SEND).toTxFeeState(fromSwapCurrencyStatus, null)
            },
        ) ?: TxFeeState.Empty
    }

    private suspend fun updatePermissionState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        swapAmount: SwapAmount,
        quotesLoadedState: SwapState.QuotesLoadedState,
        quoteModel: QuoteModel,
        isAllowedToSpend: Boolean,
    ): SwapState {
        val fromToken = fromSwapCurrencyStatus.currency
        if (isAllowedToSpend) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        // if token balance ZERO not show permission state to avoid user to spend money for fee
        val isTokenZeroBalance = getTokenBalance(fromSwapCurrencyStatus.status).value.signum() == 0
        if (isTokenZeroBalance) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        if (allowPermissionsHandler.isAddressAllowanceInProgress(getTokenAddress(fromToken))) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.PermissionLoading,
            )
        }

        val allowanceInfo = getAllowanceInfoUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            cryptoCurrency = fromToken,
            spenderAddress = requireNotNull(quoteModel.allowanceContract) { "spenderAddress cant be null" },
            requiredAmount = swapAmount.value,
        ).getOrNull()

        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionRequired(
                isResetApproval = allowanceInfo is AllowanceInfo.ResetNeeded,
                spenderAddress = quoteModel.allowanceContract,
            ),
        )
    }

    @Suppress("LongMethod")
    private suspend fun TransactionFee.toTxFeeState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        otherNativeFee: BigDecimal?,
    ): TxFeeState {
        val otherNativeFeeValue = otherNativeFee ?: BigDecimal.ZERO
        return when (this) {
            is TransactionFee.Choosable -> {
                val feeNormal = this.normal.amount.value ?: BigDecimal.ZERO
                val feePriority = this.priority.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(fromSwapCurrencyStatus, feeNormal)[0]
                val priorityFiatValue = getFormattedFiatFees(fromSwapCurrencyStatus, feePriority)[0]

                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = this.normal.amount.decimals,
                )
                val priorityCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feePriority,
                    decimals = this.priority.amount.decimals,
                )

                // region otherNativeFee
                val normalFeeWithOtherNative = feeNormal + otherNativeFeeValue
                val priorityFeeWithOtherNative = feePriority + otherNativeFeeValue
                val normalFiatValueWithNative =
                    getFormattedFiatFees(fromSwapCurrencyStatus, normalFeeWithOtherNative)[0]
                val priorityFiatValueWithNative =
                    getFormattedFiatFees(fromSwapCurrencyStatus, priorityFeeWithOtherNative)[0]

                val normalCryptoFeeWithNative = amountFormatter.formatBigDecimalAmountToUI(
                    amount = normalFeeWithOtherNative,
                    decimals = this.normal.amount.decimals,
                )
                val priorityCryptoFeeWithNative = amountFormatter.formatBigDecimalAmountToUI(
                    amount = priorityFeeWithOtherNative,
                    decimals = this.priority.amount.decimals,
                )
                // endregion
                TxFeeState.MultipleFeeState(
                    normalFee = TxFee.Legacy(
                        feeValue = feeNormal,
                        feeFiatFormatted = normalFiatValue,
                        feeCryptoFormatted = normalCryptoFee,
                        feeIncludeOtherNativeFee = normalFeeWithOtherNative,
                        feeFiatFormattedWithNative = normalFiatValueWithNative,
                        feeCryptoFormattedWithNative = normalCryptoFeeWithNative,
                        cryptoSymbol = this.normal.amount.currencySymbol,
                        feeType = FeeType.NORMAL,
                        fee = this.normal,
                    ),
                    priorityFee = TxFee.Legacy(
                        feeValue = feePriority,
                        feeFiatFormatted = priorityFiatValue,
                        feeCryptoFormatted = priorityCryptoFee,
                        feeIncludeOtherNativeFee = priorityFeeWithOtherNative,
                        feeFiatFormattedWithNative = priorityFiatValueWithNative,
                        feeCryptoFormattedWithNative = priorityCryptoFeeWithNative,
                        cryptoSymbol = this.priority.amount.currencySymbol,
                        feeType = FeeType.PRIORITY,
                        fee = this.priority,
                    ),
                )
            }
            is TransactionFee.Single -> {
                val feeNormal = this.normal.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(fromSwapCurrencyStatus, feeNormal)[0]
                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = this.normal.amount.decimals,
                )
                // region otherNativeFee
                val normalFeeWithOtherNative = feeNormal + otherNativeFeeValue
                val normalFiatValueWithNative =
                    getFormattedFiatFees(fromSwapCurrencyStatus, normalFeeWithOtherNative)[0]

                val normalCryptoFeeWithNative = amountFormatter.formatBigDecimalAmountToUI(
                    amount = normalFeeWithOtherNative,
                    decimals = this.normal.amount.decimals,
                )
                // endregion
                TxFeeState.SingleFeeState(
                    fee = TxFee.Legacy(
                        feeValue = this.normal.amount.value ?: BigDecimal.ZERO,
                        feeFiatFormatted = normalFiatValue,
                        feeCryptoFormatted = normalCryptoFee,
                        feeIncludeOtherNativeFee = normalFeeWithOtherNative,
                        feeFiatFormattedWithNative = normalFiatValueWithNative,
                        feeCryptoFormattedWithNative = normalCryptoFeeWithNative,
                        cryptoSymbol = normal.amount.currencySymbol,
                        feeType = FeeType.NORMAL,
                        fee = this.normal,
                    ),
                )
            }
        }
    }

    private fun createNativeAmountForDex(txValueAmount: String, network: Network): Amount {
        val nativeDecimals = Blockchain.fromNetworkId(network.rawId)?.decimals()
            ?: error("Blockchain not found")
        val decimalValue = txValueAmount.toBigDecimalOrNull()?.movePointLeft(nativeDecimals)
            ?: error("txValue parse error")
        return Amount(
            currencySymbol = network.currencySymbol,
            value = decimalValue,
            decimals = nativeDecimals,
        )
    }

    /**
     * We need to increase gasLimit for Ethereum fees for 2 cases
     *
     * DEX: for dex calculated gasLimit for given data might be changed when transaction processing
     * for that case dex providers recommend to increase gasLimit for few percents to ensure transaction completes
     *
     * CEX: for that case we calculate fee for random generated address and gasLimit might be different for it
     * and result address to send. That's why we should increase gasLimit a little
     *
     */
    private fun TransactionFee.patchTransactionFeeForSwap(increaseBy: Int): TransactionFee {
        return when (this) {
            is TransactionFee.Choosable -> {
                this.copy(
                    minimum = this.minimum.increaseEthGasLimitInNeeded(increaseBy),
                    normal = this.normal.increaseEthGasLimitInNeeded(increaseBy),
                    priority = this.priority.increaseEthGasLimitInNeeded(increaseBy),
                )
            }
            is TransactionFee.Single -> this.copy(normal = this.normal.increaseEthGasLimitInNeeded(increaseBy))
        }
    }

    private fun Fee.increaseEthGasLimitInNeeded(increaseBy: Int): Fee {
        return when (this) {
            is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
            is Fee.Ethereum.EIP1559,
            is Fee.Ethereum.Legacy,
            -> this.increaseGasLimitBy(increaseBy)
            is Fee.Alephium,
            is Fee.Aptos,
            is Fee.Bitcoin,
            is Fee.CardanoToken,
            is Fee.Common,
            is Fee.Filecoin,
            is Fee.Hedera,
            is Fee.Kaspa,
            is Fee.Sui,
            is Fee.Tron,
            is Fee.VeChain,
            -> this
        }
    }

    /**
     * Increase gasLimit for Fee.Ethereum
     */
    private fun Fee.increaseGasLimitBy(percentage: Int): Fee {
        if (this !is Fee.Ethereum) return this
        val gasLimit = this.gasLimit
        val increasedGasPrice = this.amount.value?.movePointRight(this.amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit.multiply(percentage.toBigInteger()).divide(hundredPercent)
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
        )
        return when (this) {
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
        }
    }

    private fun hasOutgoingTransaction(cryptoCurrencyStatuses: CryptoCurrencyStatus): Boolean {
        return cryptoCurrencyStatuses.value.pendingTransactions.any { it.isOutgoing }
    }

    private fun Fee.getGasLimit(): Int? {
        return when (this) {
            is Fee.Ethereum -> gasLimit.toInt()
            is Fee.VeChain -> gasLimit.toInt()
            is Fee.Aptos -> gasLimit.toInt()
            is Fee.Filecoin -> gasLimit.toInt()
            else -> null
        }
    }

    // [REDACTED_TASK_KEY]: legacy helper. Unreachable from production after Phase 4. Deleted in Phase 5.
    @Suppress("UnusedPrivateMember")
    private fun selectFeeByType(feeType: FeeType, txFeeState: TxFeeState): BigDecimal {
        return when (txFeeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.SingleFeeState -> txFeeState.fee.fee.amount.value
            is TxFeeState.MultipleFeeState -> when (feeType) {
                FeeType.NORMAL -> txFeeState.normalFee.fee.amount.value
                FeeType.PRIORITY -> txFeeState.priorityFee.fee.amount.value
            }
        } ?: BigDecimal.ZERO
    }

    private suspend fun isBalanceEnough(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(fromSwapCurrencyStatus.status).value
        val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)
        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> tokenBalance >= amount.value
            else -> {
                if (fromSwapCurrencyStatus.currency is CryptoCurrency.Token) {
                    tokenBalance >= amount.value
                } else {
                    tokenBalance >= amount.value.plus(fee ?: BigDecimal.ZERO)
                }
            }
        }
    }

    private suspend fun getFeePaidCurrency(swapCurrencyStatus: SwapCurrencyStatus): FeePaidCurrency {
        return currenciesRepository.getFeePaidCurrency(
            userWalletId = swapCurrencyStatus.userWalletId,
            network = swapCurrencyStatus.currency.network,
        )
    }

    private fun getTokenAddress(currency: CryptoCurrency): String {
        return when (currency) {
            is CryptoCurrency.Coin -> {
                "0"
            }
            is CryptoCurrency.Token -> {
                currency.contractAddress
            }
        }
    }

    private fun toBigDecimalOrNull(amountToSwap: String): BigDecimal? {
        return amountToSwap.replace(",", ".").toBigDecimalOrNull()
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun getFeeState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        selectedFeeToken: CryptoCurrencyStatus? = null,
    ): SwapFeeState {
        if (fee == null) {
            return SwapFeeState.NotEnough()
        }
        val fromCurrency = fromSwapCurrencyStatus.currency
        val percentsToFeeIncrease = BigDecimal.ONE
        // [REDACTED_TASK_KEY] — Phase 4: when the user explicitly picked a non-native fee token (gasless flow),
        // the balance check must verify the chosen token's balance, not the network's native coin.
        if (selectedFeeToken != null && selectedFeeToken.currency is CryptoCurrency.Token) {
            val feeTokenBalance = selectedFeeToken.value.amount ?: BigDecimal.ZERO
            return if (feeTokenBalance > fee.multiply(percentsToFeeIncrease)) {
                SwapFeeState.Enough
            } else {
                SwapFeeState.NotEnough(
                    currencyName = selectedFeeToken.currency.name,
                    currencySymbol = selectedFeeToken.currency.symbol,
                )
            }
        }
        return when (val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)) {
            FeePaidCurrency.Coin -> {
                val nativeTokenBalance = walletManagersFacade.getNativeTokenBalance(
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    networkId = fromCurrency.network.rawId,
                    derivationPath = fromCurrency.network.derivationPath.value,
                )

                val balanceToCheck = when (fromCurrency) {
                    is CryptoCurrency.Token -> nativeTokenBalance
                    is CryptoCurrency.Coin -> {
                        // need to check balance minus amount only if amount to swap in native token
                        nativeTokenBalance.minus(spendAmount.value)
                    }
                }
                if (balanceToCheck > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    val nativeToken = getNativeToken(fromSwapCurrencyStatus)
                    SwapFeeState.NotEnough(
                        currencyName = nativeToken.network.name,
                        currencySymbol = nativeToken.symbol,
                    )
                }
            }
            FeePaidCurrency.SameCurrency -> {
                val balance = fromSwapCurrencyStatus.status.value.amount ?: return SwapFeeState.NotEnough()
                if (balance.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    SwapFeeState.NotEnough(
                        currencyName = fromCurrency.name,
                        currencySymbol = fromCurrency.symbol,
                    )
                }
            }
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    SwapFeeState.NotEnough(
                        currencyName = feePaidCurrency.name,
                        currencySymbol = feePaidCurrency.symbol,
                    )
                }
            }
            is FeePaidCurrency.FeeResource -> {
                val isFeeResourceEnough = currencyChecksRepository.checkIfFeeResourceEnough(
                    amount = spendAmount.value,
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    network = fromCurrency.network,
                )

                if (isFeeResourceEnough) {
                    SwapFeeState.Enough
                } else {
                    SwapFeeState.NotEnough()
                }
            }
        }
    }

    private fun calculatePriceImpact(
        fromTokenAmount: BigDecimal,
        fromQuoteStatus: QuoteStatus.Data?,
        toTokenAmount: BigDecimal,
        toQuoteStatus: QuoteStatus.Data?,
    ): PriceImpact {
        if (fromQuoteStatus == null) return PriceImpact.Empty

        val toRate = toQuoteStatus?.fiatRate
        val fromTokenFiatValue = fromTokenAmount.multiply(fromQuoteStatus.fiatRate)
        val toTokenFiatValue = toRate?.let { toTokenAmount.multiply(toRate) } ?: return PriceImpact.Empty

        val value = BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)

        val fromAmountUSD = if (fromQuoteStatus.fiatRateUSD != BigDecimal.ZERO) {
            fromTokenAmount.multiply(fromQuoteStatus.fiatRateUSD)
        } else {
            PRICE_IMPACT_AMOUNT_MIN_THRESHOLD
        }
        val toAmountUSD = if (toQuoteStatus.fiatRateUSD != BigDecimal.ZERO) {
            toTokenAmount.multiply(toQuoteStatus.fiatRateUSD)
        } else {
            PRICE_IMPACT_AMOUNT_MIN_THRESHOLD
        }
        val amountDiff = fromAmountUSD - toAmountUSD

        val amountSignificance = when {
            fromAmountUSD <= PRICE_IMPACT_AMOUNT_MIN_THRESHOLD -> PriceImpact.AmountSignificance.LOW
            fromAmountUSD > PRICE_IMPACT_AMOUNT_MAX_THRESHOLD -> PriceImpact.AmountSignificance.HIGH
            else -> PriceImpact.AmountSignificance.MEDIUM
        }

        val type = when {
            value < PRICE_IMPACT_LOW_THRESHOLD &&
                amountDiff <= PRICE_IMPACT_AMOUNT_LOW_THRESHOLD -> PriceImpact.Type.LOW
            value > PRICE_IMPACT_HIGH_THRESHOLD -> PriceImpact.Type.HIGH
            else -> PriceImpact.Type.MEDIUM
        }

        return PriceImpact(
            value = value,
            amountSignificance = amountSignificance,
            type = type,
        )
    }

    private suspend fun getQuotes(vararg ids: CryptoCurrency.ID): Map<CryptoCurrency.ID, QuoteStatus.Data> {
        val set = ids.mapNotNullTo(destination = hashSetOf(), transform = CryptoCurrency.ID::rawCurrencyId)
            .getQuotesOrEmpty()

        return ids.mapNotNull { id ->
            val found = set.find { it.rawCurrencyId == id.rawCurrencyId && it.value is QuoteStatus.Data }
                ?: return@mapNotNull null

            id to found.value as QuoteStatus.Data
        }.toMap()
    }

    private suspend fun Set<CryptoCurrency.RawID>.getQuotesOrEmpty(): Set<QuoteStatus> {
        return runSuspendCatching {
            val cachedQuotes = quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = this@getQuotesOrEmpty)

            val areAllQuotesFound = cachedQuotes?.all { quote -> quote.value !is QuoteStatus.Empty } == true

            if (areAllQuotesFound) return@runSuspendCatching cachedQuotes

            val currenciesIds = if (cachedQuotes.isNullOrEmpty()) {
                this@getQuotesOrEmpty
            } else {
                cachedQuotes.mapNotNullTo(hashSetOf()) { quote ->
                    if (quote.value is QuoteStatus.Empty) quote.rawCurrencyId else null
                }
            }

            multiQuoteStatusFetcher(
                params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null),
            )

            quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = this@getQuotesOrEmpty).orEmpty()
        }.getOrElse { e ->
            TangemLogger.e("Failed to get quotes: ${e.message.orEmpty()}", e)
            emptySet()
        }
    }

    private fun isSolana(networkId: String): Boolean {
        return networkId == Blockchain.Solana.toNetworkId()
    }

    // TODO create usecase [REDACTED_TASK_KEY]
    private fun getFormattedHash(hash: ByteArray): ByteArray {
        return try {
            SolanaTransactionHelper.removeSignaturesPlaceholders(hash)
        } catch (e: Exception) {
            TangemLogger.e("Failed to format the hash: ${e.message.orEmpty()}", e)
            hash
        }
    }

    private fun getPayoutAddress(txData: TransactionData.Uncompiled): String {
        val ethereumCallData = (txData.extras as? EthereumTransactionExtras)?.callData
        return if (ethereumCallData is EthereumYieldSupplySendCallData) {
            ethereumCallData.destinationAddress
        } else {
            txData.destinationAddress
        }
    }

    // region temporary. will be removed
    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }

    private fun ExpressProvider.toSwapProvider(): SwapProvider {
        return SwapProvider(
            providerId = providerId,
            rateTypes = rateTypes.map { rateType ->
                when (rateType) {
                    ExpressRateType.Float -> RateType.FLOAT
                    ExpressRateType.Fixed -> RateType.FIXED
                }
            },
            name = name,
            type = when (type) {
                ExpressProviderType.DEX -> ExchangeProviderType.DEX
                ExpressProviderType.CEX -> ExchangeProviderType.CEX
                ExpressProviderType.DEX_BRIDGE -> ExchangeProviderType.DEX_BRIDGE
                ExpressProviderType.ONRAMP -> error("Invalid provider type")
            },
            imageLarge = imageLarge,
            termsOfUse = termsOfUse,
            privacyPolicy = privacyPolicy,
            isRecommended = isRecommended,
            slippage = slippage,
            isExtraIdSupported = isExtraIdSupported,
        )
    }
    // endregion

    companion object {
        private const val INCREASE_GAS_LIMIT_FOR_DEX = 112 // 12%
        private const val INCREASE_GAS_LIMIT_FOR_SEND = 105 // 5%
        private val PRICE_IMPACT_AMOUNT_MIN_THRESHOLD = 25.toBigDecimal() // in USD
        private val PRICE_IMPACT_AMOUNT_MAX_THRESHOLD = 5000.toBigDecimal() // in USD
        private val PRICE_IMPACT_AMOUNT_LOW_THRESHOLD = 100_000.toBigDecimal() // in USD
        private val PRICE_IMPACT_LOW_THRESHOLD = 0.1.toBigDecimal() // 10%
        private val PRICE_IMPACT_HIGH_THRESHOLD = 0.5.toBigDecimal() // 50%
    }
}

// [REDACTED_TASK_KEY]: redesign in progress — do not extend
sealed class TxFeeSealedState {
    class Legacy(val txFeeState: TxFeeState, val selectedFee: FeeType) : TxFeeSealedState()
    class Component(val txFee: TxFee.FeeComponent) : TxFeeSealedState()
}