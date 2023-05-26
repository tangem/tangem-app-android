package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.CryptoCurrencyConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.PermissionOptions
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapApproveType
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.ProxyFees
import com.tangem.lib.crypto.models.ProxyFiatCurrency
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.toFiatString
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LargeClass")
internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val cache: SwapDataCache,
    private val allowPermissionsHandler: AllowPermissionsHandler,
) : SwapInteractor {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter()
    private val amountFormatter = AmountFormatter()
    private var derivationPath: String? = null

    override fun initDerivationPath(derivationPath: String?) {
        this.derivationPath = derivationPath
    }

    override suspend fun initTokensToSwap(initialCurrency: Currency): TokensDataState {
        val networkId = initialCurrency.networkId
        val availableTokens = cache.getAvailableTokens(networkId)
        val allLoadedTokens = availableTokens.ifEmpty {
            val tokens = repository.getExchangeableTokens(networkId)
            cache.cacheAvailableToSwapTokens(networkId, tokens)
            tokens
        }.filter { it.symbol != initialCurrency.symbol }

        // replace tokens in wallet tokens list with loaded same
        val loadedOnWalletsMap = mutableSetOf<String>()
        val tokensInWallet =
            userWalletManager.getUserTokens(
                networkId = networkId,
                derivationPath = derivationPath,
                isExcludeCustom = true,
            ).filter { it.symbol != initialCurrency.symbol }
                .map { token ->
                    allLoadedTokens.firstOrNull { it.symbol == token.symbol }?.let {
                        loadedOnWalletsMap.add(it.symbol)
                        it
                    } ?: cryptoCurrencyConverter.convertBack(token)
                }
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
            repository.dataToApprove(networkId, getTokenAddress(permissionOptions.fromToken)).data
        } else {
            permissionOptions.approveData.approveModel.data
        }
        val result = transactionManager.sendApproveTransaction(
            networkId = networkId,
            feeAmount = permissionOptions.txFee.feeValue,
            gasLimit = permissionOptions.txFee.gasLimit,
            destinationAddress = permissionOptions.approveData.approveModel.toAddress,
            dataToSign = dataToSign,
            derivationPath = derivationPath,
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
    ): SwapState {
        syncWalletBalanceForTokens(networkId, listOf(fromToken, toToken))
        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.compareTo(BigDecimal.ZERO) == 0) {
            return createEmptyAmountState(networkId, fromToken, toToken)
        }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(fromToken))
        val fromTokenAddress = getTokenAddress(fromToken)
        val toTokenAddress = getTokenAddress(toToken)
        val isAllowedToSpend = isAllowedToSpend(networkId, fromTokenAddress, amount)
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
        val amount = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format, use only digits" }
        val result = transactionManager.sendTransaction(
            networkId = networkId,
            amountToSend = amount,
            currencyToSend = cryptoCurrencyConverter.convert(currencyToSend),
            feeAmount = fee.feeValue,
            gasLimit = fee.gasLimit,
            destinationAddress = swapStateData.swapModel.transaction.toWalletAddress,
            dataToSign = swapStateData.swapModel.transaction.data,
            isSwap = true,
            derivationPath = derivationPath,
        )
        return when (result) {
            is SendTxResult.Success -> {
                userWalletManager.addToken(cryptoCurrencyConverter.convert(currencyToGet), derivationPath)
                userWalletManager.refreshWallet()
                TxState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        swapStateData.swapModel.fromTokenAmount,
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

    private suspend fun isAllowedToSpend(networkId: String, fromTokenAddress: String, amount: SwapAmount): Boolean {
        val allowance = repository.checkTokensSpendAllowance(
            networkId = networkId,
            tokenAddress = fromTokenAddress,
            walletAddress = userWalletManager.getWalletAddress(networkId, derivationPath),
        )
        val allowanceAmount = allowance.dataModel?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return allowance.error == DataError.NoError && allowanceAmount >= amount.value.movePointRight(amount.decimals)
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
                    fromTokenAmount = quoteDataModel.fromTokenAmount,
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
                    currencyToSend = cryptoCurrencyConverter.convert(fromToken),
                    destinationAddress = swapData.transaction.toWalletAddress,
                    increaseBy = INCREASE_GAS_LIMIT_BY,
                    data = swapData.transaction.data,
                    derivationPath = derivationPath,
                )
                val txFeeState = proxyFeesToFeeState(networkId, feeData)
                val isBalanceIncludeFeeEnough =
                    isBalanceEnough(networkId, fromToken, amount, txFeeState.priorityFee.feeValue)
                val isFeeEnough = checkFeeIsEnough(
                    fee = txFeeState.normalFee.feeValue,
                    spendAmount = amount,
                    networkId = networkId,
                    fromToken = fromToken,
                )
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromTokenAmount = swapData.fromTokenAmount,
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
        val isTokenZeroBalance = getTokenBalance(networkId, fromToken).value.compareTo(BigDecimal.ZERO) == 0
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
        val transactionData = repository.dataToApprove(
            networkId = networkId,
            tokenAddress = getTokenAddress(fromToken),
            amount = swapAmount.toStringWithRightOffset(),
        )
        val feeData = transactionManager.getFee(
            networkId = networkId,
            amountToSend = BigDecimal.ZERO,
            currencyToSend = userWalletManager.getNativeTokenForNetwork(networkId),
            destinationAddress = transactionData.toAddress,
            increaseBy = INCREASE_GAS_LIMIT_BY,
            data = transactionData.data,
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
                spenderAddress = transactionData.toAddress,
                requestApproveData = RequestApproveStateData(
                    fee = feeState,
                    approveModel = transactionData,
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
                    extraTokens = tokensToSync.map { cryptoCurrencyConverter.convert(it) },
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
            ),
            priorityFee = TxFee(
                feeValue = priorityFeeValue,
                gasLimit = priorityFeeGas,
                feeFiatFormatted = priorityFiatFee,
                feeCryptoFormatted = priorityCryptoFee,
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

    private fun getWalletAddress(networkId: String): String {
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

    private fun checkFeeIsEnough(
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        networkId: String,
        fromToken: Currency,
    ): Boolean {
        if (fee == null) {
            return false
        }
        val nativeTokenBalance = userWalletManager.getNativeTokenBalance(networkId, derivationPath)
        val percentsToFeeIncrease = BigDecimal.valueOf(INCREASE_FEE_TO_CHECK_ENOUGH_PERCENT)
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

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
        private const val ZERO_BALANCE = "0"
        private const val DEFAULT_BLOCKCHAIN_INCH_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
        private const val INCREASE_FEE_TO_CHECK_ENOUGH_PERCENT = 1.4
        private const val INCREASE_GAS_LIMIT_BY = 125 // 25%
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