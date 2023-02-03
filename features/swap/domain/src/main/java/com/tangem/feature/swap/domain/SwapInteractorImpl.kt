package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.CryptoCurrencyConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.AmountFormatter
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.PreselectTokens
import com.tangem.feature.swap.domain.models.ui.RequestApproveStateData
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenBalanceData
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import com.tangem.feature.swap.domain.models.ui.TokensDataState
import com.tangem.feature.swap.domain.models.ui.TxState
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyFiatCurrency
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.toFiatString
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
    private val amountFormatter = AmountFormatter()

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

    override suspend fun searchTokens(networkId: String, searchQuery: String): FoundTokensState {
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

    override fun findTokenById(id: String): Currency? {
        val tokensInWallet = cache.getInWalletTokens()
        val loadedTokens = cache.getLoadedTokens()
        return tokensInWallet.firstOrNull { it.id == id } ?: loadedTokens.firstOrNull { it.id == id }
    }

    override suspend fun givePermissionToSwap(
        networkId: String,
        estimatedGas: Int,
        transactionData: ApproveModel,
        forTokenContractAddress: String,
    ): TxState {
        val increasedEstimatedGas = increaseByPercents(TWENTY_FIVE_PERCENTS, estimatedGas)
        val gasPrice = transactionData.gasPrice.toBigDecimalOrNull() ?: error("cannot parse gasPrice")
        val fee = transactionManager.calculateFee(networkId, gasPrice.toPlainString(), increasedEstimatedGas)
        val result = transactionManager.sendApproveTransaction(
            networkId = networkId,
            feeAmount = fee,
            estimatedGas = increasedEstimatedGas,
            destinationAddress = transactionData.toAddress,
            dataToSign = transactionData.data,
        )
        return when (result) {
            is SendTxResult.Success -> {
                allowPermissionsHandler.addAddressToInProgress(forTokenContractAddress)
                TxState.TxSent(txAddress = userWalletManager.getLastTransactionHash(networkId) ?: "")
            }
            SendTxResult.UserCancelledError -> TxState.UserCancelled
            is SendTxResult.BlockchainSdkError -> TxState.BlockchainError
            is SendTxResult.TangemSdkError -> TxState.TangemSdkError
            is SendTxResult.UnknownError -> TxState.UnknownError
        }
    }

    override suspend fun findBestQuote(
        networkId: String,
        fromToken: Currency,
        toToken: Currency,
        amountToSwap: String,
    ): SwapState {
        val amountDecimal = amountToSwap.toBigDecimalOrNull()
        if (amountDecimal == null || amountDecimal.compareTo(BigDecimal.ZERO) == 0) {
            return createEmptyAmountState(networkId, fromToken, toToken)
        }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(fromToken))
        val fromTokenAddress = getTokenAddress(fromToken)
        val toTokenAddress = getTokenAddress(toToken)
        val isAllowedToSpend = checkAllowance(networkId, fromTokenAddress)
        val fee = getAndUpdateFee(networkId, fromToken)
        val isBalanceEnough = isBalanceEnough(fromToken, networkId, amount, fee)
        val isFeeEnough = checkFeeIsEnough(fee, amount, networkId, fromToken)
        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            transactionManager.updateWalletManager(networkId)
        }
        val preparedSwapConfigState = PreparedSwapConfigState(
            isAllowedToSpend = isAllowedToSpend,
            isBalanceEnough = isBalanceEnough,
            isFeeEnough = isFeeEnough,
        )
        return if (isAllowedToSpend && isBalanceEnough && isFeeEnough) {
            loadSwapData(
                networkId = networkId,
                fromTokenAddress = fromTokenAddress,
                toTokenAddress = toTokenAddress,
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                preparedSwapConfigState = preparedSwapConfigState,
            )
        } else {
            loadQuoteData(
                networkId = networkId,
                fromTokenAddress = fromTokenAddress,
                toTokenAddress = toTokenAddress,
                amount = amount,
                fromToken = fromToken,
                toToken = toToken,
                preparedSwapConfigState = preparedSwapConfigState,
            )
        }
    }

    override suspend fun onSwap(
        networkId: String,
        swapData: SwapDataModel,
        currencyToSend: Currency,
        currencyToGet: Currency,
        amountToSwap: String,
    ): TxState {
        val amount = requireNotNull(amountToSwap.toBigDecimalOrNull()) { "wrong amount format, use only digits" }
        val estimatedGas =
            increaseByPercents(TWENTY_FIVE_PERCENTS, swapData.transaction.gas.toIntOrNull() ?: DEFAULT_GAS)
        val fee = transactionManager.calculateFee(
            networkId = networkId,
            gasPrice = swapData.transaction.gasPrice,
            estimatedGas = estimatedGas,
        )
        val result = transactionManager.sendTransaction(
            networkId = networkId,
            amountToSend = amount,
            currencyToSend = cryptoCurrencyConverter.convert(currencyToSend),
            feeAmount = fee,
            estimatedGas = estimatedGas,
            destinationAddress = swapData.transaction.toWalletAddress,
            dataToSign = swapData.transaction.data,
            isSwap = true,
        )
        return when (result) {
            is SendTxResult.Success -> {
                userWalletManager.addToken(cryptoCurrencyConverter.convert(currencyToGet))
                TxState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(swapData.fromTokenAmount, currencyToSend.symbol),
                    toAmount = amountFormatter.formatSwapAmountToUI(swapData.toTokenAmount, currencyToGet.symbol),
                    txAddress = userWalletManager.getLastTransactionHash(networkId) ?: "",
                )
            }
            SendTxResult.UserCancelledError -> TxState.UserCancelled
            is SendTxResult.BlockchainSdkError -> TxState.BlockchainError
            is SendTxResult.TangemSdkError -> TxState.TangemSdkError
            is SendTxResult.UnknownError -> TxState.UnknownError
        }
    }

    override fun getTokenBalance(token: Currency): SwapAmount {
        return userWalletManager.getCurrentWalletTokensBalance(token.networkId)[token.symbol]?.let {
            SwapAmount(it.value, it.decimals)
        } ?: SwapAmount(BigDecimal.ZERO, getTokenDecimals(token))
    }

    override fun isAvailableToSwap(networkId: String): Boolean {
        return ONE_INCH_SUPPORTED_NETWORKS.contains(networkId)
    }

    fun getTokenDecimals(token: Currency): Int {
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
        balances: Map<String, ProxyAmount>,
        rates: Map<String, Double>,
        appCurrency: ProxyFiatCurrency,
    ): List<TokenWithBalance> {
        return tokens.map {
            val balance = balances[it.symbol]
            TokenWithBalance(
                token = it,
                tokenBalanceData = TokenBalanceData(
                    amount = balance?.let { amount -> amountFormatter.formatProxyAmountToUI(amount, "") },
                    amountEquivalent = balance?.value?.toFiatString(
                        rates[it.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                        appCurrency.symbol,
                    ),
                ),
            )
        }
    }

    private suspend fun getAndUpdateFee(networkId: String, fromToken: Currency): BigDecimal? {
        val lastFee = cache.getLastFeeForNetwork(networkId)
        if (lastFee == null) {
            if (userWalletManager.getNativeTokenBalance(networkId)?.value?.compareTo(BigDecimal.ZERO) == 0) {
                return null
            }
            val transactionData = repository.dataToApprove(networkId, getTokenAddress(fromToken))
            val fee = transactionManager.getFee(
                networkId,
                BigDecimal.ZERO,
                cryptoCurrencyConverter.convert(fromToken),
                transactionData.toAddress,
            ).value
            cache.cacheLastFeeForNetwork(fee, networkId)
            return fee
        }
        return lastFee
    }

    private suspend fun checkAllowance(networkId: String, fromTokenAddress: String): Boolean {
        val allowance = repository.checkTokensSpendAllowance(
            networkId = networkId,
            tokenAddress = fromTokenAddress,
            walletAddress = userWalletManager.getWalletAddress(networkId),
        )
        return allowance.error == DataError.NoError && allowance.dataModel != ZERO_BALANCE
    }

    private fun createEmptyAmountState(
        networkId: String,
        fromToken: Currency,
        toToken: Currency,
    ): SwapState {
        val tokensBalance = userWalletManager.getCurrentWalletTokensBalance(networkId)
        val fromTokenBalance = tokensBalance[fromToken.symbol]?.let {
            SwapAmount(it.value, it.decimals)
        }
        val toTokenBalance = tokensBalance[toToken.symbol]?.let {
            SwapAmount(it.value, it.decimals)
        }
        return SwapState.EmptyAmountState(
            fromTokenWalletBalance = fromTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }.orEmpty(),
            toTokenWalletBalance = toTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }.orEmpty(),
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
        preparedSwapConfigState: PreparedSwapConfigState,
    ): SwapState {
        repository.findBestQuote(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount.toStringWithRightOffset(),
        ).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                val transactionData = repository.dataToApprove(networkId, getTokenAddress(fromToken))
                val fee = transactionManager.calculateFee(
                    networkId = networkId,
                    estimatedGas = quoteDataModel.estimatedGas,
                    gasPrice = transactionData.gasPrice,
                )
                val feeFiat = getFormattedFiatFee(networkId, fromToken.id, toToken.id, fee)
                val formattedFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = fee,
                    decimals = transactionManager.getNativeTokenDecimals(networkId),
                    currency = userWalletManager.getNetworkCurrency(networkId),
                ) + feeFiat
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromTokenAmount = quoteDataModel.fromTokenAmount,
                    toTokenAmount = quoteDataModel.toTokenAmount,
                    formattedFee = formattedFee,
                    preparedSwapConfigState = preparedSwapConfigState,
                    swapDataModel = null,
                )
                return updatePermissionState(
                    networkId = networkId,
                    fromToken = fromToken,
                    quotesLoadedState = swapState,
                    estimatedGas = quoteDataModel.estimatedGas,
                    transactionData = transactionData,
                    formattedFee = formattedFee,
                )
            } else {
                return SwapState.SwapError(quotes.error)
            }
        }
    }

    private suspend fun getFormattedFiatFee(
        networkId: String,
        fromTokenId: String,
        toTokenId: String,
        fee: BigDecimal,
    ): String {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = userWalletManager.getNativeTokenForNetwork(networkId)
        val rates = repository.getRates(appCurrency.code, listOf(fromTokenId, toTokenId, nativeToken.id))
        return rates[nativeToken.id]?.toBigDecimal()?.let { rate ->
            " (${fee.toFiatString(rate, appCurrency.symbol)})"
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
        preparedSwapConfigState: PreparedSwapConfigState,
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
                val fee = transactionManager.calculateFee(
                    networkId = networkId,
                    estimatedGas = swapData.transaction.gas.toIntOrNull() ?: DEFAULT_GAS,
                    gasPrice = swapData.transaction.gasPrice,
                )
                val feeFiat = getFormattedFiatFee(networkId, fromToken.id, toToken.id, fee)
                val formattedFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = fee,
                    decimals = transactionManager.getNativeTokenDecimals(networkId),
                    currency = userWalletManager.getNetworkCurrency(networkId),
                ) + feeFiat
                val swapState = updateBalances(
                    networkId = networkId,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromTokenAmount = swapData.fromTokenAmount,
                    toTokenAmount = swapData.toTokenAmount,
                    formattedFee = formattedFee,
                    preparedSwapConfigState = preparedSwapConfigState,
                    swapDataModel = swapData,
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
        formattedFee: String,
        preparedSwapConfigState: PreparedSwapConfigState,
        swapDataModel: SwapDataModel?,
    ): SwapState.QuotesLoadedState {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = userWalletManager.getNativeTokenForNetwork(networkId)
        val rates = repository.getRates(appCurrency.code, listOf(fromToken.id, toToken.id, nativeToken.id))
        val tokensBalance = userWalletManager.getCurrentWalletTokensBalance(networkId)
        val fromTokenBalance = tokensBalance[fromToken.symbol]?.let {
            amountFormatter.formatProxyAmountToUI(it, "")
        }
        val toTokenBalance = tokensBalance[toToken.symbol]?.let {
            amountFormatter.formatProxyAmountToUI(it, "")
        }
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                coinId = fromToken.id,
                tokenWalletBalance = fromTokenBalance ?: ZERO_BALANCE,
                tokenFiatBalance = fromTokenAmount.value.toFiatString(
                    rates[fromToken.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                    appCurrency.symbol,
                ),
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                coinId = toToken.id,
                tokenWalletBalance = toTokenBalance ?: ZERO_BALANCE,
                tokenFiatBalance = toTokenAmount.value.toFiatString(
                    rates[toToken.id]?.toBigDecimal() ?: BigDecimal.ZERO,
                    appCurrency.symbol,
                ),
            ),
            fee = formattedFee,
            networkCurrency = userWalletManager.getNetworkCurrency(networkId),
            preparedSwapConfigState = preparedSwapConfigState,
            swapDataModel = swapDataModel,
        )
    }

    @Suppress("LongParameterList")
    private fun updatePermissionState(
        networkId: String,
        fromToken: Currency,
        quotesLoadedState: SwapState.QuotesLoadedState,
        estimatedGas: Int,
        transactionData: ApproveModel,
        formattedFee: String,
    ): SwapState.QuotesLoadedState {
        if (allowPermissionsHandler.isAddressAllowanceInProgress(getTokenAddress(fromToken))) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.PermissionLoading,
            )
        }
        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionReadyForRequest(
                currency = fromToken.symbol,
                amount = INFINITY_SYMBOL,
                walletAddress = getWalletAddress(networkId),
                spenderAddress = transactionData.toAddress,
                fee = formattedFee,
                requestApproveData = RequestApproveStateData(
                    estimatedGas = estimatedGas,
                    approveModel = transactionData,
                ),
            ),
        )
    }

    private fun isBalanceEnough(fromToken: Currency, networkId: String, amount: SwapAmount, fee: BigDecimal?): Boolean {
        val tokenBalance =
            userWalletManager.getCurrentWalletTokensBalance(networkId)[fromToken.symbol]?.value ?: BigDecimal.ZERO
        return if (fromToken is Currency.NonNativeToken) {
            tokenBalance >= amount.value
        } else {
            tokenBalance > amount.value.plus(fee ?: BigDecimal.ZERO)
        }
    }

    private fun getWalletAddress(networkId: String): String {
        return userWalletManager.getWalletAddress(networkId)
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
        val nativeTokenBalance = userWalletManager.getNativeTokenBalance(networkId)
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
        return false
    }

    @Suppress("MagicNumber")
    private fun increaseByPercents(percents: Int, value: Int): Int {
        return value * (percents / 100 + 1)
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
        private const val ZERO_BALANCE = "0"
        private const val DEFAULT_GAS = 300000
        private const val DEFAULT_BLOCKCHAIN_INCH_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
        private const val TWENTY_FIVE_PERCENTS = 25
        private const val INCREASE_FEE_TO_CHECK_ENOUGH_PERCENT = 1.5
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
