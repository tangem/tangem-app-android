package com.tangem.feature.swap

import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.oneinch.OneInchErrorsHandler
import com.tangem.datasource.api.oneinch.errors.OneIncResponseException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.config.ConfigManager
import com.tangem.feature.swap.converters.ApproveConverter
import com.tangem.feature.swap.converters.QuotesConverter
import com.tangem.feature.swap.converters.SwapConverter
import com.tangem.feature.swap.converters.TokensConverter
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.mapErrors
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class SwapRepositoryImpl @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val oneInchApiFactory: OneInchApiFactory,
    private val oneInchErrorsHandler: OneInchErrorsHandler,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val configManager: ConfigManager,
) : SwapRepository {

    private val tokensConverter = TokensConverter()
    private val quotesConverter = QuotesConverter()
    private val swapConverter = SwapConverter()
    private val approveConverter = ApproveConverter()

    override suspend fun getRates(currencyId: String, tokenIds: List<String>): Map<String, Double> {
        // workaround cause backend do not return arbitrum and optimism rates
        val addedTokens = if (tokenIds.contains(OPTIMISM_ID) || tokenIds.contains(ARBITRUM_ID)) {
            tokenIds.toMutableList().apply {
                add(ETHEREUM_ID)
            }
        } else {
            tokenIds
        }
        return withContext(coroutineDispatcher.io) {
            val rates = tangemTechApi.getRates(currencyId.lowercase(), addedTokens.joinToString(",")).rates
            val ethRate = rates[ETHEREUM_ID]
            rates.mapValues {
                if (it.key == OPTIMISM_ID || it.key == ARBITRUM_ID) {
                    ethRate ?: 0.0
                } else {
                    it.value
                }
            }
        }
    }

    override suspend fun getExchangeableTokens(networkId: String): List<Currency> {
        return withContext(coroutineDispatcher.io) {
            tokensConverter.convertList(
                tangemTechApi.getCoins(
                    exchangeable = true,
                    active = true,
                    networkIds = networkId,
                ).coins,
            )
        }
    }

    override suspend fun findBestQuote(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): AggregatedSwapDataModel<QuoteModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = oneInchErrorsHandler.handleOneInchResponse(
                    getOneInchApi(networkId).quote(
                        fromTokenAddress = fromTokenAddress,
                        toTokenAddress = toTokenAddress,
                        amount = amount,
                    ),
                )
                AggregatedSwapDataModel(dataModel = quotesConverter.convert(response))
            } catch (ex: OneIncResponseException) {
                AggregatedSwapDataModel(null, mapErrors(ex.data.description))
            }
        }
    }

    override suspend fun addressForTrust(networkId: String): String {
        return withContext(coroutineDispatcher.io) {
            getOneInchApi(networkId).approveSpender().address
        }
    }

    override suspend fun dataToApprove(networkId: String, tokenAddress: String, amount: String?): ApproveModel {
        return withContext(coroutineDispatcher.io) {
            approveConverter.convert(getOneInchApi(networkId).approveTransaction(tokenAddress, amount))
        }
    }

    override suspend fun checkTokensSpendAllowance(
        networkId: String,
        tokenAddress: String,
        walletAddress: String,
    ): AggregatedSwapDataModel<String> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = oneInchErrorsHandler.handleOneInchResponse(
                    getOneInchApi(networkId).approveAllowance(tokenAddress, walletAddress),
                )
                AggregatedSwapDataModel(response.allowance)
            } catch (ex: OneIncResponseException) {
                AggregatedSwapDataModel(null, mapErrors(ex.data.description))
            }
        }
    }

    override suspend fun prepareSwapTransaction(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
        fromWalletAddress: String,
        slippage: Int,
    ): AggregatedSwapDataModel<SwapDataModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val swapResponse = oneInchErrorsHandler.handleOneInchResponse(
                    getOneInchApi(networkId).swap(
                        fromTokenAddress = fromTokenAddress,
                        toTokenAddress = toTokenAddress,
                        amount = amount,
                        fromAddress = fromWalletAddress,
                        slippage = slippage,
                        referrerAddress = configManager.config.swapReferrerAccount?.address,
                        fee = configManager.config.swapReferrerAccount?.fee,
                    ),
                )

                AggregatedSwapDataModel(swapConverter.convert(swapResponse))
            } catch (ex: OneIncResponseException) {
                AggregatedSwapDataModel(null, mapErrors(ex.data.description))
            }
        }
    }

    override fun getTangemFee(): Double {
        return configManager.config.swapReferrerAccount?.fee?.toDoubleOrNull() ?: 0.0
    }

    private fun getOneInchApi(networkId: String): OneInchApi {
        return oneInchApiFactory.getApi(networkId)
    }

    companion object {
        // TODO("get this ids from blockchain enum later")
        private const val OPTIMISM_ID = "optimistic-ethereum"
        private const val ARBITRUM_ID = "arbitrum-one"
        private const val ETHEREUM_ID = "ethereum"
    }
}
