package com.tangem.feature.swap.domain

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.ProxyAmount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import android.util.Base64
import com.tangem.blockchainsdk.utils.toNetworkId

@Suppress("LargeClass", "LongParameterList")
internal class SwapInteractorImpl @AssistedInject constructor(
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val quotesRepository: QuotesRepository,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val appCurrencyRepository: AppCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val initialToCurrencyResolver: InitialToCurrencyResolver,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getEthSpecificFeeUseCase: GetEthSpecificFeeUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val getAssetRequirementsUseCase: GetAssetRequirementsUseCase,
    private val amountFormatter: AmountFormatter,
    private val rampStateManager: RampStateManager,
    @Assisted private val userWalletId: UserWalletId,
) : SwapInteractor {

    private val getSelectedAppCurrencyUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }

    private val hundredPercent = BigInteger("100")

    private val userWallet
        get() = getUserWalletUseCase(userWalletId).getOrElse {
            error("Failed to get user wallet")
        }

    override suspend fun getTokensDataState(currency: CryptoCurrency): TokensDataStateExpress {
        val walletCurrencyStatuses = getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(userWalletId)
            .getOrElse { emptyList() }

        val walletCurrencyStatusesExceptInitial = walletCurrencyStatuses
            .filter {
                val currencyFilter = it.currency.network.backendId != currency.network.backendId ||
                    it.currency.getContractAddress() != currency.getContractAddress()
                val statusFilter = it.value is CryptoCurrencyStatus.Loaded || it.value is CryptoCurrencyStatus.NoAccount
                val notCustomTokenFilter = !it.currency.isCustom
                statusFilter && currencyFilter && notCustomTokenFilter
            }

        if (walletCurrencyStatusesExceptInitial.isEmpty()) {
            return TokensDataStateExpress.EMPTY
        }

        val pairsLeast = getPairs(
            userWallet = userWallet,
            initialCurrency = LeastTokenInfo(
                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
                network = currency.network.backendId,
            ),
            currenciesList = walletCurrencyStatusesExceptInitial.map { it.currency },
        )

        return TokensDataStateExpress(
            fromGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast.pairs,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.to },
                tokenInfoForAvailable = { it.from },
            ),
            toGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast.pairs,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.from },
                tokenInfoForAvailable = { it.to },
            ),
            allProviders = pairsLeast.allProviders,
        )
    }

    private suspend fun getToCurrenciesGroup(
        currency: CryptoCurrency,
        leastPairs: List<SwapPairLeast>,
        cryptoCurrenciesList: List<CryptoCurrencyStatus>,
        tokenInfoForFilter: (SwapPairLeast) -> LeastTokenInfo,
        tokenInfoForAvailable: (SwapPairLeast) -> LeastTokenInfo,
    ): CurrenciesGroup {
        val filteredPairs = leastPairs.filter {
            tokenInfoForFilter(it).contractAddress == currency.getContractAddress() &&
                tokenInfoForFilter(it).network == currency.network.backendId
        }

        val availableCryptoCurrencies = cryptoCurrenciesList.mapNotNull { cryptoCurrencyStatus ->
            val providers = findProvidersForPair(cryptoCurrencyStatus, filteredPairs, tokenInfoForAvailable)
            if (providers != null) {
                CryptoCurrencySwapInfo(cryptoCurrencyStatus, providers)
            } else {
                null
            }
        }

        val unavailableCryptoCurrencies = cryptoCurrenciesList - availableCryptoCurrencies
            .map { it.currencyStatus }
            .toSet()

        return CurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { CryptoCurrencySwapInfo(it, emptyList()) },
            afterSearch = false,
        )
    }

    private suspend fun findProvidersForPair(
        cryptoCurrencyStatuses: CryptoCurrencyStatus,
        swapPairsLeastList: List<SwapPairLeast>,
        tokenInfoForAvailable: (SwapPairLeast) -> LeastTokenInfo,
    ): List<SwapProvider>? {
        val requirements = getAssetRequirementsUseCase.invoke(userWalletId, cryptoCurrencyStatuses.currency).getOrNull()
        val isAvailableForSwap = rampStateManager.checkAssetRequirements(requirements)

        return swapPairsLeastList.firstNotNullOfOrNull {
            val listTokenInfo = tokenInfoForAvailable(it)
            if (cryptoCurrencyStatuses.currency.network.backendId == listTokenInfo.network &&
                cryptoCurrencyStatuses.currency.getContractAddress() == listTokenInfo.contractAddress &&
                isAvailableForSwap
            ) {
                it.providers
            } else {
                null
            }
        }
    }

    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }

    private suspend fun getPairs(
        userWallet: UserWallet,
        initialCurrency: LeastTokenInfo,
        currenciesList: List<CryptoCurrency>,
    ): PairsWithProviders {
        return repository.getPairs(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            currencyList = currenciesList,
        )
    }

    override suspend fun givePermissionToSwap(
        networkId: String,
        permissionOptions: PermissionOptions,
    ): SwapTransactionState {
        val amount = permissionOptions.approveData.fromTokenAmount.takeIf {
            permissionOptions.approveType == SwapApproveType.LIMITED
        }

        val approveTransaction = createApprovalTransactionUseCase(
            fee = permissionOptions.txFee.fee,
            userWalletId = userWalletId,
            cryptoCurrency = permissionOptions.fromToken as CryptoCurrency.Token,
            amount = amount?.value,
            contractAddress = permissionOptions.forTokenContractAddress,
            spenderAddress = permissionOptions.spenderAddress,
        ).getOrElse {
            Timber.e(it, "Failed to create approveTransaction")
            return SwapTransactionState.Error.UnknownError
        }

        val result = sendTransactionUseCase(
            txData = approveTransaction,
            userWallet = userWallet,
            network = permissionOptions.fromToken.network,
        )
        return result.fold(
            ifRight = { hash ->
                allowPermissionsHandler.addAddressToInProgress(permissionOptions.forTokenContractAddress)
                SwapTransactionState.TxSent(
                    txHash = hash,
                    timestamp = System.currentTimeMillis(),
                )
            },
            ifLeft = { SwapTransactionState.Error.TransactionError(it) },
        )
    }

    override suspend fun findBestQuote(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        reduceBalanceBy: BigDecimal,
        selectedFee: FeeType,
    ): Map<SwapProvider, SwapState> {
        Timber.i(
            """
               Find the best quote
               |- fromToken: $fromToken
               |- toToken: $toToken
               |- providers: $providers
               |- amountToSwap: $amountToSwap
               |- selectedFee: $selectedFee
            """.trimIndent(),
        )

        return providers.map { provider ->
            val amountDecimal = toBigDecimalOrNull(amountToSwap)
            if (amountDecimal == null || amountDecimal.signum() == 0) {
                return providers.associateWith { createEmptyAmountState() }
            }
            val amount = SwapAmount(amountDecimal, fromToken.currency.decimals)
            val isBalanceWithoutFeeEnough = isBalanceEnough(fromToken, amount, null)
            val networkId = fromToken.currency.network.backendId
            when (provider.type) {
                ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                    if (isSolana(networkId)) {
                        manageDexSolana(
                            networkId = networkId,
                            fromToken = fromToken,
                            toToken = toToken,
                            provider = provider,
                            selectedFee = selectedFee,
                            amount = amount,
                            isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                            expressOperationType = ExpressOperationType.SWAP,
                        )
                    } else {
                        manageDex(
                            networkId = networkId,
                            fromToken = fromToken,
                            toToken = toToken,
                            provider = provider,
                            selectedFee = selectedFee,
                            amount = amount,
                            isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                            expressOperationType = ExpressOperationType.SWAP,
                        )
                    }
                }
                ExchangeProviderType.CEX -> {
                    manageCex(
                        networkId = networkId,
                        fromToken = fromToken,
                        toToken = toToken,
                        provider = provider,
                        amount = amount,
                        reduceBalanceBy = reduceBalanceBy,
                        isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                        selectedFee = selectedFee,
                    )
                }
            }
        }.toMap()
    }

    private suspend fun manageDex(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        provider: SwapProvider,
        selectedFee: FeeType,
        amount: SwapAmount,
        isBalanceWithoutFeeEnough: Boolean,
        expressOperationType: ExpressOperationType,
    ): Pair<SwapProvider, SwapState> {
        val maybeQuotes = repository.findBestQuote(
            userWallet = userWallet,
            fromContractAddress = fromToken.currency.getContractAddress(),
            fromNetwork = fromToken.currency.network.backendId,
            toContractAddress = toToken.currency.getContractAddress(),
            toNetwork = toToken.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toToken.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        val fromTokenAddress = getTokenAddress(fromToken.currency)
        val isAllowedToSpend = maybeQuotes.fold(
            ifRight = { quotes ->
                quotes.allowanceContract?.let {
                    isAllowedToSpend(networkId, fromToken.currency, amount, it)
                } != false
            },
            ifLeft = { false },
        )

        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            fetchCurrencyStatusUseCase(userWalletId = userWalletId, id = fromToken.currency.id)
        }
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
            provider to loadDexSwapData(
                provider = provider,
                networkId = networkId,
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                selectedFee = selectedFee,
                expressOperationType = expressOperationType,
            )
        } else {
            provider to getQuotesState(
                provider = provider,
                quoteDataModel = maybeQuotes,
                amount = amount,
                fromToken = fromToken,
                toToken = toToken,
                networkId = networkId,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                txFee = TxFeeState.Empty,
                transactionFee = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded, // exclude for dex
                selectedFee = selectedFee,
            )
        }
    }

    private suspend fun manageDexSolana(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        provider: SwapProvider,
        selectedFee: FeeType,
        amount: SwapAmount,
        isBalanceWithoutFeeEnough: Boolean,
        expressOperationType: ExpressOperationType,
    ): Pair<SwapProvider, SwapState> {
        val maybeQuotes = repository.findBestQuote(
            userWallet = userWallet,
            fromContractAddress = fromToken.currency.getContractAddress(),
            fromNetwork = fromToken.currency.network.backendId,
            toContractAddress = toToken.currency.getContractAddress(),
            toNetwork = toToken.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toToken.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        return if (isBalanceWithoutFeeEnough) {
            provider to loadDexSwapData(
                provider = provider,
                networkId = networkId,
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                selectedFee = selectedFee,
                expressOperationType = expressOperationType,
            )
        } else {
            provider to getQuotesState(
                provider = provider,
                quoteDataModel = maybeQuotes,
                amount = amount,
                fromToken = fromToken,
                toToken = toToken,
                networkId = networkId,
                isAllowedToSpend = true,
                isBalanceWithoutFeeEnough = false,
                txFee = TxFeeState.Empty,
                transactionFee = null,
                includeFeeInAmount = IncludeFeeInAmount.Excluded, // exclude for dex
                selectedFee = selectedFee,
            )
        }
    }

    private suspend fun manageCex(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        isBalanceWithoutFeeEnough: Boolean,
        selectedFee: FeeType,
    ): Pair<SwapProvider, SwapState> {
        return provider to loadCexQuoteData(
            networkId = networkId,
            amount = amount,
            reduceBalanceBy = reduceBalanceBy,
            fromTokenStatus = fromToken,
            toTokenStatus = toToken,
            isAllowedToSpend = true,
            isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
            provider = provider,
            selectedFee = selectedFee,
        )
    }

    private suspend fun manageWarnings(
        fromTokenStatus: CryptoCurrencyStatus,
        amount: SwapAmount,
        feeState: TxFeeState,
        selectedFee: FeeType,
        includeFeeInAmount: IncludeFeeInAmount,
    ): CryptoCurrencyCheck {
        val fee = when (feeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> feeState.getFeeByType(selectedFee).feeValue
            is TxFeeState.SingleFeeState -> feeState.fee.feeValue
        }
        val balanceAfterTransaction = getCoinBalanceAfterTransaction(fromTokenStatus, amount, includeFeeInAmount, fee)
        val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
            includeFeeInAmount.amountSubtractFee
        } else {
            amount
        }
        val currencyCheck = getCurrencyCheckUseCase(
            userWalletId = userWalletId,
            currencyStatus = fromTokenStatus,
            amount = amountToRequest.value,
            fee = fee,
            feeCurrencyBalanceAfterTransaction = balanceAfterTransaction,
        )

        return currencyCheck
    }

    private suspend fun getCoinBalanceAfterTransaction(
        fromTokenStatus: CryptoCurrencyStatus,
        amount: SwapAmount,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: BigDecimal,
    ): BigDecimal? {
        return when (fromTokenStatus.currency) {
            is CryptoCurrency.Coin -> {
                val statusValue = fromTokenStatus.value as? CryptoCurrencyStatus.Loaded
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
                val feePaidCurrency = getFeePaidCurrency(
                    currency = fromTokenStatus.currency,
                )
                when (feePaidCurrency) {
                    FeePaidCurrency.Coin -> {
                        val nativeBalance = userWalletManager.getNativeTokenBalance(
                            networkId = fromTokenStatus.currency.network.backendId,
                            derivationPath = fromTokenStatus.currency.network.derivationPath.value,
                        )
                        nativeBalance?.let { it.value - fee }
                    }
                    else -> null // it doesnt matter for this fun
                }
            }
        }
    }

    private suspend fun manageTransactionValidationWarnings(
        fromToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        feeState: TxFeeState,
        userWalletId: UserWalletId,
    ): Throwable? {
        val currency = fromToken.currency
        val fee = Fee.Common(
            amount = Amount(
                value = when (feeState) {
                    TxFeeState.Empty -> BigDecimal.ZERO
                    is TxFeeState.MultipleFeeState -> feeState.normalFee.feeValue
                    is TxFeeState.SingleFeeState -> feeState.fee.feeValue
                },
                blockchain = currency.network.toBlockchain(),
            ),
        )

        val result = validateTransactionUseCase(
            amount = amount.value.convertToSdkAmount(currency),
            fee = fee,
            memo = null,
            destination = getTokenAddress(fromToken.currency),
            userWalletId = userWalletId,
            network = currency.network,
        ).leftOrNull()

        return result
    }

    override suspend fun onSwap(
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amountToSwap: String,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: TxFee,
        expressOperationType: ExpressOperationType,
    ): SwapTransactionState {
        Timber.i(
            """
               Swap
               |- swapProvider: $swapProvider
               |- swapData: $swapData
               |- currencyToSend: $currencyToSend
               |- currencyToGet: $currencyToGet
               |- amountToSwap: $amountToSwap
               |- includeFeeInAmount: $includeFeeInAmount
               |- fee: $fee
            """.trimIndent(),
        )

        val userWallet = userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.DemoMode
        }

        return when (swapProvider.type) {
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amountToSwap)
                val amount = SwapAmount(requireNotNull(amountDecimal), currencyToSend.currency.decimals)
                val amountToSwapWithFee = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                    includeFeeInAmount.amountSubtractFee
                } else {
                    amount
                }
                onSwapCex(
                    currencyToSend = currencyToSend,
                    currencyToGet = currencyToGet,
                    amount = amountToSwapWithFee,
                    txFee = fee,
                    swapProvider = swapProvider,
                    expressOperationType = expressOperationType,
                )
            }
            ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                val networkId = currencyToSend.currency.network.backendId
                if (isSolana(networkId)) {
                    onSwapSolanaDex(
                        provider = swapProvider,
                        networkId = currencyToSend.currency.network.backendId,
                        swapData = requireNotNull(swapData),
                        currencyToSendStatus = currencyToSend,
                        currencyToGetStatus = currencyToGet,
                        amountToSwap = amountToSwap,
                    )
                } else {
                    onSwapDex(
                        provider = swapProvider,
                        networkId = currencyToSend.currency.network.backendId,
                        swapData = requireNotNull(swapData),
                        currencyToSendStatus = currencyToSend,
                        currencyToGetStatus = currencyToGet,
                        txFee = fee,
                        amountToSwap = amountToSwap,
                    )
                }
            }
        }
    }

    // override suspend fun updateQuotesStateWithSelectedFee(
    //     state: SwapState.QuotesLoadedState,
    //     selectedFee: FeeType,
    //     fromToken: CryptoCurrencyStatus,
    //     amountToSwap: String,
    //     reduceBalanceBy: BigDecimal,
    // ): SwapState.QuotesLoadedState {
    //     val amountDecimal = toBigDecimalOrNull(amountToSwap)
    //     if (amountDecimal == null || amountDecimal.signum() == 0) {
    //         return state
    //     }
    //     val amount = SwapAmount(amountDecimal, fromToken.currency.decimals)
    //     val includeFeeInAmount = getIncludeFeeInAmount(
    //         networkId = fromToken.currency.network.backendId,
    //         txFee = state.txFee,
    //         amount = amount,
    //         reduceBalanceBy = reduceBalanceBy,
    //         fromToken = fromToken.currency,
    //         selectedFee = selectedFee,
    //     )
    //     val fee = when (val txFee = state.txFee) {
    //         TxFeeState.Empty -> BigDecimal.ZERO
    //         is TxFeeState.MultipleFeeState -> txFee.priorityFee.feeIncludeOtherNativeFee
    //         is TxFeeState.SingleFeeState -> txFee.fee.feeIncludeOtherNativeFee
    //     }
    //     val feeState = getFeeState(
    //         fee = fee,
    //         spendAmount = amount,
    //         networkId = fromToken.currency.network.backendId,
    //         fromTokenStatus = fromToken,
    //     )
    //     return state.copy(
    //         permissionState = PermissionDataState.Empty,
    //         preparedSwapConfigState = state.preparedSwapConfigState.copy(
    //             feeState = feeState,
    //             isBalanceEnough = includeFeeInAmount !is IncludeFeeInAmount.BalanceNotEnough,
    //             includeFeeInAmount = includeFeeInAmount,
    //         ),
    //     )
    // }

    private suspend fun onSwapDex(
        provider: SwapProvider,
        networkId: String,
        swapData: SwapDataModel,
        currencyToSendStatus: CryptoCurrencyStatus,
        currencyToGetStatus: CryptoCurrencyStatus,
        amountToSwap: String,
        txFee: TxFee,
    ): SwapTransactionState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val txValue = requireNotNull(swapData.transaction.txValue) { "txValue is null" }
        val amount = SwapAmount(amountDecimal, currencyToSendStatus.currency.decimals)
        val derivationPath = currencyToSendStatus.currency.network.derivationPath.value
        val dexTransaction = swapData.transaction as ExpressTransactionModel.DEX
        val dataToSign = dexTransaction.txData
        val amountToSend = createNativeAmountForDex(txValue, currencyToSendStatus.currency.network)
        val txData = createTransactionUseCase(
            amount = amountToSend,
            fee = txFee.fee,
            memo = null,
            destination = swapData.transaction.txTo,
            userWalletId = userWalletId,
            network = currencyToSendStatus.currency.network,
            txExtras = createDexTxExtras(dataToSign, currencyToSendStatus.currency.network, txFee.fee.getGasLimit()),
        ).getOrElse {
            Timber.e(it, "Failed to create swap dex tx data")
            return SwapTransactionState.Error.UnknownError
        }
        return handleSwapResult(
            provider = provider,
            networkId = networkId,
            swapData = swapData,
            currencyToSendStatus = currencyToSendStatus,
            currencyToGetStatus = currencyToGetStatus,
            amount = amount,
            derivationPath = derivationPath,
            txData = txData,
            payInAddress = txData.destinationAddress,
        )
    }

    private suspend fun onSwapSolanaDex(
        provider: SwapProvider,
        networkId: String,
        swapData: SwapDataModel,
        currencyToSendStatus: CryptoCurrencyStatus,
        currencyToGetStatus: CryptoCurrencyStatus,
        amountToSwap: String,
    ): SwapTransactionState {
        val dexTransaction = swapData.transaction as? ExpressTransactionModel.DEX
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val txDataBase64 = requireNotNull(dexTransaction?.txData) { "txData is null" }
        val amount = SwapAmount(amountDecimal, currencyToSendStatus.currency.decimals)
        val derivationPath = currencyToSendStatus.currency.network.derivationPath.value
        val compiledTransaction = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(Base64.decode(txDataBase64, Base64.NO_WRAP)),
        )
        return handleSwapResult(
            provider = provider,
            networkId = networkId,
            swapData = swapData,
            currencyToSendStatus = currencyToSendStatus,
            currencyToGetStatus = currencyToGetStatus,
            amount = amount,
            derivationPath = derivationPath,
            txData = compiledTransaction,
            payInAddress = swapData.transaction.txTo,
        )
    }

    private suspend fun handleSwapResult(
        provider: SwapProvider,
        networkId: String,
        swapData: SwapDataModel,
        currencyToSendStatus: CryptoCurrencyStatus,
        currencyToGetStatus: CryptoCurrencyStatus,
        amount: SwapAmount,
        derivationPath: String?,
        txData: TransactionData,
        payInAddress: String,
    ): SwapTransactionState {
        val result = sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = currencyToSendStatus.currency.network,
        )
        return result.fold(
            ifRight = { txHash ->
                repository.exchangeSent(
                    userWallet = userWallet,
                    txId = swapData.transaction.txId,
                    fromNetwork = currencyToSendStatus.currency.network.backendId,
                    fromAddress = currencyToSendStatus.value.networkAddress?.defaultAddress?.value.orEmpty(),
                    payInAddress = payInAddress,
                    txHash = txHash,
                    payInExtraId = swapData.transaction.txExtraId,
                )
                if (provider.type == ExchangeProviderType.DEX_BRIDGE) {
                    val timestamp = System.currentTimeMillis()
                    storeSwapTransaction(
                        currencyToSend = currencyToSendStatus,
                        currencyToGet = currencyToGetStatus,
                        amount = amount,
                        swapProvider = provider,
                        swapDataModel = swapData,
                        timestamp = timestamp,
                    )
                }
                storeLastCryptoCurrencyId(currencyToGetStatus.currency)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        currencyToSendStatus.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        swapData.toTokenAmount,
                        currencyToGetStatus.currency.symbol,
                    ),
                    toAmountValue = swapData.toTokenAmount.value,
                    txHash = userWalletManager.getLastTransactionHash(networkId, derivationPath).orEmpty(),
                    timestamp = System.currentTimeMillis(),
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

    @Suppress("LongMethod")
    private suspend fun onSwapCex(
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amount: SwapAmount,
        txFee: TxFee,
        swapProvider: SwapProvider,
        expressOperationType: ExpressOperationType,
    ): SwapTransactionState {
        val exchangeData = repository.getExchangeData(
            userWallet = userWallet,
            fromContractAddress = currencyToSend.currency.getContractAddress(),
            fromNetwork = currencyToSend.currency.network.backendId,
            toContractAddress = currencyToGet.currency.getContractAddress(),
            fromAddress = currencyToSend.value.networkAddress?.defaultAddress?.value.orEmpty(),
            toNetwork = currencyToGet.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = currencyToGet.currency.decimals,
            providerId = swapProvider.providerId,
            rateType = RateType.FLOAT,
            expressOperationType = expressOperationType,
            toAddress = currencyToGet.value.networkAddress?.defaultAddress?.value.orEmpty(),
            refundAddress = currencyToSend.value.networkAddress?.defaultAddress?.value,
            refundExtraId = null, // currently always null,
        ).getOrElse { return SwapTransactionState.Error.ExpressError(it) }

        val exchangeDataCex =
            exchangeData.transaction as? ExpressTransactionModel.CEX ?: return SwapTransactionState.Error.UnknownError

        val userWallet = userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.Error.UnknownError
        }

        val txData = createTransferTransactionUseCase(
            amount = amount.value.convertToSdkAmount(currencyToSend.currency),
            fee = txFee.fee,
            memo = exchangeDataCex.txExtraId,
            destination = exchangeDataCex.txTo,
            userWalletId = userWalletId,
            network = currencyToSend.currency.network,
        ).getOrElse {
            Timber.e(it, "Failed to create swap CEX tx data")
            return SwapTransactionState.Error.UnknownError
        }

        if (txData.extras == null && exchangeDataCex.txExtraId != null) {
            return SwapTransactionState.Error.UnknownError
        }

        val result = sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = currencyToSend.currency.network,
        )

        val derivationPath = currencyToSend.currency.network.derivationPath.value
        return result.fold(
            ifLeft = { SwapTransactionState.Error.TransactionError(it) },
            ifRight = { txHash ->
                repository.exchangeSent(
                    userWallet = userWallet,
                    txId = exchangeDataCex.txId,
                    fromNetwork = currencyToSend.currency.network.backendId,
                    fromAddress = currencyToSend.value.networkAddress?.defaultAddress?.value.orEmpty(),
                    payInAddress = txData.destinationAddress,
                    txHash = txHash,
                    payInExtraId = exchangeDataCex.txExtraId,
                )
                val timestamp = System.currentTimeMillis()
                val txExternalUrl = exchangeDataCex.externalTxUrl
                storeSwapTransaction(
                    currencyToSend = currencyToSend,
                    currencyToGet = currencyToGet,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    timestamp = timestamp,
                    txExternalUrl = txExternalUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                )
                storeLastCryptoCurrencyId(currencyToGet.currency)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        currencyToSend.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        exchangeData.toTokenAmount,
                        currencyToGet.currency.symbol,
                    ),
                    toAmountValue = exchangeData.toTokenAmount.value,
                    txHash = userWalletManager.getLastTransactionHash(
                        currencyToSend.currency.network.backendId,
                        derivationPath,
                    ).orEmpty(),
                    txExternalUrl = txExternalUrl,
                    timestamp = timestamp,
                )
            },
        )
    }

    private suspend fun storeSwapTransaction(
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amount: SwapAmount,
        swapProvider: SwapProvider,
        swapDataModel: SwapDataModel,
        timestamp: Long,
        txExternalUrl: String? = null,
        txExternalId: String? = null,
        averageDuration: Int? = null,
    ) {
        swapTransactionRepository.storeTransaction(
            userWalletId = userWalletId,
            fromCryptoCurrency = currencyToSend.currency,
            toCryptoCurrency = currencyToGet.currency,
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

    private suspend fun storeLastCryptoCurrencyId(cryptoCurrency: CryptoCurrency) {
        swapTransactionRepository.storeLastSwappedCryptoCurrencyId(
            UserWalletId(userWalletManager.getWalletId()),
            cryptoCurrency.id,
        )
    }

    override fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount {
        return SwapAmount(token.value.amount ?: BigDecimal.ZERO, token.currency.decimals)
    }

    override suspend fun getInitialCurrencyToSwap(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): CryptoCurrencyStatus? {
        val group = state.getGroupWithReverse(isReverseFromTo)
        return initialToCurrencyResolver.tryGetFromCache(userWallet, initialCryptoCurrency, state, isReverseFromTo)
            ?: initialToCurrencyResolver.tryGetWithMaxAmount(state, isReverseFromTo)
            ?: group.available.firstOrNull()?.currencyStatus
    }

    override fun getNativeToken(networkId: String): CryptoCurrency {
        return repository.getNativeTokenForNetwork(networkId)
    }

    private suspend fun isAllowedToSpend(
        networkId: String,
        fromToken: CryptoCurrency,
        amount: SwapAmount,
        spenderAddress: String,
    ): Boolean {
        if (fromToken is CryptoCurrency.Coin) return true

        val allowance = repository.getAllowance(
            userWalletId = userWallet.walletId,
            networkId = networkId,
            derivationPath = fromToken.network.derivationPath.value,
            tokenDecimalCount = fromToken.decimals,
            tokenAddress = getTokenAddress(fromToken),
            spenderAddress = spenderAddress,
        )
        return allowance >= amount.value
    }

    private suspend fun createEmptyAmountState(): SwapState {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        return SwapState.EmptyAmountState(
            zeroAmountEquivalent = BigDecimal.ZERO.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            },
        )
    }

    /**
     * Load quote data calls only if spend is not allowed for token contract address
     */
    @Suppress("LongParameterList")
    private suspend fun loadCexQuoteData(
        networkId: String,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        provider: SwapProvider,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
        selectedFee: FeeType,
    ): SwapState {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        return coroutineScope {
            val txFeeResult = getUnhandledFee(
                amount = amount.value,
                userWallet = userWallet,
                cryptoCurrency = fromToken,
            )

            val txFee = if (provider.type == ExchangeProviderType.CEX) {
                getFeeForCex(txFeeResult, fromTokenStatus)
            } else {
                TxFeeState.Empty
            }

            val includeFeeInAmount = getIncludeFeeInAmount(
                networkId = networkId,
                txFee = txFee,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                fromToken = fromToken,
                selectedFee = selectedFee,
            )
            val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                includeFeeInAmount.amountSubtractFee
            } else {
                amount
            }

            val quotes = repository.findBestQuote(
                userWallet = userWallet,
                fromContractAddress = fromToken.getContractAddress(),
                fromNetwork = fromToken.network.backendId,
                toContractAddress = toToken.getContractAddress(),
                toNetwork = toToken.network.backendId,
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
                fromToken = fromTokenStatus,
                toToken = toTokenStatus,
                networkId = networkId,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                txFee = txFee,
                transactionFee = txFeeResult.getOrNull(),
                includeFeeInAmount = includeFeeInAmount,
                selectedFee = selectedFee,
            )
        }
    }

    @Suppress("LongMethod")
    private suspend fun getQuotesState(
        provider: SwapProvider,
        quoteDataModel: Either<ExpressDataError, QuoteModel>,
        amount: SwapAmount,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        networkId: String,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
        txFee: TxFeeState,
        transactionFee: TransactionFee?,
        includeFeeInAmount: IncludeFeeInAmount,
        selectedFee: FeeType,
    ): SwapState {
        return quoteDataModel.fold(
            ifRight = { quoteModel ->
                val swapState = updateBalances(
                    networkId = networkId,
                    fromTokenStatus = fromToken,
                    toTokenStatus = toToken,
                    fromTokenAmount = amount,
                    toTokenAmount = quoteModel.toTokenAmount,
                    swapData = null,
                    txFeeState = txFee,
                    provider = provider,
                ).copy(
                    currencyCheck = manageWarnings(
                        fromTokenStatus = fromToken,
                        amount = amount,
                        feeState = txFee,
                        selectedFee = selectedFee,
                        includeFeeInAmount = includeFeeInAmount,
                    ),
                    validationResult = manageTransactionValidationWarnings(
                        fromToken = fromToken,
                        amount = amount,
                        feeState = txFee,
                        userWalletId = userWalletId,
                    ),
                    minAdaValue = (transactionFee?.normal as? Fee.CardanoToken)?.minAdaValue,
                )

                when (provider.type) {
                    ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                        val state = updatePermissionState(
                            networkId = networkId,
                            fromTokenStatus = fromToken,
                            swapAmount = amount,
                            quotesLoadedState = swapState,
                            isAllowedToSpend = isAllowedToSpend,
                            spenderAddress = quoteModel.allowanceContract,
                        )
                        if (state !is SwapState.QuotesLoadedState) return state
                        state.copy(
                            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                                isAllowedToSpend = isAllowedToSpend,
                                isBalanceEnough = isBalanceWithoutFeeEnough,
                            ),
                        )
                    }
                    ExchangeProviderType.CEX -> {
                        val fee = when (txFee) {
                            TxFeeState.Empty -> BigDecimal.ZERO
                            is TxFeeState.MultipleFeeState -> txFee.priorityFee.feeValue
                            is TxFeeState.SingleFeeState -> txFee.fee.feeValue
                        }
                        val feeState = getFeeState(
                            fee = fee,
                            spendAmount = amount,
                            networkId = networkId,
                            fromTokenStatus = fromToken,
                        )
                        swapState.copy(
                            permissionState = PermissionDataState.Empty,
                            preparedSwapConfigState = PreparedSwapConfigState(
                                feeState = feeState,
                                isAllowedToSpend = isAllowedToSpend,
                                isBalanceEnough = isBalanceWithoutFeeEnough,
                                hasOutgoingTransaction = hasOutgoingTransaction(fromToken),
                                includeFeeInAmount = includeFeeInAmount,
                            ),
                        )
                    }
                }
            },
            ifLeft = { error ->
                createSwapErrorWith(
                    fromToken = fromToken,
                    amount = amount,
                    includeFeeInAmount = includeFeeInAmount,
                    expressDataError = error,
                )
            },
        )
    }

    private suspend fun createSwapErrorWith(
        fromToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        includeFeeInAmount: IncludeFeeInAmount,
        expressDataError: ExpressDataError,
    ): SwapState.SwapError {
        val rates = getQuotes(fromToken.currency.id)
        val fromTokenSwapInfo = TokenSwapInfo(
            tokenAmount = amount,
            amountFiat = rates[fromToken.currency.id]?.multiply(amount.value)
                ?: BigDecimal.ZERO,
            cryptoCurrencyStatus = fromToken,
        )
        return SwapState.SwapError(fromTokenSwapInfo, expressDataError, includeFeeInAmount)
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun getIncludeFeeInAmount(
        networkId: String,
        txFee: TxFeeState,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        fromToken: CryptoCurrency,
        selectedFee: FeeType,
    ): IncludeFeeInAmount {
        val feeValue = when (txFee) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> txFee.getFeeByType(selectedFee).feeIncludeOtherNativeFee
            is TxFeeState.SingleFeeState -> txFee.fee.feeIncludeOtherNativeFee
        }
        val feePaidCurrency = getFeePaidCurrency(
            currency = fromToken,
        )

        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > feeValue) {
                    IncludeFeeInAmount.Excluded
                } else {
                    IncludeFeeInAmount.BalanceNotEnough
                }
            }
            else -> getIncludeFeeAmountForCoinFee(
                networkId = networkId,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                feeValue = feeValue,
                fromToken = fromToken,
            )
        }
    }

    private suspend fun getIncludeFeeAmountForCoinFee(
        networkId: String,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
        fromToken: CryptoCurrency,
    ): IncludeFeeInAmount {
        val tokenForFeeBalance =
            userWalletManager.getNativeTokenBalance(
                networkId,
                fromToken.network.derivationPath.value,
            ) ?: ProxyAmount.empty()
        val reducedBalance = tokenForFeeBalance.value - reduceBalanceBy
        val amountWithFee = amount.value + feeValue
        return when {
            fromToken is CryptoCurrency.Token -> {
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
                    IncludeFeeInAmount.Included(
                        amountSubtractFee = SwapAmount(
                            reducedBalance - feeValue,
                            getNativeToken(fromToken.network.backendId).decimals,
                        ),
                    )
                } else {
                    IncludeFeeInAmount.BalanceNotEnough
                }
            }
        }
    }

    private suspend fun getFormattedFiatFees(fromToken: CryptoCurrency, vararg fees: BigDecimal): List<String> {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        val feePaidCurrency = getFeePaidCurrency(
            currency = fromToken,
        )
        val feeCurrencyId: CryptoCurrency.ID = when (feePaidCurrency) {
            is FeePaidCurrency.Token -> feePaidCurrency.tokenId
            else -> getNativeToken(networkId = fromToken.network.backendId).id
        }
        val rates = getQuotes(feeCurrencyId)
        return rates[feeCurrencyId]?.let { rate ->
            fees.map { fee ->
                rate.multiply(fee).format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                }
            }
        }.orEmpty()
    }

    /**
     * Load swap data calls only if spend is allowed for token contract address
     */
    @Suppress("LongParameterList", "LongMethod")
    private suspend fun loadDexSwapData(
        provider: SwapProvider,
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        selectedFee: FeeType,
        expressOperationType: ExpressOperationType,
    ): SwapState {
        return repository.getExchangeData(
            userWallet = userWallet,
            fromContractAddress = fromToken.currency.getContractAddress(),
            fromNetwork = fromToken.currency.network.backendId,
            toContractAddress = toToken.currency.getContractAddress(),
            fromAddress = fromToken.value.networkAddress?.defaultAddress?.value.orEmpty(),
            toNetwork = toToken.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toToken.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
            toAddress = toToken.value.networkAddress?.defaultAddress?.value.orEmpty(),
            refundAddress = fromToken.value.networkAddress?.defaultAddress?.value,
            expressOperationType = expressOperationType,
        ).fold(
            ifRight = { swapData ->
                val transaction = swapData.transaction as ExpressTransactionModel.DEX
                val nativeCoinDecimals = Blockchain.fromNetworkId(networkId)?.decimals()
                    ?: error("Blockchain not found")
                val otherNativeFee = transaction.otherNativeFeeWei
                    ?.movePointLeft(nativeCoinDecimals)
                    ?: BigDecimal.ZERO
                val txFeeState = if (isSolana(networkId)) {
                    getFeeDataForSolanaDexSwap(
                        network = fromToken.currency.network,
                        transaction = transaction,
                    )
                        .toTxFeeState(fromToken.currency, otherNativeFee)
                } else {
                    getFeeDataForDexSwap(
                        network = fromToken.currency.network,
                        transaction = transaction,
                        fromToken = fromToken.currency,
                    )
                        .patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_DEX)
                        .toTxFeeState(fromToken.currency, otherNativeFee)
                }

                val includeFeeInAmount = IncludeFeeInAmount.Excluded // exclude for dex
                val feeByPriority = selectFeeByType(feeType = selectedFee, txFeeState = txFeeState)
                val feeToCheckFunds = feeByPriority + (otherNativeFee ?: BigDecimal.ZERO)
                val isBalanceIncludeFeeEnough = isBalanceEnough(fromToken, amount, feeToCheckFunds)
                val feeState = getFeeState(
                    fee = feeToCheckFunds,
                    spendAmount = amount,
                    networkId = networkId,
                    fromTokenStatus = fromToken,
                )
                val preparedSwapConfigState = PreparedSwapConfigState(
                    isAllowedToSpend = true,
                    isBalanceEnough = isBalanceIncludeFeeEnough,
                    feeState = feeState,
                    hasOutgoingTransaction = hasOutgoingTransaction(fromToken),
                    includeFeeInAmount = includeFeeInAmount,
                )
                val swapState = updateBalances(
                    networkId = networkId,
                    fromTokenStatus = fromToken,
                    toTokenStatus = toToken,
                    fromTokenAmount = amount,
                    toTokenAmount = swapData.toTokenAmount,
                    swapData = swapData,
                    txFeeState = txFeeState,
                    provider = provider,
                )
                swapState.copy(
                    permissionState = PermissionDataState.Empty,
                    currencyCheck = manageWarnings(
                        fromTokenStatus = fromToken,
                        amount = amount,
                        feeState = txFeeState,
                        selectedFee = selectedFee,
                        includeFeeInAmount = includeFeeInAmount,
                    ),
                    validationResult = manageTransactionValidationWarnings(
                        fromToken = fromToken,
                        amount = amount,
                        feeState = txFeeState,
                        userWalletId = userWalletId,
                    ),
                    preparedSwapConfigState = preparedSwapConfigState,
                )
            },
            ifLeft = { error ->
                val rates = getQuotes(fromToken.currency.id)
                val fromTokenSwapInfo = TokenSwapInfo(
                    tokenAmount = amount,
                    amountFiat = rates[fromToken.currency.id]?.multiply(amount.value)
                        ?: BigDecimal.ZERO,
                    cryptoCurrencyStatus = fromToken,
                )
                SwapState.SwapError(
                    fromTokenSwapInfo,
                    error,
                    IncludeFeeInAmount.Excluded,
                )
            },
        )
    }

    private suspend fun getFeeDataForDexSwap(
        network: Network,
        transaction: ExpressTransactionModel.DEX,
        fromToken: CryptoCurrency,
    ): TransactionFee {
        return try {
            val txAmountValue = transaction.txValue ?: error("unable to get txValue")
            val nativeBalance = userWalletManager.getNativeTokenBalance(
                networkId = network.backendId,
                derivationPath = fromToken.network.derivationPath.value,
            ) ?: ProxyAmount.empty()
            val amountToSend = createNativeAmountForDex(txAmountValue, fromToken.network)
            // transaction.txValue is always native coin
            if (nativeBalance.value < amountToSend.value) {
                error("It's impossible to calculate fee for nativeBalance.value < amountToSend.value")
            }

            val extras = createTransactionExtrasUseCase(
                data = transaction.txData,
                network = network,
            ).getOrNull() ?: error("unable to create extras")

            val transactionData = TransactionData.Uncompiled(
                amount = amountToSend,
                destinationAddress = transaction.txTo,
                fee = null,
                sourceAddress = transaction.txFrom,
                extras = extras,
            )
            getFeeUseCase(
                transactionData = transactionData,
                network = network,
                userWallet = userWallet,
            ).getOrNull() ?: error("unable to calculate fee")
        } catch (_: IllegalStateException) {
            getEthSpecificFeeUseCase(
                userWallet = userWallet,
                cryptoCurrency = fromToken,
                gasLimit = transaction.gas,
            ).getOrNull() ?: error("can't get fee for getEthSpecificFeeUseCase")
        }
    }

    private suspend fun getFeeDataForSolanaDexSwap(
        network: Network,
        transaction: ExpressTransactionModel.DEX,
    ): TransactionFee {
        val txData = transaction.txData
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(Base64.decode(txData, Base64.NO_WRAP)),
        )

        return getFeeUseCase(
            transactionData = transactionData,
            network = network,
            userWallet = userWallet,
        ).getOrNull() ?: error("unable to calculate fee")
    }

    @Suppress("LongParameterList")
    private suspend fun updateBalances(
        provider: SwapProvider,
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        fromTokenAmount: SwapAmount,
        toTokenAmount: SwapAmount,
        swapData: SwapDataModel?,
        txFeeState: TxFeeState,
    ): SwapState.QuotesLoadedState {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        val nativeToken = repository.getNativeTokenForNetwork(networkId)
        val rates = getQuotes(fromToken.id, toToken.id, nativeToken.id)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                cryptoCurrencyStatus = fromTokenStatus,
                amountFiat = rates[fromToken.id]?.multiply(fromTokenAmount.value)
                    ?: BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                cryptoCurrencyStatus = toTokenStatus,
                amountFiat = rates[toToken.id]?.multiply(toTokenAmount.value)
                    ?: BigDecimal.ZERO,
            ),
            priceImpact = calculatePriceImpact(
                fromTokenAmount = fromTokenAmount.value,
                fromRate = rates[fromToken.id]?.toDouble() ?: 0.0,
                toTokenAmount = toTokenAmount.value,
                toRate = rates[toToken.id]?.toDouble() ?: 0.0,
            ),
            swapDataModel = swapData,
            txFee = txFeeState,
            swapProvider = provider,
            minAdaValue = null,
        )
    }

    private suspend fun getFeeForCex(
        txFeeResult: Either<GetFeeError, TransactionFee>?,
        fromToken: CryptoCurrencyStatus,
    ): TxFeeState {
        return txFeeResult?.fold(
            ifLeft = { TxFeeState.Empty },
            ifRight = { txFee ->
                txFee
                    .patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_SEND)
                    .toTxFeeState(fromToken.currency, null)
            },
        ) ?: TxFeeState.Empty
    }

    private suspend fun getUnhandledFee(
        amount: BigDecimal,
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): Either<GetFeeError, TransactionFee> {
        return estimateFeeUseCase(
            amount = amount,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
        )
    }

    @Suppress("LongParameterList", "LongMethod")
    private suspend fun updatePermissionState(
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
        swapAmount: SwapAmount,
        quotesLoadedState: SwapState.QuotesLoadedState,
        spenderAddress: String?,
        isAllowedToSpend: Boolean,
    ): SwapState {
        val fromToken = fromTokenStatus.currency
        if (isAllowedToSpend) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        // if token balance ZERO not show permission state to avoid user to spend money for fee
        val isTokenZeroBalance = getTokenBalance(fromTokenStatus).value.signum() == 0
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
        val derivationPath = fromToken.network.derivationPath.value
        // setting up amount for approve with given amount for swap [SwapApproveType.Limited]
        val callData = SmartContractCallDataProviderFactory.getApprovalCallData(
            spenderAddress = requireNotNull(spenderAddress) { "Spender address is null" },
            amount = swapAmount.value.convertToSdkAmount(fromToken),
            blockchain = fromToken.network.toBlockchain(),
        )
        val feeData = try {
            val extras = createTransactionExtrasUseCase(
                callData = callData,
                network = fromToken.network,
            ).getOrNull() ?: error("unable to create extras")

            val fromAddress = requireNotNull(
                fromTokenStatus.value.networkAddress?.defaultAddress?.value,
            ) { "networkAddress cant be null" }
            val transactionData = TransactionData.Uncompiled(
                amount = createNativeAmountForDex("0", fromToken.network),
                destinationAddress = fromToken.getContractAddress(),
                fee = null,
                sourceAddress = fromAddress,
                extras = extras,
            )
            getFeeUseCase(
                transactionData = transactionData,
                network = fromToken.network,
                userWallet = userWallet,
            ).getOrNull() ?: error("unable to calculate fee")
        } catch (e: Exception) {
            Timber.e(e, "Failed to get fee")
            // it's impossible next steps without fee
            return createSwapErrorWith(
                fromToken = fromTokenStatus,
                amount = swapAmount,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
                expressDataError = ExpressDataError.UnknownError,
            )
        }

        val feeState = feeData
            .patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_DEX)
            .toTxFeeState(fromToken, null)

        val fee = when (feeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> feeState.normalFee.feeValue
            is TxFeeState.SingleFeeState -> feeState.fee.feeValue
        }
        val swapFeeState = getFeeState(
            fee = fee,
            spendAmount = SwapAmount.zeroSwapAmount(),
            networkId = networkId,
            fromTokenStatus = fromTokenStatus,
        )
        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionReadyForRequest(
                currency = fromToken.symbol,
                amount = INFINITY_SYMBOL,
                walletAddress = getWalletAddress(networkId, derivationPath),
                spenderAddress = getTokenAddress(fromToken),
                requestApproveData = RequestApproveStateData(
                    fee = feeState,
                    fromTokenAmount = swapAmount,
                    spenderAddress = spenderAddress,
                ),
            ),
            preparedSwapConfigState = quotesLoadedState.preparedSwapConfigState.copy(
                feeState = swapFeeState,
            ),
        )
    }

    @Suppress("LongMethod")
    private suspend fun TransactionFee.toTxFeeState(
        fromToken: CryptoCurrency,
        otherNativeFee: BigDecimal?,
    ): TxFeeState {
        val otherNativeFeeValue = otherNativeFee ?: BigDecimal.ZERO
        return when (this) {
            is TransactionFee.Choosable -> {
                val feeNormal = this.normal.amount.value ?: BigDecimal.ZERO
                val feePriority = this.priority.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(fromToken, feeNormal)[0]
                val priorityFiatValue = getFormattedFiatFees(fromToken, feePriority)[0]

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
                val normalFiatValueWithNative = getFormattedFiatFees(fromToken, normalFeeWithOtherNative)[0]
                val priorityFiatValueWithNative = getFormattedFiatFees(fromToken, priorityFeeWithOtherNative)[0]

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
                    normalFee = TxFee(
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
                    priorityFee = TxFee(
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
                val normalFiatValue = getFormattedFiatFees(fromToken, feeNormal)[0]
                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = this.normal.amount.decimals,
                )
                // region otherNativeFee
                val normalFeeWithOtherNative = feeNormal + otherNativeFeeValue
                val normalFiatValueWithNative = getFormattedFiatFees(fromToken, normalFeeWithOtherNative)[0]

                val normalCryptoFeeWithNative = amountFormatter.formatBigDecimalAmountToUI(
                    amount = normalFeeWithOtherNative,
                    decimals = this.normal.amount.decimals,
                )
                // endregion
                TxFeeState.SingleFeeState(
                    fee = TxFee(
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
        val nativeDecimals = Blockchain.fromNetworkId(network.backendId)?.decimals()
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
        val increasedGasLimit = gasLimit
            .multiply(percentage.toBigInteger())
            .divide(hundredPercent)
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
        )
        return when (this) {
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
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

    private fun selectFeeByType(feeType: FeeType, txFeeState: TxFeeState): BigDecimal {
        return when (txFeeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.SingleFeeState -> txFeeState.fee.feeValue
            is TxFeeState.MultipleFeeState -> when (feeType) {
                FeeType.NORMAL -> txFeeState.normalFee.feeValue
                FeeType.PRIORITY -> txFeeState.priorityFee.feeValue
            }
        }
    }

    private suspend fun isBalanceEnough(
        fromToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(fromToken).value
        val feePaidCurrency = getFeePaidCurrency(
            currency = fromToken.currency,
        )
        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> tokenBalance >= amount.value
            else -> {
                if (fromToken.currency is CryptoCurrency.Token) {
                    tokenBalance >= amount.value
                } else {
                    tokenBalance >= amount.value.plus(fee ?: BigDecimal.ZERO)
                }
            }
        }
    }

    private suspend fun getFeePaidCurrency(currency: CryptoCurrency): FeePaidCurrency {
        return currenciesRepository.getFeePaidCurrency(
            userWalletId = userWalletId,
            network = currency.network,
        )
    }

    private suspend fun getWalletAddress(networkId: String, derivationPath: String?): String {
        return userWalletManager.getWalletAddress(networkId, derivationPath)
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
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
    ): SwapFeeState {
        if (fee == null) {
            return SwapFeeState.NotEnough()
        }

        val percentsToFeeIncrease = BigDecimal.ONE
        return when (val feePaidCurrency = getFeePaidCurrency(fromTokenStatus.currency)) {
            FeePaidCurrency.Coin -> {
                val nativeTokenBalance = userWalletManager.getNativeTokenBalance(
                    networkId,
                    fromTokenStatus.currency.network.derivationPath.value,
                )
                nativeTokenBalance?.let { balance ->
                    val balanceToCheck = when (fromTokenStatus.currency) {
                        is CryptoCurrency.Token -> {
                            balance.value
                        }
                        is CryptoCurrency.Coin -> {
                            // need to check balance minus amount only if amount to swap in native token
                            balance.value.minus(spendAmount.value)
                        }
                    }
                    if (balanceToCheck > fee.multiply(percentsToFeeIncrease)) {
                        SwapFeeState.Enough
                    } else {
                        val nativeToken = getNativeToken(fromTokenStatus.currency.network.backendId)
                        SwapFeeState.NotEnough(
                            feeCurrency = nativeToken,
                            currencyName = nativeToken.network.name,
                            currencySymbol = nativeToken.symbol,
                        )
                    }
                } ?: SwapFeeState.NotEnough()
            }
            FeePaidCurrency.SameCurrency -> {
                val balance = fromTokenStatus.value.amount ?: return SwapFeeState.NotEnough()
                if (balance.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    SwapFeeState.NotEnough(
                        feeCurrency = fromTokenStatus.currency,
                        currencyName = fromTokenStatus.currency.name,
                        currencySymbol = fromTokenStatus.currency.symbol,
                    )
                }
            }
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    val tokens = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                        params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                    )
                        .orEmpty()

                    val token = tokens
                        .filterIsInstance<CryptoCurrency.Token>()
                        .find {
                            it.contractAddress.equals(feePaidCurrency.contractAddress, ignoreCase = true) &&
                                it.network.derivationPath == fromTokenStatus.currency.network.derivationPath
                        }

                    SwapFeeState.NotEnough(
                        feeCurrency = token,
                        currencyName = feePaidCurrency.name,
                        currencySymbol = feePaidCurrency.symbol,
                    )
                }
            }
            is FeePaidCurrency.FeeResource -> {
                val network = repository.getNativeTokenForNetwork(networkId).network
                val isFeeResourceEnough = currencyChecksRepository.checkIfFeeResourceEnough(
                    amount = spendAmount.value,
                    userWalletId = userWalletId,
                    network = network,
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
        fromRate: Double,
        toTokenAmount: BigDecimal,
        toRate: Double,
    ): PriceImpact {
        val fromTokenFiatValue = fromTokenAmount.multiply(fromRate.toBigDecimal())
        val toTokenFiatValue = toTokenAmount.multiply(toRate.toBigDecimal())
        val value = (BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)).toFloat()
        return PriceImpact.Value(value)
    }

    private suspend fun getQuotes(vararg ids: CryptoCurrency.ID): Map<CryptoCurrency.ID, BigDecimal> {
        val set = ids.mapNotNullTo(destination = hashSetOf(), transform = CryptoCurrency.ID::rawCurrencyId)
            .getQuotesOrEmpty()

        return ids
            .mapNotNull { id ->
                val found = set.find { it.rawCurrencyId == id.rawCurrencyId && it.value is QuoteStatus.Data }
                    ?: return@mapNotNull null

                id to (found.value as QuoteStatus.Data).fiatRate
            }
            .toMap()
    }

    private suspend fun Set<CryptoCurrency.RawID>.getQuotesOrEmpty(): Set<QuoteStatus> {
        return runCatching {
            val cachedQuotes = quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = this)

            val allQuotesFound = cachedQuotes?.all { it.value !is QuoteStatus.Empty } == true

            if (allQuotesFound) return@runCatching cachedQuotes

            val currenciesIds = if (cachedQuotes.isNullOrEmpty()) {
                this
            } else {
                cachedQuotes.mapNotNullTo(hashSetOf()) {
                    if (it.value is QuoteStatus.Empty) it.rawCurrencyId else null
                }
            }

            multiQuoteStatusFetcher(
                params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null),
            )

            quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = this)
        }
            .getOrNull()
            .orEmpty()
    }

    private fun isSolana(networkId: String): Boolean {
        return networkId == Blockchain.Solana.toNetworkId()
    }

    companion object {
        private const val INCREASE_GAS_LIMIT_FOR_DEX = 112 // 12%
        private const val INCREASE_GAS_LIMIT_FOR_SEND = 105 // 5%
        private const val INFINITY_SYMBOL = "∞"
    }

    @AssistedFactory
    interface Factory : SwapInteractor.Factory {
        override fun create(selectedWalletId: UserWalletId): SwapInteractorImpl
    }
}