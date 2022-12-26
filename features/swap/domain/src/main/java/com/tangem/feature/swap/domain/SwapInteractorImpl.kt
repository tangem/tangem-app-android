package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.CryptoCurrencyConverter
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.DataError
import com.tangem.feature.swap.domain.models.data.SwapState
import com.tangem.feature.swap.domain.models.data.TransactionModel
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import java.math.BigDecimal
import javax.inject.Inject

internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val cache: SwapDataCache,
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

    override suspend fun getTokenBalance(tokenId: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(): BigDecimal {
        return transactionManager.getFee(
            networkId = cache.getNetworkId() ?: error("no networkId found, call getTokensToSwap first"),
            amountToSend = cache.getAmountToSwap() ?: error("no amount found, call findBestQuote first"),
            currencyToSend = cryptoCurrencyConverter.convert(
                cache.getExchangeCurrencies()?.fromCurrency ?: error(
                    "no fromCurrency found, call findBestQuote first",
                ),
            ),
            destinationAddress = getTokenAddress(
                cache.getExchangeCurrencies()?.toCurrency ?: error(
                    "no toCurrency found, call findBestQuote first",
                ),
            ),
        ).value
    }

    override suspend fun givePermissionToSwap(tokenToApprove: Currency) {
        cache.getNetworkId()?.let { networkId ->
            if (tokenToApprove is Currency.NonNativeToken) {
                val estimatedGas =
                    requireNotNull(cache.getLastQuote()?.estimatedGas) { "estimatedGas not found call findBestQuote" }
                val transactionData = repository.dataToApprove(networkId, tokenToApprove.contractAddress)
                val gasPrice = transactionData.gasPrice.toBigDecimalOrNull() ?: error("cannot parse gasPrice")
                val fee = gasPrice.multiply(estimatedGas.toBigDecimal()).movePointLeft(tokenToApprove.decimalCount)
                val result = transactionManager.sendTransaction(
                    networkId = networkId,
                    amountToSend = BigDecimal.ZERO,
                    currencyToSend = cryptoCurrencyConverter.convert(tokenToApprove),
                    feeAmount = fee,
                    destinationAddress = transactionData.toAddress,
                    dataToSign = transactionData.data,
                )
            }
        }
    }

    override suspend fun findBestQuote(fromToken: Currency, toToken: Currency, amount: String): SwapState {
        val networkId = cache.getNetworkId()
        val fromTokenAddress = getTokenAddress(fromToken)
        val toTokenAddress = getTokenAddress(toToken)
        val bigDecimalAmount = amount.toBigDecimalOrNull() ?: error("wrong amount format, use only digits")
        val isAllowedToSpend = if (networkId.isNullOrEmpty()) {
            error("no networkId found, please call getTokensToSwap first")
        } else {
            val allowance =
                repository.checkTokensSpendAllowance(
                    networkId = networkId,
                    tokenAddress = fromTokenAddress,
                    walletAddress = userWalletManager.getWalletAddress(networkId),
                )
            allowance.error == DataError.NO_ERROR && allowance.dataModel != "0"
        }
        repository.findBestQuote(
            networkId = networkId,
            fromTokenAddress = fromTokenAddress,
            toTokenAddress = toTokenAddress,
            amount = amount,
        ).let { quotes ->
            val quoteDataModel = quotes.dataModel
            if (quoteDataModel != null) {
                cache.cacheSwapParams(
                    quoteModel = quoteDataModel.copy(isAllowedToSpend = isAllowedToSpend),
                    amount = bigDecimalAmount,
                    fromCurrency = fromToken,
                    toCurrency = toToken,
                )
                return quoteDataModel
            } else {
                return SwapState.SwapError(quotes.error)
            }
        }
    }

    override suspend fun onSwap(): SwapState {
        val quoteModel = cache.getLastQuote()
        val amountToSwap = cache.getAmountToSwap()
        val networkId = cache.getNetworkId()
        if (quoteModel != null && amountToSwap != null && networkId != null) {
            repository.prepareSwapTransaction(
                networkId = networkId,
                fromTokenAddress = quoteModel.fromTokenAmount,
                toTokenAddress = quoteModel.toTokenAmount,
                amount = amountToSwap.toPlainString(),
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
// [REDACTED_TODO_COMMENT]
    private fun signTransactionData(transaction: TransactionModel) {
    }

    companion object {
        private const val DEFAULT_SLIPPAGE = 2
    }
}
