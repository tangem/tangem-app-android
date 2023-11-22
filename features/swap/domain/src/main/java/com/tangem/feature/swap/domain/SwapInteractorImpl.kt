package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.SwapCurrencyConverter
import com.tangem.feature.swap.domain.models.SwapAmount
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
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import com.tangem.lib.crypto.models.Currency as LibCurrency

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
) : SwapInteractor {
// [REDACTED_TODO_COMMENT]
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        AddCryptoCurrenciesUseCase(currenciesRepository, networksRepository)
    }

    private val swapCurrencyConverter = SwapCurrencyConverter()
    private val amountFormatter = AmountFormatter()
    private var derivationPath: String? = null
    private var network: Network? = null

    override fun initDerivationPathAndNetwork(derivationPath: String?, network: Network?) {
        this.derivationPath = derivationPath
        this.network = network
    }

    override suspend fun initTokensToSwap(initialCurrency: Currency): TokensDataState {
// [REDACTED_TODO_COMMENT]
        val networkId = initialCurrency.networkId
        val availableTokens = cache.getAvailableTokens(networkId)
        val allLoadedTokens = availableTokens.ifEmpty {
            val tokens = repository.getExchangeableTokens(networkId)
            cache.cacheAvailableToSwapTokens(networkId, tokens)
            tokens
        }.filter { it.symbol != initialCurrency.symbol }

        // replace tokens in wallet tokens list with loaded same
        val loadedOnWalletsMap = mutableSetOf<String>()
        val tokensInWallet = userWalletManager.getUserTokens(
            networkId = networkId,
            derivationPath = derivationPath,
            isExcludeCustom = true,
        )
            .map { token ->
                val contractAddress = (token as? LibCurrency.NonNativeToken)?.contractAddress
                allLoadedTokens.firstOrNull {
                    if (it is Currency.NonNativeToken) {
                        it.symbol == token.symbol && it.contractAddress == contractAddress
                    } else {
                        it.symbol == token.symbol
                    }
                }?.let {
                    loadedOnWalletsMap.add(it.symbol)
                    it
                } ?: swapCurrencyConverter.convertBack(token)
            }
            .filter { it.symbol != initialCurrency.symbol && allLoadedTokens.contains(it) }
        val loadedTokens = allLoadedTokens
            .filter {
                !loadedOnWalletsMap.contains(it.symbol)
            }
        val tokensBalance = userWalletManager.getCurrentWalletTokensBalance(networkId, emptyList(), derivationPath)
            .mapValues { SwapAmount(it.value.value, it.value.decimals) }
        val appCurrency = userWalletManager.getUserAppCurrency()
        val rates = repository.getRates(appCurrency.code, tokensInWallet.map { it.id })
        cache.cacheBalances(networkId, derivationPath, tokensBalance)
        cache.cacheLoadedTokens(loadedTokens.map { TokenWithBalance(it) })
        cache.cacheInWalletTokens(getTokensWithBalance(tokensInWallet, tokensBalance, rates, appCurrency))
        return TokensDataState(
            preselectTokens = PreselectTokens(
                fromToken = initialCurrency,
                toToken = selectToToken(initialCurrency, tokensInWallet, loadedTokens),
            ),
            foundTokensState = FoundTokensState(
                tokensInWallet = cache.getInWalletTokens(),
                loadedTokens = cache.getLoadedTokens(),
            ),
        )
    }

    override suspend fun searchTokens(networkId: String, searchQuery: String): FoundTokensState {
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
        return FoundTokensState(
            tokensInWallet = tokensInWallet,
            loadedTokens = loadedTokens,
        )
    }

    override fun findTokenById(id: String): Currency? {
        val tokensInWallet = cache.getInWalletTokens()
        val loadedTokens = cache.getLoadedTokens()
        return tokensInWallet.firstOrNull { it.token.id == id }?.token
            ?: loadedTokens.firstOrNull { it.token.id == id }?.token
    }

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

    override suspend fun findBestQuote(
        networkId: String,
        fromToken: Currency,
        toToken: Currency,
        amountToSwap: String,
        selectedFee: FeeType,
    ): SwapState {
        syncWalletBalanceForTokens(networkId, listOf(fromToken, toToken))
        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.signum() == 0) {
            return createEmptyAmountState(networkId, fromToken, toToken)
        }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(fromToken))
        val fromTokenAddress = getTokenAddress(fromToken)
        val toTokenAddress = getTokenAddress(toToken)
        val isAllowedToSpend = isAllowedToSpend(networkId, fromToken, amount)
        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            transactionManager.updateWalletManager(networkId, derivationPath)
        }
        val isBalanceWithoutFeeEnough = isBalanceEnough(networkId, fromToken, amount, null)
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
            loadSwapData(
                networkId = networkId,
                fromTokenAddress = fromTokenAddress,
                toTokenAddress = toTokenAddress,
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                selectedFee = selectedFee,
            )
        } else {
            loadQuoteData(
                networkId = networkId,
                fromTokenAddress = fromTokenAddress,
                toTokenAddress = toTokenAddress,
                amount = amount,
                fromToken = fromToken,
                toToken = toToken,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
            )
        }
    }

    override suspend fun onSwap(
        networkId: String,
        swapStateData: SwapStateData,
        currencyToSend: Currency,
        currencyToGet: Currency,
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

    override fun getTokenBalance(networkId: String, token: Currency): SwapAmount {
        return cache.getBalanceForToken(
            networkId = networkId,
            derivationPath = derivationPath,
            symbol = token.symbol,
        ) ?: SwapAmount(BigDecimal.ZERO, getTokenDecimals(token))
    }

    override fun isAvailableToSwap(networkId: String): Boolean {
        return ONE_INCH_SUPPORTED_NETWORKS.contains(networkId)
    }

    override fun getSwapAmountForToken(amount: String, token: Currency): SwapAmount {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amount)) { "wrong amount format" }
        return SwapAmount(amountDecimal, getTokenDecimals(token))
    }

    private suspend fun onSuccessLegacyFlow(currency: Currency) {
        userWalletManager.addToken(swapCurrencyConverter.convert(currency), derivationPath)
        userWalletManager.refreshWallet()
    }

    private suspend fun onSuccessNewFlow(currency: Currency) {
        val network = network ?: return
        getSelectedWalletSyncUseCase().fold(
            ifRight = { userWallet ->
                getAndAddCryptoCurrency(userWallet, currency, network)
            },
            ifLeft = {
                Timber.e("Swap Error on getSelectedWalletUseCase")
            },
        )
    }

    private suspend fun getAndAddCryptoCurrency(userWallet: UserWallet, currency: Currency, network: Network) {
        repository.getCryptoCurrency(userWallet, currency, network)?.let { cryptoCurrency ->
            addCryptoCurrenciesUseCase(userWallet.walletId, cryptoCurrency)
        }
    }

    private fun getTangemFee(): Double {
        return repository.getTangemFee()
    }

    private fun getTokenDecimals(token: Currency): Int {
        return if (token is Currency.NonNativeToken) {
            token.decimalCount
        } else {
            transactionManager.getNativeTokenDecimals(token.networkId)
        }
    }

    private fun selectToToken(
        initialToken: Currency,
        tokensInWallet: List<Currency>,
        loadedTokens: List<Currency>,
    ): Currency {
        val toToken = if (tokensInWallet.isNotEmpty()) {
            tokensInWallet.firstOrNull { it.symbol != initialToken.symbol }
                ?: loadedTokens.first { it.symbol != initialToken.symbol }
        } else {
            val findUsdt = loadedTokens.firstOrNull { it.symbol == USDT_SYMBOL && it.symbol != initialToken.symbol }
            if (findUsdt == null) {
                val findUsdc = loadedTokens.firstOrNull { it.symbol == USDC_SYMBOL && it.symbol != initialToken.symbol }
                findUsdc ?: loadedTokens.first { it.symbol != initialToken.symbol }
            } else {
                findUsdt
            }
        }
        return toToken
    }

    private fun getTokensWithBalance(
        tokens: List<Currency>,
        balances: Map<String, SwapAmount>,
        rates: Map<String, Double>,
        appCurrency: ProxyFiatCurrency,
    ): List<TokenWithBalance> {
        return tokens.map {
            val balance = balances[it.symbol]
            TokenWithBalance(
                token = it,
                tokenBalanceData = TokenBalanceData(
                    amount = balance?.let { amount ->
                        amountFormatter.formatSwapAmountToUI(amount, it.symbol)
                    },
                    amountEquivalent = balance?.value?.toFiatString(
                        rateValue = rates[it.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                        fiatCurrencyName = appCurrency.symbol,
                        formatWithSpaces = true,
                    ),
                ),
            )
        }
    }

    private suspend fun isAllowedToSpend(networkId: String, fromToken: Currency, amount: SwapAmount): Boolean {
        if (fromToken is Currency.NativeToken) return true
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

    private fun createEmptyAmountState(networkId: String, fromToken: Currency, toToken: Currency): SwapState {
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
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: SwapAmount,
        fromToken: Currency,
        toToken: Currency,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
    ): SwapState {
        repository.findBestQuote(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount.toStringWithRightOffset(),
        ).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromTokenAmount = amount,
                    toTokenAmount = quoteDataModel.toTokenAmount,
                    swapStateData = null,
                )
                val quotesState = updatePermissionState(
                    networkId = networkId,
                    fromToken = fromToken,
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
                return SwapState.SwapError(quotes.error)
            }
        }
    }

    private suspend fun getFormattedFiatFees(networkId: String, vararg fees: BigDecimal): List<String> {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = userWalletManager.getNativeTokenForNetwork(networkId)
        val rates = repository.getRates(appCurrency.code, listOf(nativeToken.id))
        return rates[nativeToken.id]?.toBigDecimal()?.let { rate ->
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
        fromToken: Currency,
        toToken: Currency,
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
                    currencyToSend = swapCurrencyConverter.convert(fromToken),
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
                    isBalanceEnough(networkId, fromToken, amount, feeByPriority)
                val isFeeEnough = checkFeeIsEnough(
                    fee = feeByPriority,
                    spendAmount = amount,
                    networkId = networkId,
                    fromToken = fromToken,
                )
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
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
        fromToken: Currency,
        toToken: Currency,
        fromTokenAmount: SwapAmount,
        toTokenAmount: SwapAmount,
        swapStateData: SwapStateData?,
    ): SwapState.QuotesLoadedState {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = userWalletManager.getNativeTokenForNetwork(networkId)
        val rates = repository.getRates(appCurrency.code, listOf(fromToken.id, toToken.id, nativeToken.id))
        val fromTokenBalance = cache.getBalanceForToken(networkId, derivationPath, fromToken.symbol)
        val toTokenBalance = cache.getBalanceForToken(networkId, derivationPath, toToken.symbol)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                coinId = fromToken.id,
                tokenWalletBalance = fromTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }
                    ?: ZERO_BALANCE,
                tokenFiatBalance = fromTokenAmount.value.toFiatString(
                    rateValue = rates[fromToken.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                    fiatCurrencyName = appCurrency.symbol,
                    formatWithSpaces = true,
                ),
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                coinId = toToken.id,
                tokenWalletBalance = toTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }
                    ?: ZERO_BALANCE,
                tokenFiatBalance = toTokenAmount.value.toFiatString(
                    rateValue = rates[toToken.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                    fiatCurrencyName = appCurrency.symbol,
                    formatWithSpaces = true,
                ),
            ),
            priceImpact = calculatePriceImpact(
                fromTokenAmount = fromTokenAmount.value,
                fromRate = rates[fromToken.id] ?: 0.0,
                toTokenAmount = toTokenAmount.value,
                toRate = rates[toToken.id] ?: 0.0,
            ),
            networkCurrency = userWalletManager.getNetworkCurrency(networkId),
            swapDataModel = swapStateData,
            tangemFee = getTangemFee(),
        )
    }

    @Suppress("LongParameterList")
    private suspend fun updatePermissionState(
        networkId: String,
        fromToken: Currency,
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
            currencyToSend = userWalletManager.getNativeTokenForNetwork(networkId),
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

    private suspend fun syncWalletBalanceForTokens(networkId: String, tokens: List<Currency>) {
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
        fromToken: Currency,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(networkId, fromToken).value
        return if (fromToken is Currency.NonNativeToken) {
            tokenBalance >= amount.value
        } else {
            tokenBalance > amount.value.plus(fee ?: BigDecimal.ZERO)
        }
    }

    private suspend fun getWalletAddress(networkId: String): String {
        return userWalletManager.getWalletAddress(networkId, derivationPath)
    }

    private fun getTokenAddress(currency: Currency): String {
        return when (currency) {
            is Currency.NativeToken -> {
                DEFAULT_BLOCKCHAIN_INCH_ADDRESS
            }
            is Currency.NonNativeToken -> {
                currency.contractAddress
            }
        }
    }

    override suspend fun checkFeeIsEnough(
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        networkId: String,
        fromToken: Currency,
    ): Boolean {
        if (fee == null) {
            return false
        }
        val nativeTokenBalance = userWalletManager.getNativeTokenBalance(networkId, derivationPath)
        val percentsToFeeIncrease = BigDecimal.ONE
        return when (fromToken) {
            is Currency.NativeToken -> {
                nativeTokenBalance?.let { balance ->
                    return balance.value.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)
                } ?: false
            }
            is Currency.NonNativeToken -> {
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
        fromToken: Currency,
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

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
        private const val ZERO_BALANCE = "0"
        private const val DEFAULT_BLOCKCHAIN_INCH_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"

        @Suppress("UnusedPrivateMember")
        private const val INCREASE_FEE_TO_CHECK_ENOUGH_PERCENT = 1.0 // if need to increase fee when check isEnough
        private const val INCREASE_GAS_LIMIT_BY = 112 // 12%
        private const val USDT_SYMBOL = "USDT"
        private const val USDC_SYMBOL = "USDC"
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
