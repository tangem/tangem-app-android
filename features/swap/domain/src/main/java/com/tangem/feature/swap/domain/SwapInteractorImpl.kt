package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.CryptoCurrencyConverter
import com.tangem.feature.swap.domain.models.ApproveModel
import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.PermissionDataState
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.SwapState
import com.tangem.feature.swap.domain.models.TransactionModel
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.toFiatString
import com.tangem.utils.toFormattedString
import java.math.BigDecimal
import javax.inject.Inject

internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val cache: SwapDataCache,
    private val allowPermissionsHandler: AllowPermissionsHandler,
) : SwapInteractor {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter()

    override suspend fun getTokensToSwap(networkId: String): List<Currency> {
        val availableTokens = cache.getAvailableTokens(networkId)
        return availableTokens.ifEmpty {
            val tokens = repository.getExchangeableTokens(networkId)
            cache.cacheAvailableToSwapTokens(networkId, tokens)
            cache.cacheNetworkId(networkId)
            tokens
        }
    }

    override suspend fun givePermissionToSwap(tokenToApprove: Currency) {
        cache.getNetworkId()?.let { networkId ->
            if (tokenToApprove is Currency.NonNativeToken) {
                val estimatedGas =
                    requireNotNull(cache.getLastQuote()?.estimatedGas) { "estimatedGas not found call findBestQuote" }
                val transactionData =
                    requireNotNull(cache.getApproveTransactionData()) { "getApproveTransactionData not found, call findQuotes" }
                val gasPrice = transactionData.gasPrice.toBigDecimalOrNull() ?: error("cannot parse gasPrice")
                val fee = transactionManager.calculateFee(networkId, gasPrice.toPlainString(), estimatedGas)
                val result = transactionManager.sendTransaction(
                    networkId = networkId,
                    amountToSend = BigDecimal.ZERO,
                    currencyToSend = cryptoCurrencyConverter.convert(tokenToApprove),
                    feeAmount = fee,
                    destinationAddress = transactionData.toAddress,
                    dataToSign = transactionData.data,
                )
                when (result) {
                    SendTxResult.Success -> {
                        allowPermissionsHandler.addAddressToInProgress(tokenToApprove.contractAddress)
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
        val networkId = requireNotNull(cache.getNetworkId()) { "no networkId found, please call getTokensToSwap first" }
        val fromTokenAddress = getTokenAddress(fromToken)
        val toTokenAddress = getTokenAddress(toToken)
        val isAllowedToSpend = checkAllowance(networkId, fromTokenAddress)
        val isBalanceZero = isNotZeroBalance(fromToken, networkId)
        return if (isAllowedToSpend && isBalanceZero) {
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
            )
        }
    }

    override suspend fun onSwap(): SwapState {
        val quoteModel = cache.getLastQuote()
        val amountToSwap = cache.getAmountToSwap()
        val networkId = cache.getNetworkId()
        if (quoteModel != null && amountToSwap != null && networkId != null) {
            repository.prepareSwapTransaction(
                networkId = networkId,
                fromTokenAddress = quoteModel.fromTokenAddress,
                toTokenAddress = quoteModel.toTokenAddress,
                amount = amountToSwap.toStringWithRightOffset(),
                slippage = DEFAULT_SLIPPAGE,
                fromWalletAddress = getWalletAddress(networkId),
            ).let {
                val swapData = it.dataModel
                if (swapData != null) {
                    signTransactionData(swapData.transaction) //todo implement
                    return SwapState.SwapSuccess(
                        quoteModel.fromTokenAmount,
                        quoteModel.toTokenAmount,
                    )
                } else {
                    return SwapState.SwapError(it.error)
                }
            }
        } else {
            throw IllegalStateException("cache is empty, call 'findBestQuote' first")
        }
    }

    override fun getTokenDecimals(token: Currency): Int {
        return if (token is Currency.NonNativeToken) {
            token.decimalCount
        } else {
            transactionManager.getNativeTokenDecimals(token.networkId)
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
    private suspend fun loadQuoteData(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: SwapAmount,
        fromToken: Currency,
        toToken: Currency,
    ): SwapState {
        repository.findBestQuote(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount.toStringWithRightOffset(),
        ).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                cache.cacheSwapParams(
                    quoteModel = quoteDataModel,
                    amount = amount,
                    fromCurrency = fromToken,
                    toCurrency = toToken,
                )
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
                    isAllowedToSpend = false,
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
                    isAllowedToSpend = false,
                )
                return swapState.copy(
                    permissionState = PermissionDataState.Empty,
                )
            } else {
                return SwapState.SwapError(it.error)
            }
        }
    }

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
            fee = fee.toPlainString(),
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
                amount = "infinite", //FIXME
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
                transactionManager.getNativeAddress(currency.networkId)
            }
            is Currency.NonNativeToken -> {
                currency.contractAddress
            }
        }
    }

    //todo implement after merge referral
    private fun signTransactionData(transaction: TransactionModel) {
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
        private const val ZERO_BALANCE = "0"
        private const val DEFAULT_GAS = 300000
    }
}