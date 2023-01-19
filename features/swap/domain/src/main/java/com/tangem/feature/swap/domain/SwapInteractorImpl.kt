package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.CryptoCurrencyConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.cache.ExchangeCurrencies
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.PreselectTokens
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenBalanceData
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import com.tangem.feature.swap.domain.models.ui.TokensDataState
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyFiatCurrency
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.toFiatString
import com.tangem.utils.toFormattedString
import java.math.BigDecimal
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

    override suspend fun initTokensToSwap(initialCurrency: Currency): TokensDataState {
        val networkId = initialCurrency.networkId
        cache.cacheNetworkId(networkId)
        val availableTokens = cache.getAvailableTokens(networkId)
        val allLoadedTokens = availableTokens.ifEmpty {
            val tokens = repository.getExchangeableTokens(networkId)
            cache.cacheAvailableToSwapTokens(networkId, tokens)
            tokens
        }.filter { it.symbol != initialCurrency.symbol }

        // replace tokens in wallet tokens list with loaded same
        val loadedOnWalletsMap = mutableSetOf<String>()
        val tokensInWallet = userWalletManager.getUserTokens(networkId)
            .filter { it.symbol != initialCurrency.symbol }
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
        cache.cacheLoadedTokens(loadedTokens)
        cache.cacheInWalletTokens(tokensInWallet)
        val tokensBalance = userWalletManager.getCurrentWalletTokensBalance(networkId)
        val appCurrency = userWalletManager.getUserAppCurrency()
        val rates = repository.getRates(appCurrency.code, tokensInWallet.map { it.id })
        return TokensDataState(
            preselectTokens = PreselectTokens(
                fromToken = initialCurrency,
                toToken = selectToToken(initialCurrency, tokensInWallet, loadedTokens),
            ),
            foundTokensState = FoundTokensState(
                tokensInWallet = getTokensWithBalance(tokensInWallet, tokensBalance, rates, appCurrency),
                loadedTokens = loadedTokens.map { TokenWithBalance(it) },
            ),
        )
    }

    override suspend fun onSearchToken(searchQuery: String): FoundTokensState {
        val networkId = requireNotNull(cache.getNetworkId()) { "networkId is null" }
        val searchQueryLowerCase = searchQuery.lowercase()
        val tokensInWallet = cache.getInWalletTokens()
            .filter {
                it.name.lowercase().contains(searchQueryLowerCase) ||
                    it.symbol.lowercase().contains(searchQueryLowerCase)
            }
        val loadedTokens = cache.getLoadedTokens()
            .filter {
                it.name.lowercase().contains(searchQueryLowerCase) ||
                    it.symbol.lowercase().contains(searchQueryLowerCase)
            }
        val tokensBalance = userWalletManager.getCurrentWalletTokensBalance(networkId)
        val appCurrency = userWalletManager.getUserAppCurrency()
        val rates = repository.getRates(appCurrency.code, tokensInWallet.map { it.id })
        return FoundTokensState(
            tokensInWallet = getTokensWithBalance(tokensInWallet, tokensBalance, rates, appCurrency),
            loadedTokens = loadedTokens.map { TokenWithBalance(it) },
        )
    }

    override fun getExchangeCurrencies(): ExchangeCurrencies? {
        return cache.getExchangeCurrencies()
    }

    override fun findTokenById(id: String): Currency? {
        val tokensInWallet = cache.getInWalletTokens()
        val loadedTokens = cache.getLoadedTokens()
        return tokensInWallet.firstOrNull { it.id == id } ?: loadedTokens.firstOrNull { it.id == id }
    }

    override suspend fun givePermissionToSwap() {
        cache.getNetworkId()?.let { networkId ->
            val currencyToSend =
                requireNotNull(cache.getExchangeCurrencies()?.fromCurrency) { "currency is not selected" }
            if (currencyToSend is Currency.NonNativeToken) {
                val estimatedGas =
                    increaseByPercents(
                        TWENTY_FIVE_PERCENTS,
                        requireNotNull(cache.getLastQuote()?.estimatedGas) {
                            "estimatedGas not found call findBestQuote"
                        },
                    )
                val transactionData = requireNotNull(cache.getApproveTransactionData()) {
                    "getApproveTransactionData not found, call findQuotes"
                }
                val gasPrice = transactionData.gasPrice.toBigDecimalOrNull() ?: error("cannot parse gasPrice")
                val fee = transactionManager.calculateFee(networkId, gasPrice.toPlainString(), estimatedGas)
                val result = transactionManager.sendTransaction(
                    networkId = networkId,
                    amountToSend = BigDecimal.ZERO,
                    currencyToSend = cryptoCurrencyConverter.convert(currencyToSend),
                    feeAmount = fee,
                    estimatedGas = estimatedGas,
                    destinationAddress = transactionData.toAddress,
                    dataToSign = transactionData.data,
                )
                when (result) {
                    SendTxResult.Success -> {
                        allowPermissionsHandler.addAddressToInProgress(currencyToSend.contractAddress)
                    }
                    SendTxResult.UserCancelledError -> TODO()
                    is SendTxResult.BlockchainSdkError -> TODO()
                    is SendTxResult.TangemSdkError -> TODO()
                    is SendTxResult.UnknownError -> TODO()
                }
            }
        }
    }

    override suspend fun findBestQuote(fromToken: Currency, toToken: Currency, amount: SwapAmount): SwapState {
        val networkId = requireNotNull(cache.getNetworkId()) { "no networkId found, please call initTokens first" }
        val fromTokenAddress = getTokenAddress(fromToken)
        val toTokenAddress = getTokenAddress(toToken)
        val isAllowedToSpend = checkAllowance(networkId, fromTokenAddress)
        val isNotZeroBalance = isNotZeroBalance(fromToken, networkId)
        cache.cacheExchangeCurrencies(fromToken, toToken)
        cache.cacheAmountToSwap(amount)
        return if (isAllowedToSpend && isNotZeroBalance) {
            if (allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
                allowPermissionsHandler.removeAddressFromProgress(toTokenAddress)
            }
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
            )
        }
    }

    override suspend fun onSwap(): SwapState {
        val swapData = requireNotNull(cache.getLastSwapData()) { "swap data is not ready" }
        val networkId = requireNotNull(cache.getNetworkId()) { "no networkId found, please call getTokensToSwap first" }
        val currencyToSend = requireNotNull(cache.getExchangeCurrencies()?.fromCurrency) { "currency is not selected" }
        val amountToSwap = requireNotNull(cache.getAmountToSwap()) { "" }
        val estimatedGas =
            increaseByPercents(TWENTY_FIVE_PERCENTS, swapData.transaction.gas.toIntOrNull() ?: DEFAULT_GAS)
        val fee = transactionManager.calculateFee(
            networkId = networkId,
            gasPrice = swapData.transaction.gasPrice,
            estimatedGas = estimatedGas,
        )
        val result = transactionManager.sendTransaction(
            networkId = networkId,
            amountToSend = amountToSwap.value,
            currencyToSend = cryptoCurrencyConverter.convert(currencyToSend),
            feeAmount = fee,
            estimatedGas = estimatedGas,
            destinationAddress = swapData.transaction.toWalletAddress,
            dataToSign = swapData.transaction.data,
        )
        when (result) {
            SendTxResult.Success -> {
            }
            SendTxResult.UserCancelledError -> TODO()
            is SendTxResult.BlockchainSdkError -> TODO()
            is SendTxResult.TangemSdkError -> TODO()
            is SendTxResult.UnknownError -> TODO()
        }
        return SwapState.SwapError(DataError.UNKNOWN_ERROR)
    }

    override fun getTokenDecimals(token: Currency): Int {
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
            val findUsdt = loadedTokens.firstOrNull { it.symbol == "USDT" && it.symbol != initialToken.symbol }
            if (findUsdt == null) {
                val findUsdc = loadedTokens.firstOrNull { it.symbol == "USDC" && it.symbol != initialToken.symbol }
                findUsdc ?: loadedTokens.first { it.symbol != initialToken.symbol }
            } else {
                findUsdt
            }
        }
        return toToken
    }

    private fun getTokensWithBalance(
        tokens: List<Currency>,
        balances: Map<String, ProxyAmount>,
        rates: Map<String, Double>,
        appCurrency: ProxyFiatCurrency,
    ): List<TokenWithBalance> {
        return tokens.map {
            val balance = balances[it.id]
            TokenWithBalance(
                token = it,
                tokenBalanceData = TokenBalanceData(
                    amount = balance?.let { b -> b.value.toFormattedString(b.decimals) },
                    amountEquivalent = balance?.value?.toFiatString(
                        rates[it.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                        appCurrency.symbol,
                    ),
                ),
            )
        }
    }

    private suspend fun checkAllowance(networkId: String, fromTokenAddress: String): Boolean {
        val allowance = repository.checkTokensSpendAllowance(
            networkId = networkId,
            tokenAddress = fromTokenAddress,
            walletAddress = userWalletManager.getWalletAddress(networkId),
        )
        return allowance.error == DataError.NO_ERROR && allowance.dataModel != ZERO_BALANCE
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
    ): SwapState {
        repository.findBestQuote(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount.toStringWithRightOffset(),
        ).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                cache.cacheQuoteData(quoteModel = quoteDataModel)
                val transactionData = repository.dataToApprove(networkId, getTokenAddress(fromToken))
                cache.cacheApproveTransactionData(transactionData)
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromTokenAmount = quoteDataModel.fromTokenAmount,
                    toTokenAmount = quoteDataModel.toTokenAmount,
                    fee = transactionManager.calculateFee(
                        networkId = networkId,
                        estimatedGas = quoteDataModel.estimatedGas,
                        gasPrice = transactionData.gasPrice,
                    ),
                    isAllowedToSpend = isAllowedToSpend,
                )
                return updatePermissionState(
                    networkId = networkId,
                    fromToken = fromToken,
                    quotesLoadedState = swapState,
                    estimatedGas = quoteDataModel.estimatedGas,
                    transactionData = transactionData,
                )
            } else {
                return SwapState.SwapError(quotes.error)
            }
        }
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
                cache.cacheSwapData(swapData)
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromTokenAmount = swapData.fromTokenAmount,
                    toTokenAmount = swapData.toTokenAmount,
                    fee = transactionManager.calculateFee(
                        networkId = networkId,
                        estimatedGas = swapData.transaction.gas.toIntOrNull() ?: DEFAULT_GAS,
                        gasPrice = swapData.transaction.gasPrice,
                    ),
                    isAllowedToSpend = true,
                )
                return swapState.copy(
                    permissionState = PermissionDataState.Empty,
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
        fee: BigDecimal,
        isAllowedToSpend: Boolean,
    ): SwapState.QuotesLoadedState {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val rates = repository.getRates(appCurrency.code, listOf(fromToken.id, toToken.id))
        val tokensBalance = userWalletManager.getCurrentWalletTokensBalance(networkId)
        val fromTokenBalance = tokensBalance[fromToken.symbol]?.let {
            it.value.toFormattedString(it.decimals)
        }
        val toTokenBalance = tokensBalance[toToken.symbol]?.let {
            it.value.toFormattedString(it.decimals)
        }
        return SwapState.QuotesLoadedState(
            fromTokenAmount = fromTokenAmount,
            toTokenAmount = toTokenAmount,
            fromTokenAddress = getTokenAddress(fromToken),
            toTokenAddress = getTokenAddress(toToken),
            fee = "${fee.toPlainString()} ${userWalletManager.getCurrencyByNetworkId(networkId)}",
            isAllowedToSpend = isAllowedToSpend,
            fromTokenWalletBalance = fromTokenBalance ?: ZERO_BALANCE,
            fromTokenFiatBalance = fromTokenAmount.value.toFiatString(
                rates[fromToken.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                appCurrency.symbol,
            ),
            toTokenWalletBalance = toTokenBalance ?: ZERO_BALANCE,
            toTokenFiatBalance = fromTokenAmount.value.toFiatString(
                rates[fromToken.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                appCurrency.symbol,
            ),
        )
    }

    private fun updatePermissionState(
        networkId: String,
        fromToken: Currency,
        quotesLoadedState: SwapState.QuotesLoadedState,
        estimatedGas: Int,
        transactionData: ApproveModel,
    ): SwapState.QuotesLoadedState {
        if (allowPermissionsHandler.isAddressAllowanceInProgress(fromToken.networkId)) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.PermissionLoading,
            )
        }
        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionReadyForRequest(
                currency = userWalletManager.getCurrencyByNetworkId(networkId),
                amount = "infinite", // FIXME
                walletAddress = getWalletAddress(networkId),
                spenderAddress = transactionData.toAddress,
                fee = transactionManager.calculateFee(
                    networkId = networkId,
                    gasPrice = transactionData.gasPrice,
                    estimatedGas = estimatedGas,
                ).toPlainString(),
            ),
        )
    }

    private fun isNotZeroBalance(fromToken: Currency, networkId: String): Boolean {
        /** to compare [BigDecimal] use only comparator */
        return (userWalletManager.getCurrentWalletTokensBalance(networkId)[fromToken.symbol]?.value
            ?: BigDecimal.ZERO).compareTo(BigDecimal.ZERO) != 0
    }

    private fun getWalletAddress(networkId: String): String {
        return userWalletManager.getWalletAddress(networkId)
    }

    private fun getTokenAddress(currency: Currency): String {
        return when (currency) {
            is Currency.NativeToken -> {
                DEFAULT_BLOCKCHAIN_ADDRESS
            }
            is Currency.NonNativeToken -> {
                currency.contractAddress
            }
        }
    }

    @Suppress("MagicNumber")
    private fun increaseByPercents(percents: Int, value: Int): Int {
        return value * (percents / 100 + 1)
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
        private const val ZERO_BALANCE = "0"
        private const val DEFAULT_GAS = 300000
        private const val DEFAULT_BLOCKCHAIN_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
        private const val TWENTY_FIVE_PERCENTS = 25
    }
}
