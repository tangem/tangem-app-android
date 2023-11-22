package com.tangem.feature.swap.domain

import arrow.core.getOrElse
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusesSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.SwapCurrencyConverter
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.*
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.toFiatString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LargeClass", "LongParameterList")
internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val cache: SwapDataCache,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val walletFeatureToggles: WalletFeatureToggles,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getMultiCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusesSyncUseCase,
    private val quotesRepository: QuotesRepository,
) : SwapInteractor {
// [REDACTED_TODO_COMMENT]
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        AddCryptoCurrenciesUseCase(currenciesRepository, networksRepository)
    }

    private val swapCurrencyConverter = SwapCurrencyConverter()
    private val amountFormatter = AmountFormatter()
    private var derivationPath: String? = null
    private var network: Network? = null

    override suspend fun getTokensDataState(currency: CryptoCurrency): TokensDataStateExpress {
        val selectedWallet = getSelectedWalletSyncUseCase().fold(
            ifLeft = { null },
            ifRight = { it },
        )

        requireNotNull(selectedWallet) { "No selected wallet" }

        val walletCurrencyStatuses = getMultiCryptoCurrencyStatusUseCase(selectedWallet.walletId)
            .getOrElse { emptyList() }

        val walletCurrencyStatusesExceptInitial = walletCurrencyStatuses.filter {
            it.currency.network.backendId != currency.network.backendId ||
                it.currency.getContractAddress() != currency.getContractAddress()
        }

        val pairsLeast = getPairs(
            initialCurrency = LeastTokenInfo(
                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
                network = currency.network.backendId,
            ),
            currenciesList = walletCurrencyStatusesExceptInitial.map { it.currency },
        )

        return TokensDataStateExpress(
            fromGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.from },
                tokenInfoForAvailable = { it.to },
            ),
            toGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.to },
                tokenInfoForAvailable = { it.from },
            ),
        )
    }

    override fun getSelectedWallet(): UserWallet? {
        return getSelectedWalletSyncUseCase().getOrNull()
    }

    private fun getToCurrenciesGroup(
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

        val availableCryptoCurrencies = filteredPairs.mapNotNull { pair ->
            val status = findCryptoCurrencyStatusByLeastInfo(tokenInfoForAvailable(pair), cryptoCurrenciesList)
            status?.let { CryptoCurrencySwapInfo(it, pair.providers) }
        }

        val unavailableCryptoCurrencies = cryptoCurrenciesList - availableCryptoCurrencies
            .map { it.currencyStatus }
            .toSet()

        return CurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { CryptoCurrencySwapInfo(it, emptyList()) },
        )
    }

    private fun findCryptoCurrencyStatusByLeastInfo(
        leastTokenInfo: LeastTokenInfo,
        cryptoCurrencyStatusesList: List<CryptoCurrencyStatus>,
    ): CryptoCurrencyStatus? {
        return cryptoCurrencyStatusesList.find {
            it.currency.network.backendId == leastTokenInfo.network &&
                it.currency.getContractAddress() == leastTokenInfo.contractAddress
        }
    }

    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }

    private suspend fun getPairs(
        initialCurrency: LeastTokenInfo,
        currenciesList: List<CryptoCurrency>,
    ): List<SwapPairLeast> {
        val pairs = repository.getPairs(initialCurrency, currenciesList)
        val providers = pairs.flatMap { it.providers }.toSet()
        val updatedProviders = repository.getProvidersDetails(providers).associateBy { it.providerId }
        return pairs.map { pair ->
            pair.copy(
                providers = pair.providers.mapNotNull { currentProvider ->
                    updatedProviders[currentProvider.providerId]
                },
            )
        }
    }

    @Deprecated("used in old swap mechanism")
    override fun initDerivationPathAndNetwork(derivationPath: String?, network: Network) {
        this.derivationPath = derivationPath
        this.network = network
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun searchTokens(networkId: String, searchQuery: String): FoundTokensStateExpress {
        val searchQueryLowerCase = searchQuery.lowercase()
        val tokensInWallet = cache.getInWalletTokens()
            .filter {
                it.token.name.lowercase().contains(searchQueryLowerCase) ||
                    it.token.symbol.lowercase().contains(searchQueryLowerCase)
            }
        val loadedTokens = cache.getLoadedTokens()
            .filter {
                it.token.name.lowercase().contains(searchQueryLowerCase) ||
                    it.token.symbol.lowercase().contains(searchQueryLowerCase)
            }
        return FoundTokensStateExpress(
            tokensInWallet = tokensInWallet,
            loadedTokens = loadedTokens,
        )
    }

    @Deprecated("used in old swap mechanism")
    override fun findTokenById(id: String): CryptoCurrency? {
        val tokensInWallet = cache.getInWalletTokens()
        val loadedTokens = cache.getLoadedTokens()
        return tokensInWallet.firstOrNull { it.token.id.value == id }?.token
            ?: loadedTokens.firstOrNull { it.token.id.value == id }?.token
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun givePermissionToSwap(networkId: String, permissionOptions: PermissionOptions): TxState {
        val dataToSign = if (permissionOptions.approveType == SwapApproveType.UNLIMITED) {
            getApproveData(
                networkId = networkId,
                derivationPath = derivationPath,
                fromToken = permissionOptions.fromToken,
            )
        } else {
            permissionOptions.approveData.approveData
        }
        val result = transactionManager.sendApproveTransaction(
            txData = ApproveTxData(
                networkId = networkId,
                feeAmount = permissionOptions.txFee.feeValue,
                gasLimit = permissionOptions.txFee.gasLimit,
                destinationAddress = getTokenAddress(permissionOptions.fromToken),
                dataToSign = dataToSign,
            ),
            derivationPath = derivationPath,
            analyticsData = AnalyticsData(
                feeType = permissionOptions.txFee.feeType.getNameForAnalytics(),
                tokenSymbol = permissionOptions.fromToken.symbol,
                permissionType = permissionOptions.approveType.getNameForAnalytics(),
            ),
        )
        return when (result) {
            is SendTxResult.Success -> {
                allowPermissionsHandler.addAddressToInProgress(permissionOptions.forTokenContractAddress)
                TxState.TxSent(txAddress = userWalletManager.getLastTransactionHash(networkId, derivationPath) ?: "")
            }
            SendTxResult.UserCancelledError -> TxState.UserCancelled
            is SendTxResult.BlockchainSdkError -> TxState.BlockchainError
            is SendTxResult.TangemSdkError -> TxState.TangemSdkError
            is SendTxResult.NetworkError -> TxState.NetworkError
            is SendTxResult.UnknownError -> TxState.UnknownError
        }
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun findBestQuote(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        selectedFee: FeeType,
    ): Map<SwapProvider, SwapState> {
        syncWalletBalanceForTokens(networkId, listOf(fromToken.currency, toToken.currency))
        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.signum() == 0) {
            return providers.associateWith { createEmptyAmountState(networkId, fromToken.currency, toToken.currency) }
        }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(fromToken.currency))
        val fromTokenAddress = getTokenAddress(fromToken.currency)
        val toTokenAddress = getTokenAddress(toToken.currency)
        val isAllowedToSpend = isAllowedToSpend(networkId, fromToken.currency, amount)
        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            transactionManager.updateWalletManager(networkId, derivationPath)
        }
        val isBalanceWithoutFeeEnough = isBalanceEnough(networkId, fromToken.currency, amount, null)
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
// [REDACTED_TODO_COMMENT]
            providers.associateWith {
                loadSwapData(
                    networkId = networkId,
                    fromTokenAddress = fromTokenAddress,
                    toTokenAddress = toTokenAddress,
                    fromToken = fromToken,
                    toToken = toToken,
                    amount = amount,
                    selectedFee = selectedFee,
                )
            }
        } else {
            loadQuoteData(
                networkId = networkId,
                amount = amount,
                fromTokenStatus = fromToken,
                toTokenStatus = toToken,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                providers = providers,
            )
        }
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun onSwap(
        networkId: String,
        swapStateData: SwapStateData,
        currencyToSend: CryptoCurrency,
        currencyToGet: CryptoCurrency,
        amountToSwap: String,
        fee: TxFee,
    ): TxState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(currencyToSend))
        val result = transactionManager.sendTransaction(
            txData = SwapTxData(
                networkId = networkId,
                amountToSend = amountDecimal,
                currencyToSend = swapCurrencyConverter.convert(currencyToSend),
                feeAmount = fee.feeValue,
                gasLimit = fee.gasLimit,
                destinationAddress = swapStateData.swapModel.transaction.toWalletAddress,
                dataToSign = swapStateData.swapModel.transaction.data,
            ),
            isSwap = true,
            derivationPath = derivationPath,
            analyticsData = AnalyticsData(
                feeType = fee.feeType.getNameForAnalytics(),
                tokenSymbol = currencyToSend.symbol,
            ),
        )
        return when (result) {
            is SendTxResult.Success -> {
                if (walletFeatureToggles.isRedesignedScreenEnabled) {
                    onSuccessNewFlow(currencyToGet)
                } else {
                    onSuccessLegacyFlow(currencyToGet)
                }
                TxState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        currencyToSend.symbol,
                    ),
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        swapStateData.swapModel.toTokenAmount,
                        currencyToGet.symbol,
                    ),
                    txAddress = userWalletManager.getLastTransactionHash(networkId, derivationPath) ?: "",
                )
            }
            SendTxResult.UserCancelledError -> TxState.UserCancelled
            is SendTxResult.BlockchainSdkError -> TxState.BlockchainError
            is SendTxResult.TangemSdkError -> TxState.TangemSdkError
            is SendTxResult.NetworkError -> TxState.NetworkError
            is SendTxResult.UnknownError -> TxState.UnknownError
        }
    }

    @Deprecated("used in old swap mechanism")
    override fun getTokenBalance(networkId: String, token: CryptoCurrency): SwapAmount {
        return cache.getBalanceForToken(
            networkId = networkId,
            derivationPath = derivationPath,
            symbol = token.symbol,
        ) ?: SwapAmount(BigDecimal.ZERO, getTokenDecimals(token))
    }

    @Deprecated("used in old swap mechanism")
    override fun isAvailableToSwap(networkId: String): Boolean {
        return ONE_INCH_SUPPORTED_NETWORKS.contains(networkId)
    }

    @Deprecated("used in old swap mechanism")
    override fun getSwapAmountForToken(amount: String, token: CryptoCurrency): SwapAmount {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amount)) { "wrong amount format" }
        return SwapAmount(amountDecimal, getTokenDecimals(token))
    }

    @Deprecated("used in old swap mechanism")
    private suspend fun onSuccessLegacyFlow(currency: CryptoCurrency) {
        userWalletManager.addToken(swapCurrencyConverter.convert(currency), derivationPath)
        userWalletManager.refreshWallet()
    }

    @Deprecated("used in old swap mechanism")
    private suspend fun onSuccessNewFlow(currency: CryptoCurrency) {
        getSelectedWalletSyncUseCase().fold(
            ifRight = { userWallet ->
                addCryptoCurrenciesUseCase(userWallet.walletId, currency)
            },
            ifLeft = {
                Timber.e("Swap Error on getSelectedWalletUseCase")
            },
        )
    }

    @Deprecated("used in old swap mechanism")
    private fun getTangemFee(): Double {
        return repository.getTangemFee()
    }

    private fun getTokenDecimals(token: CryptoCurrency): Int {
        return if (token is CryptoCurrency.Token) {
            token.decimals
        } else {
            transactionManager.getNativeTokenDecimals(token.network.backendId)
        }
    }

    private suspend fun isAllowedToSpend(networkId: String, fromToken: CryptoCurrency, amount: SwapAmount): Boolean {
        if (fromToken is CryptoCurrency.Coin) return true
        return getSelectedWalletSyncUseCase().fold(
            ifRight = { userWallet ->
                val allowance = repository.getAllowance(
                    userWallet.walletId,
                    networkId,
                    derivationPath,
                    getTokenDecimals(fromToken),
                    getTokenAddress(fromToken),
                )
                allowance >= amount.value
            },
            ifLeft = {
                Timber.e("Swap Error on isAllowedToSpend")
                false
            },
        )
    }

    private fun createEmptyAmountState(
        networkId: String,
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
    ): SwapState {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val fromTokenBalance = cache.getBalanceForToken(networkId, derivationPath, fromToken.symbol)
        val toTokenBalance = cache.getBalanceForToken(networkId, derivationPath, toToken.symbol)
        return SwapState.EmptyAmountState(
            fromTokenWalletBalance = fromTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }.orEmpty(),
            toTokenWalletBalance = toTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }.orEmpty(),
            zeroAmountEquivalent = BigDecimal.ZERO.toFiatString(
                rateValue = BigDecimal.ONE,
                fiatCurrencyName = appCurrency.symbol,
                formatWithSpaces = true,
            ),
        )
    }

    /**
     * Load quote data calls only if spend is not allowed for token contract address
     */
    @Suppress("LongParameterList")
    private suspend fun loadQuoteData(
        networkId: String,
        amount: SwapAmount,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
    ): Map<SwapProvider, SwapState> {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        return coroutineScope {
            val quoteRequests = providers.map { provider ->
                async {
                    provider to repository.findBestQuote(
                        fromContractAddress = fromToken.getContractAddress(),
                        fromNetwork = fromToken.network.backendId,
                        toContractAddress = toToken.getContractAddress(),
                        toNetwork = toToken.network.backendId,
                        fromAmount = amount.toStringWithRightOffset(),
                        providerId = provider.providerId,
                        rateType = RateType.FLOAT,
                    )
                }
            }

            quoteRequests.awaitAll().map {
                it.first to
                    getState(
                        quoteDataModel = it.second,
                        amount = amount,
                        fromToken = fromTokenStatus,
                        toToken = toTokenStatus,
                        networkId = networkId,
                        isAllowedToSpend = isAllowedToSpend,
                        isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                    )
            }.associate { it.first to it.second }
        }
    }

    private suspend fun getState(
        quoteDataModel: AggregatedSwapDataModel<QuoteModel>,
        amount: SwapAmount,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        networkId: String,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
    ): SwapState {
        val quoteModel = quoteDataModel.dataModel
        if (quoteModel != null) {
            val swapState = updateBalances(
                networkId = networkId,
                fromTokenStatus = fromToken,
                toTokenStatus = toToken,
                fromTokenAmount = amount,
                toTokenAmount = quoteModel.toTokenAmount,
                swapStateData = null,
            )
            val quotesState = updatePermissionState(
                networkId = networkId,
                fromToken = fromToken.currency,
                swapAmount = amount,
                quotesLoadedState = swapState,
            )
            return quotesState.copy(
                preparedSwapConfigState = quotesState.preparedSwapConfigState.copy(
                    isAllowedToSpend = isAllowedToSpend,
                    isBalanceEnough = isBalanceWithoutFeeEnough,
                ),
            )
        } else {
            return SwapState.SwapError(quoteDataModel.error)
        }
    }

    private suspend fun getFormattedFiatFees(networkId: String, vararg fees: BigDecimal): List<String> {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = userWalletManager.getNativeTokenForNetwork(networkId)
        val rates = getQuotes(nativeToken.id)
        return rates[nativeToken.id]?.fiatRate?.let { rate ->
            fees.map { fee ->
                " (${fee.toFiatString(rate, appCurrency.symbol, true)})"
            }
        }.orEmpty()
    }

    /**
     * Load swap data calls only if spend is allowed for token contract address
     */
    @Suppress("LongParameterList")
    private suspend fun loadSwapData(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        selectedFee: FeeType,
    ): SwapState {
        repository.prepareSwapTransaction(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount.toStringWithRightOffset(),
            slippage = DEFAULT_SLIPPAGE,
            fromWalletAddress = getWalletAddress(networkId),
        ).let {
            val swapData = it.dataModel
            if (swapData != null) {
                val feeData = transactionManager.getFee(
                    networkId = networkId,
                    amountToSend = amount.value,
                    currencyToSend = swapCurrencyConverter.convert(fromToken.currency),
                    destinationAddress = swapData.transaction.toWalletAddress,
                    increaseBy = INCREASE_GAS_LIMIT_BY,
                    data = swapData.transaction.data,
                    derivationPath = derivationPath,
                )
                val txFeeState = proxyFeesToFeeState(networkId, feeData)
                val feeByPriority = when (selectedFee) {
                    FeeType.NORMAL -> txFeeState.normalFee.feeValue
                    FeeType.PRIORITY -> txFeeState.priorityFee.feeValue
                }
                val isBalanceIncludeFeeEnough =
                    isBalanceEnough(networkId, fromToken.currency, amount, feeByPriority)
                val isFeeEnough = checkFeeIsEnough(
                    fee = feeByPriority,
                    spendAmount = amount,
                    networkId = networkId,
                    fromToken = fromToken.currency,
                )
                val swapState = updateBalances(
                    networkId = networkId,
                    fromTokenStatus = fromToken,
                    toTokenStatus = toToken,
                    fromTokenAmount = amount,
                    toTokenAmount = swapData.toTokenAmount,
                    swapStateData = SwapStateData(
                        fee = txFeeState,
                        swapModel = swapData,
                    ),
                )
                return swapState.copy(
                    permissionState = PermissionDataState.Empty,
                    preparedSwapConfigState = PreparedSwapConfigState(
                        isAllowedToSpend = true,
                        isBalanceEnough = isBalanceIncludeFeeEnough,
                        isFeeEnough = isFeeEnough,
                    ),
                )
            } else {
                return SwapState.SwapError(it.error)
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun updateBalances(
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        fromTokenAmount: SwapAmount,
        toTokenAmount: SwapAmount,
        swapStateData: SwapStateData?,
    ): SwapState.QuotesLoadedState {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = userWalletManager.getNativeTokenForNetwork(networkId)
    
        val rates = getQuotes(fromToken.id, toToken.id, nativeToken.id)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                cryptoCurrencyStatus = fromTokenStatus,
                amountFiat = rates[fromToken.id]?.fiatRate?.multiply(fromTokenAmount.value)
                    ?: BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                cryptoCurrencyStatus = toTokenStatus,
                amountFiat = rates[toToken.id]?.fiatRate?.multiply(toTokenAmount.value)
                    ?: BigDecimal.ZERO,
            ),
            priceImpact = calculatePriceImpact(
                fromTokenAmount = fromTokenAmount.value,
                fromRate = rates[fromToken.id]?.fiatRate?.toDouble() ?: 0.0,
                toTokenAmount = toTokenAmount.value,
                toRate = rates[toToken.id]?.fiatRate?.toDouble() ?: 0.0,
            ),
            networkCurrency = userWalletManager.getNetworkCurrency(networkId),
            swapDataModel = swapStateData,
            tangemFee = getTangemFee(),
        )
    }

    @Suppress("LongParameterList")
    private suspend fun updatePermissionState(
        networkId: String,
        fromToken: CryptoCurrency,
        swapAmount: SwapAmount,
        quotesLoadedState: SwapState.QuotesLoadedState,
    ): SwapState.QuotesLoadedState {
        // if token balance ZERO not show permission state to avoid user to spend money for fee
        val isTokenZeroBalance = getTokenBalance(networkId, fromToken).value.signum() == 0
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
        // setting up amount for approve with given amount for swap [SwapApproveType.Limited]
        val transactionData = getApproveData(
            networkId = networkId,
            derivationPath = derivationPath,
            fromToken = fromToken,
            swapAmount = swapAmount,
        )
        val feeData = transactionManager.getFee(
            networkId = networkId,
            amountToSend = BigDecimal.ZERO,
            currencyToSend = swapCurrencyConverter.convert(userWalletManager.getNativeTokenForNetwork(networkId)),
            destinationAddress = getTokenAddress(fromToken),
            increaseBy = INCREASE_GAS_LIMIT_BY,
            data = transactionData,
            derivationPath = derivationPath,
        )
        val feeState = proxyFeesToFeeState(networkId, feeData)
        val isFeeEnough = checkFeeIsEnough(
            fee = feeData.normalFee.fee.value,
            spendAmount = SwapAmount.zeroSwapAmount(),
            networkId = networkId,
            fromToken = fromToken,
        )
        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionReadyForRequest(
                currency = fromToken.symbol,
                amount = INFINITY_SYMBOL,
                walletAddress = getWalletAddress(networkId),
                spenderAddress = getTokenAddress(fromToken),
                requestApproveData = RequestApproveStateData(
                    fee = feeState,
                    approveData = transactionData,
                    fromTokenAmount = swapAmount,
                ),
            ),
            preparedSwapConfigState = quotesLoadedState.preparedSwapConfigState.copy(
                isFeeEnough = isFeeEnough,
            ),
        )
    }

    private suspend fun syncWalletBalanceForTokens(networkId: String, tokens: List<CryptoCurrency>) {
        val tokensToSync = tokens.filter { cache.getBalanceForToken(networkId, derivationPath, it.symbol) == null }
        if (tokensToSync.isNotEmpty()) {
            val tokensBalance =
                userWalletManager.getCurrentWalletTokensBalance(
                    networkId = networkId,
                    extraTokens = tokensToSync.map { swapCurrencyConverter.convert(it) },
                    derivationPath = derivationPath,
                )
            cache.cacheBalances(
                networkId = networkId,
                derivationPath = derivationPath,
                balances = tokensBalance.mapValues { SwapAmount(it.value.value, it.value.decimals) },
            )
        }
    }

    private suspend fun proxyFeesToFeeState(networkId: String, proxyFees: ProxyFees): TxFeeState {
        val normalFeeValue = proxyFees.minFee.fee.value // in swap for normal use min fee
        val normalFeeGas = proxyFees.minFee.gasLimit.toInt()
        val priorityFeeValue = proxyFees.normalFee.fee.value // in swap for priority use normal fee
        val priorityFeeGas = proxyFees.normalFee.gasLimit.toInt()
        val feesFiat = getFormattedFiatFees(networkId, normalFeeValue, priorityFeeValue)
        val normalFiatFee = requireNotNull(feesFiat.getOrNull(0)) { "feesFiat item 0 couldn't be null" }
        val priorityFiatFee = requireNotNull(feesFiat.getOrNull(1)) { "feesFiat item 1 couldn't be null" }
        val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = normalFeeValue,
            decimals = transactionManager.getNativeTokenDecimals(networkId),
            currency = userWalletManager.getNetworkCurrency(networkId),
        )
        val priorityCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = priorityFeeValue,
            decimals = transactionManager.getNativeTokenDecimals(networkId),
            currency = userWalletManager.getNetworkCurrency(networkId),
        )
        return TxFeeState(
            normalFee = TxFee(
                feeValue = normalFeeValue,
                gasLimit = normalFeeGas,
                feeFiatFormatted = normalFiatFee,
                feeCryptoFormatted = normalCryptoFee,
                feeType = FeeType.NORMAL,
            ),
            priorityFee = TxFee(
                feeValue = priorityFeeValue,
                gasLimit = priorityFeeGas,
                feeFiatFormatted = priorityFiatFee,
                feeCryptoFormatted = priorityCryptoFee,
                feeType = FeeType.PRIORITY,
            ),
        )
    }

    private fun isBalanceEnough(
        networkId: String,
        fromToken: CryptoCurrency,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(networkId, fromToken).value
        return if (fromToken is CryptoCurrency.Token) {
            tokenBalance >= amount.value
        } else {
            tokenBalance > amount.value.plus(fee ?: BigDecimal.ZERO)
        }
    }

    private suspend fun getWalletAddress(networkId: String): String {
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

    override suspend fun checkFeeIsEnough(
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        networkId: String,
        fromToken: CryptoCurrency,
    ): Boolean {
        if (fee == null) {
            return false
        }
        val nativeTokenBalance = userWalletManager.getNativeTokenBalance(networkId, derivationPath)
        val percentsToFeeIncrease = BigDecimal.ONE
        return when (fromToken) {
            is CryptoCurrency.Coin -> {
                nativeTokenBalance?.let { balance ->
                    return balance.value.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)
                } ?: false
            }
            is CryptoCurrency.Token -> {
                nativeTokenBalance?.let { balance ->
                    return balance.value > fee.multiply(percentsToFeeIncrease)
                } ?: false
            }
        }
    }

    private fun toBigDecimalOrNull(amountToSwap: String): BigDecimal? {
        return amountToSwap.replace(",", ".").toBigDecimalOrNull()
    }

    private fun calculatePriceImpact(
        fromTokenAmount: BigDecimal,
        fromRate: Double,
        toTokenAmount: BigDecimal,
        toRate: Double,
    ): Float {
        val toTokenFiatValue = toTokenAmount.multiply(toRate.toBigDecimal())
        val fromTokenFiatValue = fromTokenAmount.multiply(fromRate.toBigDecimal())
        return (BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)).toFloat()
    }

    private suspend fun getApproveData(
        networkId: String,
        derivationPath: String?,
        fromToken: CryptoCurrency,
        swapAmount: SwapAmount? = null,
    ): String {
        return getSelectedWalletSyncUseCase().fold(
            ifRight = { userWallet ->
                repository.getApproveData(
                    userWalletId = userWallet.walletId,
                    networkId = networkId,
                    derivationPath = derivationPath,
                    currency = fromToken,
                    amount = swapAmount?.value,
                )
            },
            ifLeft = {
                Timber.e("Swap Error on getApproveData")
                error("Swap Error on getApproveData")
            },
        )
    }

    private suspend fun getQuotes(vararg ids: CryptoCurrency.ID) : Map<CryptoCurrency.ID, Quote> {
        val set = quotesRepository.getQuotesSync(ids.toSet(), false)

        return ids
            .mapNotNull { id -> set.find { it.rawCurrencyId == id.rawCurrencyId }?.let { id to it } }
            .toMap()
    }


    companion object {
        private const val DEFAULT_SLIPPAGE = 2

        @Suppress("UnusedPrivateMember")
        private const val INCREASE_FEE_TO_CHECK_ENOUGH_PERCENT = 1.0 // if need to increase fee when check isEnough
        private const val INCREASE_GAS_LIMIT_BY = 112 // 12%
        private const val INFINITY_SYMBOL = "∞"

        private val ONE_INCH_SUPPORTED_NETWORKS = listOf(
            "ethereum",
            "binance-smart-chain",
            "polygon-pos",
            "optimistic-ethereum",
            "arbitrum-one",
            "xdai",
            "avalanche",
            "fantom",
        )
    }
}
